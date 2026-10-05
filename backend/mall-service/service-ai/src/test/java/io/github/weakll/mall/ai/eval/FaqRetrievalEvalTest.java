package io.github.weakll.mall.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.weakll.mall.ai.config.MallAiProperties;
import io.github.weakll.mall.ai.rag.Bm25Retriever;
import io.github.weakll.mall.ai.rag.LlmReranker;
import io.github.weakll.mall.ai.rag.RrfFusion;
import io.github.weakll.mall.ai.tool.FaqTools;
import io.github.weakll.mall.ai.tool.ToolSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 售后政策检索评测：四组配置对比。
 *
 * <p><b>四组配置的递进关系：</b>
 * <table>
 *   <tr><th>组</th><th>配置</th><th>引入它的理由</th></tr>
 *   <tr><td>A</td><td>词表匹配</td><td>改造前的基线</td></tr>
 *   <tr><td>B</td><td>+ MySQL 全文索引与 BM25</td><td>词表是人工枚举的，覆盖不了同义改写</td></tr>
 *   <tr><td>C</td><td>+ RRF 融合</td><td>两路召回量纲不可比，用排名融合免去归一化超参</td></tr>
 *   <tr><td>D</td><td>+ LLM 精排</td><td>BM25 是词袋模型，判断不了"词都在但语义无关"，越界问题需语义判断</td></tr>
 * </table>
 *
 * <p><b>按问法类型分组统计的原因：</b>总体平均会把问题掩盖掉。
 * 词表匹配在书面问法上天然占优、在口语问法上断崖式下跌，
 * 只报总体数字看不出该改哪里。
 *
 * <p>标注集在写检索代码之前完成，且未回看检索结果调整，避免按答案调参导致指标虚高。
 * A/B/C 三组不依赖外部服务，始终运行；D 组需 {@code DEEPSEEK_API_KEY}，未设置时自动跳过。
 */
class FaqRetrievalEvalTest {

    /** 评测取前 5 条：线上 FaqTools 可返回多条供模型综合，多意图查询需要这个余量。 */
    private static final int TOP_K = 5;

    private static final int EXPECTED_KB_SIZE = 63;

    private static final int EXPECTED_CASE_COUNT = 120;

    private final ObjectMapper mapper = new ObjectMapper();

    // ---------- 评测集与知识库 ----------

    private record EvalCase(String query, String type, List<String> expected) {
        boolean outOfScope() {
            return expected.isEmpty();
        }
    }

    /** 检索函数：给定问法返回按相关性排序的条目 id。 */
    @FunctionalInterface
    private interface Retriever {
        List<String> retrieve(String query);
    }

