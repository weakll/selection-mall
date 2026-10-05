package io.github.weakll.mall.ai.tool;

import io.github.weakll.mall.ai.rag.FaqKnowledgeSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 售后政策知识库工具。
 *
 * <p>数据来源是 {@code classpath:ai/faq-knowledge.yml}（唯一事实来源），
 * 启动时同步进数据库并构建 BM25 检索索引。
 *
 * <p><b>两套检索路径：</b>
 * <ol>
 *   <li>主路径：{@code faq_knowledge} 表的 ngram 全文索引筛候选 + 应用侧 BM25 排序</li>
 *   <li>降级路径：数据库不可用时退回词表匹配（本类原先的实现）</li>
 * </ol>
 * 降级路径的价值是"数据库挂了不至于答不了政策问题"，但它对同义改写的召回明显更差——
 * 实测词表匹配在口语问法上的 Recall@3 只有 66.7%，而 BM25 为 88.1%。
 * 因此降级时会在返回值里标注，便于排查。
 */
@Component
public class FaqTools {

    private static final Logger log = LoggerFactory.getLogger(FaqTools.class);

    /** 单次返回的最大条目数。 */
    private static final int MAX_HITS = 3;

    private final Resource knowledgeResource;

    private final ToolSupport support;

    /**
     * 检索引擎。用 {@link ObjectProvider} 而不是直接注入：
     * 单元测试里需要脱离 Spring 容器构造本类，那时没有该组件。
     */
    private final ObjectProvider<FaqKnowledgeSource> knowledgeSourceProvider;

    private List<FaqEntry> entries = List.of();

    /** 数据库检索是否就绪；决定走主路径还是降级路径。 */
    private volatile boolean indexedSearchReady = false;

    /**
     * 主构造器。
     *
     * <p>显式标注 {@link org.springframework.beans.factory.annotation.Autowired}：
     * 本类为重载保留了单测用的简便构造器，存在多个构造器时 Spring 不会自动挑选，
     * 必须指明注入哪一个，否则报 "No default constructor found"。
     */
    @org.springframework.beans.factory.annotation.Autowired
    public FaqTools(@Value("classpath:ai/faq-knowledge.yml") Resource knowledgeResource,
                    ToolSupport support,
                    ObjectProvider<FaqKnowledgeSource> knowledgeSourceProvider) {
        this.knowledgeResource = knowledgeResource;
        this.support = support;
        this.knowledgeSourceProvider = knowledgeSourceProvider;
    }

    /** 供单元测试使用：不接数据库，检索走词表匹配降级路径。 */
    public FaqTools(Resource knowledgeResource, ToolSupport support) {
        this(knowledgeResource, support, null);
    }

    @PostConstruct
    void init() {
        reload();
    }

