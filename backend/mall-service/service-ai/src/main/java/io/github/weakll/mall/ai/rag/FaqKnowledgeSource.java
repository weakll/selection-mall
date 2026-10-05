package io.github.weakll.mall.ai.rag;

import io.github.weakll.mall.ai.tool.FaqTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 售后政策知识库的数据来源与检索引擎。
 *
 * <p><b>职责分工：</b>
 * <ul>
 *   <li>MySQL（{@code faq_knowledge} 表 + ngram 全文索引）—— 承载索引结构，
 *       用全文检索先筛出"含查询词"的候选，避免应用侧全表扫描</li>
 *   <li>应用侧 BM25（{@link Bm25Retriever}）—— 精确打分与排序。
 *       MySQL 默认排序是 TF-IDF 变体且 {@code k1}/{@code b} 不可配，做不到 BM25</li>
 * </ul>
 *
 * <p><b>数据一致性：</b>YAML 是知识库的唯一事实来源，启动时同步进数据库
 * （按 id upsert，并删除库中多余的条目）。这样知识库只有一处需要维护，
 * 数据库只是它的检索索引副本，不存在"两份数据手工对齐"的问题。
 *
 * <p><b>降级：</b>数据库不可用时 {@link #retrieve} 返回空，
 * 由 {@link FaqTools} 退回原有的词表匹配。知识库丢失会导致客服答不了政策问题，
 * 但不应该让整个服务不可用。
 */
@Component
public class FaqKnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(FaqKnowledgeSource.class);

    private final JdbcTemplate jdbcTemplate;

    private final LlmReranker reranker;

    /** 建好的检索器；数据库不可用时为 null，表示需要降级。 */
    private volatile Bm25Retriever retriever;

    /**
     * 条目快照，供词表那一路召回与精排取文本。
     * 与 {@link #retriever} 同时更新，保证两路看到的是同一版知识库。
     */
    private volatile List<FaqTools.FaqEntry> entrySnapshot = List.of();

    /** id → 答复文本，供精排构造判断依据，避免每条候选都线性扫全表。 */
    private volatile Map<String, String> answerById = Map.of();

    public FaqKnowledgeSource(JdbcTemplate jdbcTemplate, LlmReranker reranker) {
        this.jdbcTemplate = jdbcTemplate;
        this.reranker = reranker;
    }

    /**
     * 把 YAML 载入的知识库同步进数据库，并构建检索器。
     *
     * @param entries YAML 解析出的条目（唯一事实来源）
     * @return 成功同步的条数；数据库不可用时返回 0
     */
    public int syncAndBuild(List<FaqTools.FaqEntry> entries) {
        if (entries.isEmpty()) {
            log.warn("知识库为空，跳过同步");
            return 0;
        }
        try {
            // upsert：知识库条目变更时直接覆盖，不依赖手写迁移
            String upsert = """
                    INSERT INTO faq_knowledge (id, category, question, answer, keywords, source_text)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        category = VALUES(category),
                        question = VALUES(question),
                        answer = VALUES(answer),
                        keywords = VALUES(keywords),
                        source_text = VALUES(source_text)
                    """;

            List<Object[]> batch = new ArrayList<>(entries.size());
            for (FaqTools.FaqEntry e : entries) {
                batch.add(new Object[]{
                        e.id(),
                        e.category(),
                        e.question(),
                        e.answer(),
                        e.keywords() == null ? "" : String.join(",", e.keywords()),
                        buildSourceText(e)
                });
            }
            jdbcTemplate.batchUpdate(upsert, batch);

            // 删除 YAML 中已移除、但库里还留着的条目，避免索引出现幽灵文档
            String inClause = entries.stream().map(x -> "?").collect(Collectors.joining(","));
            List<Object> ids = new ArrayList<>(entries.stream().map(FaqTools.FaqEntry::id).toList());
            int removed = jdbcTemplate.update(
                    "DELETE FROM faq_knowledge WHERE id NOT IN (" + inClause + ")", ids.toArray());

            List<Bm25Retriever.Document> docs = loadDocuments();
            this.retriever = new Bm25Retriever(docs);
            this.entrySnapshot = List.copyOf(entries);

            Map<String, String> answers = new java.util.HashMap<>();
            for (FaqTools.FaqEntry e : entries) {
                answers.put(e.id(), e.question() + " " + e.answer());
            }
            this.answerById = Map.copyOf(answers);

            log.info("售后政策知识库已同步 {} 条到数据库（清理过期 {} 条），BM25 索引就绪",
                    entries.size(), removed);
            return entries.size();
        } catch (Exception ex) {
            this.retriever = null;
            this.entrySnapshot = List.of();
            this.answerById = Map.of();
            log.warn("知识库同步数据库失败，检索将退回词表匹配：{}", ex.getMessage());
            return 0;
        }
    }

    /**
     * 检索。
     *
     * <p><b>采用组 E 配置：RRF 融合后 LLM 精排。</b>选型依据是 120 条标注问法的实测对比：
     * <pre>
     *   配置            Recall@1  Recall@3  Recall@5   MRR    越界拒绝率
     *   A 词表匹配         46.1%     79.1%     79.1%  0.625    100.0%
     *   B BM25            88.7%     94.8%     95.7%  0.918     60.0%
     *   C RRF 融合         81.7%     98.3%     98.3%  0.899     60.0%
     *   D BM25+精排        93.9%     94.8%     94.8%  0.943    100.0%
     *   E RRF+精排         95.7%     96.5%     96.5%  0.961    100.0%
     * </pre>
     *
     * <p>单看 C 与 D 会发现一个矛盾：C 靠两路互补拿到最高召回，但 RRF 只融合排名、
     * 把首位顺序拉平了；D 的排序与越界判断最好，却只用了单路候选。
     * 串起来（E）同时拿到了两者的长处——且 E 的 Recall@5 只比 C 低 1.8pt，
     * 换来的是越界拒绝率从 60% 提到 100% 与 Recall@1 高出 14pt。
     * 对客服场景而言"第一个答案对不对"和"不知道就别编"权重更高，因此选 E。
     *
     * @return 命中的条目 id（按相关性降序）；未就绪或数据库不可用时返回空列表
     */
    public List<String> retrieve(String query, int topK) {
        Bm25Retriever current = this.retriever;
        if (current == null) {
            return List.of();
        }
        try {
            // 第一路：MySQL ngram 全文索引筛候选 + BM25 打分
            List<String> bm25Ranked = rankedByBm25(current, query, topK);
            // 第二路：词表匹配（人工标注的口语同义词，与 BM25 互补）
            List<String> keywordRanked = rankedByKeyword(query, topK);
            // 融合：两路分数不可比，只融合排名
            List<String> fused = RrfFusion.fuse(List.of(keywordRanked, bm25Ranked), topK);
            // 精排：判断"词都在但语义无关"的情况，同时修正首位顺序
            return reranker == null
                    ? fused
                    : reranker.rerank(query, fused, this::answerOf);
        } catch (Exception ex) {
            log.warn("检索失败：{}", ex.getMessage());
            return List.of();
        }
    }

    /** BM25 一路：先经 MySQL 全文索引过滤候选，再打分排序。 */
    private List<String> rankedByBm25(Bm25Retriever retriever, String query, int topK) {
        try {
            Set<String> candidates = fullTextCandidates(query);
            if (candidates.isEmpty()) {
                return List.of();
            }
            return retriever.search(query, topK).stream()
                    .map(Bm25Retriever.Hit::id)
                    .filter(candidates::contains)
                    .toList();
        } catch (Exception ex) {
            // 全文索引不可用不应让整条链路失败：退回纯 BM25
            log.warn("全文检索失败，本路退回纯 BM25：{}", ex.getMessage());
            return retriever.search(query, topK).stream().map(Bm25Retriever.Hit::id).toList();
        }
    }

    /** 词表一路：命中即入选，顺序按库中次序（该路只提供"是否相关"，不提供排序信息）。 */
    private List<String> rankedByKeyword(String query, int topK) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (FaqTools.FaqEntry e : entrySnapshot) {
            boolean matched = false;
            for (String keyword : e.keywords()) {
                if (!keyword.isBlank() && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                hits.add(e.id());
                if (hits.size() >= topK) {
                    break;
                }
            }
        }
        return hits;
    }

    private String answerOf(String id) {
        return answerById.get(id);
    }

    /** 用 MySQL ngram 全文索引筛出可能相关的文档 id。 */
    private Set<String> fullTextCandidates(String query) {
        String matchable = buildMatchExpression(query);
        if (matchable.isBlank()) {
            return Set.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM faq_knowledge WHERE MATCH(source_text) AGAINST (? IN NATURAL LANGUAGE MODE)",
                matchable);
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            ids.add(String.valueOf(row.get("id")));
        }
        return ids;
    }

    /**
     * 构造全文检索的匹配串。
     *
     * <p>MySQL 的 ngram 全文检索按词匹配，把整句中文交给它虽然也能工作，
     * 但这里显式送入"应用侧切好的 2-gram 词，用空格连接"，
     * 让数据库侧的切分与应用侧保持一致——否则两套切分规则会互相打架，
     * 出现"应用侧认为该命中、数据库侧筛掉了"的漏召回。
     */
    private static String buildMatchExpression(String query) {
        List<String> tokens = FaqTokenizer.distinctTokens(query);
        return tokens.isEmpty() ? "" : String.join(" ", tokens);
    }

    private List<Bm25Retriever.Document> loadDocuments() {
        return jdbcTemplate.query(
                "SELECT id, question, answer, keywords, source_text FROM faq_knowledge",
                (rs, rowNum) -> {
                    String keywords = rs.getString("keywords");
                    List<String> keywordList = keywords == null || keywords.isBlank()
                            ? List.of()
                            : List.of(keywords.split(","));
                    return new Bm25Retriever.Document(
                            rs.getString("id"),
                            rs.getString("source_text"),
                            keywordList);
                });
    }

    /** 检索用的合并文本。关键词是人工标注的口语同义词，召回价值最高。 */
    private static String buildSourceText(FaqTools.FaqEntry e) {
        String keywords = e.keywords() == null ? "" : String.join(" ", e.keywords());
        return e.question() + " " + e.answer() + " " + keywords;
    }

    /** 检索器是否就绪，供健康检查与诊断使用。 */
    public boolean isReady() {
        Bm25Retriever current = this.retriever;
        return current != null && current.size() > 0;
    }

    /** 已建索引的文档数。 */
    public int indexedSize() {
        Bm25Retriever current = this.retriever;
        return current == null ? 0 : current.size();
    }
}