    private List<EvalCase> loadEvalSet() throws Exception {
        List<EvalCase> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("ai/faq-eval-set.jsonl").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Map<String, Object> node = mapper.readValue(line, new TypeReference<>() {
                });
                @SuppressWarnings("unchecked")
                List<String> expected = (List<String>) node.get("expected");
                cases.add(new EvalCase((String) node.get("q"), (String) node.get("type"), expected));
            }
        }
        return cases;
    }

    private FaqTools faqTools() {
        FaqTools tools = new FaqTools(new ClassPathResource("ai/faq-knowledge.yml"), new ToolSupport());
        tools.reload();
        return tools;
    }

    /** 按生产同一条路径构建 BM25 索引：源文本 = 问法 + 答复 + 关键词（关键词重复加权）。 */
    private Bm25Retriever buildRetriever(List<FaqTools.FaqEntry> entries) {
        List<Bm25Retriever.Document> docs = new ArrayList<>(entries.size());
        for (FaqTools.FaqEntry e : entries) {
            docs.add(new Bm25Retriever.Document(
                    e.id(),
                    e.question() + " " + e.answer() + " " + String.join(" ", e.keywords()),
                    e.keywords()));
        }
        return new Bm25Retriever(docs);
    }

    // ---------- 指标计算 ----------

    /** byType 的值为 {样本数, R@1 命中, R@3 命中, R@5 命中}。越界样本以"正确拒绝"计入命中。 */
    private record Metrics(String config, int total, double recallAt1, double recallAt3,
                           double recallAt5, double mrr, double refusalRate,
                           Map<String, double[]> byType) {
    }

    private Metrics evaluate(String configName, Retriever retriever, List<EvalCase> cases) {
        int hit1 = 0;
        int hit3 = 0;
        int hit5 = 0;
        double reciprocalSum = 0;
        int scopedCount = 0;
        int outOfScopeCount = 0;
        int outOfScopeRefused = 0;
        Map<String, int[]> typeStat = new LinkedHashMap<>();

        for (EvalCase c : cases) {
            List<String> gotIds = retriever.retrieve(c.query());
            typeStat.computeIfAbsent(c.type(), k -> new int[4]);
            typeStat.get(c.type())[0]++;

            if (c.outOfScope()) {
                outOfScopeCount++;
                if (gotIds.isEmpty()) {
                    outOfScopeRefused++;
                    typeStat.get(c.type())[1]++;
                    typeStat.get(c.type())[2]++;
                    typeStat.get(c.type())[3]++;
                }
                continue;
            }

            scopedCount++;
            Set<String> expected = new LinkedHashSet<>(c.expected());
            if (!gotIds.isEmpty() && expected.contains(gotIds.get(0))) {
                hit1++;
                typeStat.get(c.type())[1]++;
            }
            for (int i = 0; i < gotIds.size(); i++) {
                if (expected.contains(gotIds.get(i))) {
                    if (i < 3) {
                        hit3++;
                        typeStat.get(c.type())[2]++;
                    }
                    if (i < 5) {
                        hit5++;
                        typeStat.get(c.type())[3]++;
                    }
                    reciprocalSum += 1.0 / (i + 1);
                    break;
                }
            }
        }

        Map<String, double[]> byType = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> e : typeStat.entrySet()) {
            int[] s = e.getValue();
            byType.put(e.getKey(), new double[]{s[0], s[1], s[2], s[3]});
        }
        return new Metrics(configName, cases.size(),
                scopedCount == 0 ? 0 : (double) hit1 / scopedCount,
                scopedCount == 0 ? 0 : (double) hit3 / scopedCount,
                scopedCount == 0 ? 0 : (double) hit5 / scopedCount,
                scopedCount == 0 ? 0 : reciprocalSum / scopedCount,
                outOfScopeCount == 0 ? 0 : (double) outOfScopeRefused / outOfScopeCount,
                byType);
    }

    private static String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }

    private static void printMetrics(Metrics m) {
        System.out.printf("%n--- %s ---%n", m.config());
        System.out.printf("Recall@1 %s  Recall@3 %s  Recall@5 %s  MRR %.3f  越界拒绝率 %s%n",
                pct(m.recallAt1()), pct(m.recallAt3()), pct(m.recallAt5()), m.mrr(), pct(m.refusalRate()));
    }

    /** 打印四组汇总对比表。 */
    private static void printComparison(List<Metrics> all) {
        System.out.println("\n==================================================================");
        System.out.println("检索配置对比（知识库 63 条，标注集 120 条，评测 topK=" + TOP_K + "）");
        System.out.println("==================================================================");
        System.out.printf("%-24s %9s %9s %9s %8s %10s%n",
                "配置", "Recall@1", "Recall@3", "Recall@5", "MRR", "越界拒绝率");
        System.out.println("-".repeat(74));
        for (Metrics m : all) {
            System.out.printf("%-24s %9s %9s %9s %8.3f %10s%n",
                    m.config(), pct(m.recallAt1()), pct(m.recallAt3()), pct(m.recallAt5()),
                    m.mrr(), pct(m.refusalRate()));
        }

        System.out.println("\n按问法类型 Recall@5：");
        Set<String> types = new LinkedHashSet<>();
        for (Metrics m : all) {
            types.addAll(m.byType().keySet());
        }
        StringBuilder header = new StringBuilder(String.format("%-10s", "类型"));
        for (Metrics m : all) {
            header.append(String.format("%12s", m.config().substring(0, 1)));
        }
        header.append(String.format("%10s", "样本"));
        System.out.println(header);
        System.out.println("-".repeat(74));
        for (String type : types) {
            StringBuilder row = new StringBuilder(String.format("%-10s", type));
            int samples = 0;
            for (Metrics m : all) {
                double[] s = m.byType().get(type);
                if (s == null) {
                    row.append(String.format("%12s", "-"));
                } else {
                    samples = (int) s[0];
                    row.append(String.format("%12s", pct(s[3] / s[0])));
                }
            }
            row.append(String.format("%10d", samples));
            System.out.println(row);
        }
        System.out.println("=".repeat(74));
    }

    private static double typeRatio(Metrics m, String type) {
        double[] s = m.byType().get(type);
        return s == null || s[0] == 0 ? 0 : s[3] / s[0];
    }

    // ---------- 测试 ----------

    @Test
    @DisplayName("评测集自身完整性：id 必须都能在知识库中找到")
    void evalSetIdsMustExistInKnowledgeBase() throws Exception {
        FaqTools tools = faqTools();
        Set<String> kbIds = new LinkedHashSet<>();
        for (FaqTools.FaqEntry e : tools.entries()) {
            kbIds.add(e.id());
        }
        assertEquals(EXPECTED_KB_SIZE, kbIds.size(), "知识库条目数变化需同步标注集");

        List<String> bad = new ArrayList<>();
        for (EvalCase c : loadEvalSet()) {
            for (String id : c.expected()) {
                if (!kbIds.contains(id)) {
                    bad.add(c.query() + " -> " + id);
                }
            }
        }
        assertTrue(bad.isEmpty(), "标注集引用了不存在的政策 id：" + bad);
    }

    @Test
    @DisplayName("组 A/B/C 对比：词表匹配 → BM25 → RRF 融合")
    void retrievalConfigsComparison() throws Exception {
        FaqTools tools = faqTools();
        List<EvalCase> cases = loadEvalSet();
        assertEquals(EXPECTED_CASE_COUNT, cases.size(), "标注集条数变化需重新核对");

        List<FaqTools.FaqEntry> entries = tools.entries();
        Bm25Retriever retriever = buildRetriever(entries);

        // 组 A：词表匹配（词表是人工枚举的，同义改写覆盖不到）
        Retriever groupA = q -> ids(tools.retrieve(q, TOP_K));

        // 组 B：全文索引 + BM25
        Retriever groupB = q -> retriever.search(q, TOP_K).stream().map(Bm25Retriever.Hit::id).toList();

        // 组 C：A 与 B 用 RRF 融合。两路分数不可比，因此只融合排名。
        Retriever groupC = q -> RrfFusion.fuse(List.of(groupA.retrieve(q), groupB.retrieve(q)), TOP_K);

        Metrics a = evaluate("A 词表匹配", groupA, cases);
        Metrics b = evaluate("B BM25", groupB, cases);
        Metrics c = evaluate("C RRF 融合", groupC, cases);

        printMetrics(a);
        printMetrics(b);
        printMetrics(c);
        printComparison(List.of(a, b, c));

        double oralA = typeRatio(a, "口语");
        double oralB = typeRatio(b, "口语");
        assertTrue(oralB > oralA + 0.10,
                String.format("BM25 在口语问法上应明显优于词表匹配，实际 A=%.1f%% B=%.1f%%",
                        oralA * 100, oralB * 100));
        assertTrue(b.recallAt5() >= a.recallAt5() - 0.05, "BM25 整体召回不应显著低于基线");
    }

    @Test
    @DisplayName("组 D：LLM 精排（需 DEEPSEEK_API_KEY）")
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void llmRerankComparison() throws Exception {
        FaqTools tools = faqTools();
        List<EvalCase> cases = loadEvalSet();
        List<FaqTools.FaqEntry> entries = tools.entries();
        Bm25Retriever retriever = buildRetriever(entries);

        MallAiProperties properties = new MallAiProperties();
        properties.getModel().setApiKey(System.getenv("DEEPSEEK_API_KEY"));
        properties.getModel().setBaseUrl(envOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
        properties.getModel().setModel(envOrDefault("DEEPSEEK_MODEL", "deepseek-chat"));
        LlmReranker reranker = new LlmReranker(properties);

        Map<String, String> answerById = new LinkedHashMap<>();
        for (FaqTools.FaqEntry e : entries) {
            answerById.put(e.id(), e.question() + " " + e.answer());
        }

        Retriever groupB = q -> retriever.search(q, TOP_K).stream().map(Bm25Retriever.Hit::id).toList();
        Retriever groupD = q -> reranker.rerank(q, groupB.retrieve(q), answerById::get);

        // 组 E：RRF 融合后精排。
        // 单独看 C 与 D 会发现一个矛盾：C 的召回最高（两路互补）但排序被融合拉平，
        // D 的排序与越界判断最好但只用了单路候选。串起来应能同时拿到两者。
        Retriever groupA = q -> ids(tools.retrieve(q, TOP_K));
        Retriever groupE = q -> reranker.rerank(
                q,
                RrfFusion.fuse(List.of(groupA.retrieve(q), groupB.retrieve(q)), TOP_K),
                answerById::get);

        Metrics b = evaluate("B BM25", groupB, cases);
        Metrics cReal = evaluate("C RRF 融合", q -> RrfFusion.fuse(
                List.of(groupA.retrieve(q), groupB.retrieve(q)), TOP_K), cases);
        Metrics d = evaluate("D BM25 + LLM 精排", groupD, cases);
        Metrics e = evaluate("E RRF + LLM 精排", groupE, cases);

        printMetrics(b);
        printMetrics(cReal);
        printMetrics(d);
        printMetrics(e);
        printComparison(List.of(b, cReal, d, e));

        System.out.printf("%n越界拒绝率：B %s → D %s → E %s%n",
                pct(b.refusalRate()), pct(d.refusalRate()), pct(e.refusalRate()));
        System.out.printf("Recall@5：  B %s → C %s → E %s%n",
                pct(b.recallAt5()), pct(cReal.recallAt5()), pct(e.recallAt5()));
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<String> ids(List<FaqTools.FaqEntry> entries) {
        return entries.stream().map(FaqTools.FaqEntry::id).toList();
    }
}
