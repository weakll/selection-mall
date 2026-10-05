package io.github.weakll.mall.ai.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BM25 检索器。
 *
 * <p>实现标准 BM25：
 * <pre>
 *   score(q,d) = Σ_t IDF(t) · f(t,d)·(k1+1) / ( f(t,d) + k1·(1 − b + b·|d|/avgdl) )
 *   IDF(t)     = ln( 1 + (N − n(t) + 0.5) / (n(t) + 0.5) )
 * </pre>
 *
 * <p><b>为什么自己算而不直接用 MySQL 的 {@code MATCH ... AGAINST}：</b>
 * MySQL InnoDB 全文检索的默认排序是 TF-IDF 的变体，不是 BM25，
 * 无法通过参数调整成 BM25（{@code k1}/{@code b} 无从配置）。
 * 因此分工是：MySQL 的 ngram 全文索引负责承载索引结构，
 * BM25 打分在应用侧精确计算，参数完全可控。
 *
 * <p><b>关键词加权：</b>知识库的人工标注关键词（口语同义词）对召回价值最高，
 * 建索引时把关键词文本重复计入文档词频。这比给整个字段加系数更简单，
 * 且效果等价于提高该部分的词频权重。
 *
 * <p>线程安全：实例构建后 {@code docs}/{@code df} 只读，可安全并发检索。
 */
public class Bm25Retriever {

    /** 词频饱和系数。1.2 是业界常用值：让高频词收益递减，避免单一词反复出现就霸榜。 */
    private static final double K1 = 1.2;

    /** 文档长度归一化系数。0.75 是业界常用值。 */
    private static final double B = 0.75;

    /**
     * 候选过滤用的覆盖率下限。
     *
     * <p>取值依据实测：越界问题的最高覆盖率约 25%，有效查询的最低覆盖率约 8%，
     * 两者重叠，任何阈值都无法完全分开。0.15 是在"挡住多数越界"与
     * "尽量不误杀有效查询"之间的折中。剩余漏网靠提示词兜底，而非继续抬高阈值。
     */
    private static final double MIN_COVERAGE = 0.15;

    /** 分数下限：滤掉仅个别词偶然命中的极弱匹配。同为实测取值。 */
    private static final double MIN_SCORE = 3.0;

    /** 一篇可检索文档。 */
    public record Document(String id, String text, List<String> keywords) {
    }

    /** 检索结果。 */
    public record Hit(String id, double score, double coverage) {
    }

    private record IndexedDoc(String id, Map<String, Integer> termFreq, int length) {
    }

    private final List<IndexedDoc> docs;

    private final Map<String, Integer> docFreq;

    private final double avgLength;

    private final Map<String, Document> byId;

    public Bm25Retriever(List<Document> documents) {
        List<IndexedDoc> indexed = new ArrayList<>(documents.size());
        Map<String, Integer> df = new HashMap<>();
        Map<String, Document> idMap = new HashMap<>();
        int totalLength = 0;

        for (Document doc : documents) {
            // 关键词重复计入：它们是最强的召回信号
            String keywords = doc.keywords() == null ? "" : String.join(" ", doc.keywords());
            String text = String.join(" ",
                    nullToEmpty(doc.text()),
                    keywords,
                    keywords);

            Map<String, Integer> tf = new HashMap<>();
            int length = 0;
            for (String token : FaqTokenizer.tokenize(text)) {
                tf.merge(token, 1, Integer::sum);
                length++;
            }
            indexed.add(new IndexedDoc(doc.id(), tf, length));
            totalLength += length;
            for (String token : tf.keySet()) {
                df.merge(token, 1, Integer::sum);
            }
            idMap.put(doc.id(), doc);
        }

        this.docs = List.copyOf(indexed);
        this.docFreq = Map.copyOf(df);
        this.avgLength = indexed.isEmpty() ? 0 : (double) totalLength / indexed.size();
        this.byId = Map.copyOf(idMap);
    }

    /** 文档数，供诊断。 */
    public int size() {
        return docs.size();
    }

    public Document document(String id) {
        return byId.get(id);
    }

    /**
     * 检索。
     *
     * @param query 用户问法
     * @param topK  返回条数上限
     * @return 命中的文档 id 与分数，按分数降序
     */
    public List<Hit> search(String query, int topK) {
        return search(query, topK, MIN_COVERAGE, MIN_SCORE);
    }

    /** 可指定门槛的检索，供评测扫描不同阈值。 */
    public List<Hit> search(String query, int topK, double minCoverage, double minScore) {
        List<String> queryTokens = FaqTokenizer.distinctTokens(query);
        if (queryTokens.isEmpty() || docs.isEmpty()) {
            return List.of();
        }

        List<Hit> hits = new ArrayList<>();
        for (IndexedDoc doc : docs) {
            double score = 0;
            int matched = 0;
            for (String token : queryTokens) {
                Integer freq = doc.termFreq().get(token);
                if (freq == null) {
                    continue;
                }
                matched++;
                int n = docFreq.getOrDefault(token, 0);
                double idf = Math.log(1 + (docs.size() - n + 0.5) / (n + 0.5));
                double denominator = freq + K1 * (1 - B + B * doc.length() / avgLength);
                score += idf * (freq * (K1 + 1)) / denominator;
            }
            if (score <= 0) {
                continue;
            }
            double coverage = (double) matched / queryTokens.size();
            if (coverage < minCoverage || score < minScore) {
                continue;
            }
            hits.add(new Hit(doc.id(), score, coverage));
        }

        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > topK ? List.copyOf(hits.subList(0, topK)) : List.copyOf(hits);
    }

    /** 查询用到的词，供诊断输出。 */
    public static Set<String> queryTokens(String query) {
        return new LinkedHashSet<>(FaqTokenizer.distinctTokens(query));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