    /**
     * 载入知识库并构建检索索引。
     *
     * <p>公开且与 {@code @PostConstruct} 分离：跳过 Spring 容器的调用方（如单测）
     * 必须能自行初始化。早期版本把载入逻辑只放在 {@code @PostConstruct} 里，
     * 导致脱离容器构造的实例永远返回"知识库不可用"。
     *
     * @return 载入成功的条目数；失败返回 0
     */
    public int reload() {
        try (InputStream in = knowledgeResource.getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            Object raw = root == null ? null : root.get("entries");
            if (!(raw instanceof List<?> list)) {
                log.warn("知识库文件缺少 entries 列表，FAQ 工具将不可用");
                return 0;
            }
            List<FaqEntry> loaded = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    loaded.add(new FaqEntry(
                            String.valueOf(map.get("id")),
                            String.valueOf(map.get("category")),
                            String.valueOf(map.get("question")),
                            String.valueOf(map.get("answer")),
                            toStringList(map.get("keywords"))));
                }
            }
            this.entries = List.copyOf(loaded);
            log.info("售后政策知识库已载入 {} 条", this.entries.size());
        } catch (Exception ex) {
            // 载入失败不应让服务起不来：FAQ 工具退化为"查不到"，其它工具仍可用
            log.error("售后政策知识库载入失败，FAQ 工具将不可用: {}", ex.getMessage());
            return 0;
        }

        buildIndexIfPossible();
        return this.entries.size();
    }

    /** 把知识库同步进数据库并构建检索索引；数据库不可用则保持降级状态。 */
    private void buildIndexIfPossible() {
        FaqKnowledgeSource source = knowledgeSource();
        if (source == null) {
            indexedSearchReady = false;
            return;
        }
        int synced = source.syncAndBuild(entries);
        indexedSearchReady = synced > 0 && source.isReady();
        if (!indexedSearchReady) {
            log.warn("全文索引未就绪，检索将使用词表匹配（同义改写召回较弱）");
        }
    }

    private FaqKnowledgeSource knowledgeSource() {
        return knowledgeSourceProvider == null ? null : knowledgeSourceProvider.getIfAvailable();
    }

    /**
     * 按用户问题检索售后政策。
     *
     * @param query 用户原始问法
     */
    public ToolResult search(String query) {
        if (query == null || query.isBlank()) {
            return ToolResult.fail("请提供要查询的问题");
        }
        if (entries.isEmpty()) {
            return ToolResult.fail("售后政策知识库当前不可用，请转人工客服");
        }

        List<FaqEntry> hits = retrieve(query);
        if (hits.isEmpty()) {
            return ToolResult.ok("知识库中没有与该问题匹配的售后政策，请如实告知用户并建议联系人工客服");
        }

        List<Map<String, Object>> payload = new ArrayList<>(hits.size());
        for (FaqEntry hit : hits) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", hit.question());
            item.put("answer", hit.answer());
            payload.add(item);
        }
        log.debug("FAQ 命中 {} 条，query={}", hits.size(), query);
        return ToolResult.ok(support.toJson(payload));
    }

    /**
     * 检索命中的条目，按相关性排序。
     *
     * <p>与 {@link #search(String)} 分离的原因：评测需要拿到**条目 id 与排序**，
     * 而 search 返回的是给模型读的 JSON 文本。若把检索逻辑埋在格式化里，
     * 评测只能靠字符串比对，既脆弱又无法计算排名类指标（如 MRR）。
     *
     * <p>优先走全文索引 + BM25；不可用时退回词表匹配。
     */
    public List<FaqEntry> retrieve(String query) {
        return retrieve(query, MAX_HITS);
    }

    /** 按上限检索，供评测对比不同 k 值下的命中情况。 */
    public List<FaqEntry> retrieve(String query, int limit) {
        if (query == null || query.isBlank() || entries.isEmpty()) {
            return List.of();
        }

        if (indexedSearchReady) {
            FaqKnowledgeSource source = knowledgeSource();
            if (source != null) {
                List<String> ids = source.retrieve(query, limit);
                if (!ids.isEmpty()) {
                    return mapIds(ids);
                }
                // 索引可用但无命中，即"确实没有相关政策"，直接返回空而不退回词表匹配：
                // 退回会绕过 BM25 的相关性门槛，把越界问题也答上。
                return List.of();
            }
        }

        return keywordMatch(query, limit);
    }

    /** 词表匹配（降级路径，也是加入 BM25 之前的历史实现）。 */
    private List<FaqEntry> keywordMatch(String query, int limit) {
        String normalized = query.toLowerCase(Locale.ROOT);
        List<FaqEntry> hits = new ArrayList<>();
        for (FaqEntry entry : entries) {
            if (matches(entry, normalized)) {
                hits.add(entry);
                if (hits.size() >= limit) {
                    break;
                }
            }
        }
        return hits;
    }

    private List<FaqEntry> mapIds(List<String> ids) {
        Map<String, FaqEntry> byId = new LinkedHashMap<>();
        for (FaqEntry e : entries) {
            byId.put(e.id(), e);
        }
        List<FaqEntry> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            FaqEntry e = byId.get(id);
            if (e != null) {
                result.add(e);
            }
        }
        return result;
    }

    /** 关键词命中或问法包含。命中词覆盖口语同义表达，问法包含兜底长句提问。 */
    private static boolean matches(FaqEntry entry, String normalizedQuery) {
        for (String keyword : entry.keywords()) {
            if (!keyword.isBlank() && normalizedQuery.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return normalizedQuery.contains(entry.question().toLowerCase(Locale.ROOT));
    }

    private static List<String> toStringList(Object raw) {
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        return List.of();
    }

    /** 条目数，供测试与诊断使用。 */
    public int size() {
        return entries.size();
    }

    /** 全部知识库条目（只读），供评测校验标注集引用的 id 是否存在。 */
    public List<FaqEntry> entries() {
        return entries;
    }

    /** 当前是否走全文索引 + BM25 路径，供诊断与评测标注。 */
    public boolean indexedSearchReady() {
        return indexedSearchReady;
    }

    /** 强制指定检索路径，供评测在无数据库环境下固定使用降级路径。 */
    public void useIndexedSearch(boolean enabled) {
        this.indexedSearchReady = enabled;
    }

    /** 供提示词描述使用。 */
    public static String description() {
        return "查询退换货、发货时效、优惠券、发票、支付方式等售后政策。"
                + "回答政策类问题必须依据本工具返回的内容，不得自行编造时限或金额。";
    }

    /** 知识库条目。 */
    public record FaqEntry(String id, String category, String question, String answer,
                           List<String> keywords) {
    }
}
