package io.github.weakll.mall.ai.tool;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>数据来自 {@code classpath:ai/faq-knowledge.yml}，启动时载入内存。
 * 当前条目量级下用"关键词命中"足够，且比向量检索更可解释——
 * 命中哪条、为什么命中都能在日志里说清，也便于回答"为什么不用向量库"。
 */
@Component
public class FaqTools {

    private static final Logger log = LoggerFactory.getLogger(FaqTools.class);

    /** 单次返回的最大条目数。 */
    private static final int MAX_HITS = 3;

    private final Resource knowledgeResource;

    private final ToolSupport support;

    private List<FaqEntry> entries = List.of();

    public FaqTools(@Value("classpath:ai/faq-knowledge.yml") Resource knowledgeResource,
                    ToolSupport support) {
        this.knowledgeResource = knowledgeResource;
        this.support = support;
    }

    @PostConstruct
    void init() {
        reload();
    }

    /**
     * 载入知识库。
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
                            String.valueOf(map.get("question")),
                            String.valueOf(map.get("answer")),
                            toStringList(map.get("keywords"))));
                }
            }
            this.entries = List.copyOf(loaded);
            log.info("售后政策知识库已载入 {} 条", this.entries.size());
            return this.entries.size();
        } catch (Exception ex) {
            // 载入失败不应让服务起不来：FAQ 工具退化为"查不到"，其它工具仍可用
            log.error("售后政策知识库载入失败，FAQ 工具将不可用: {}", ex.getMessage());
            return 0;
        }
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

        String normalized = query.toLowerCase(Locale.ROOT);
        List<FaqEntry> hits = new ArrayList<>();
        for (FaqEntry entry : entries) {
            if (matches(entry, normalized)) {
                hits.add(entry);
                if (hits.size() >= MAX_HITS) {
                    break;
                }
            }
        }

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

    /** 供提示词描述使用。 */
    public static String description() {
        return "查询退换货、发货时效、优惠券、发票、支付方式等售后政策。"
                + "回答政策类问题必须依据本工具返回的内容，不得自行编造时限或金额。";
    }

    /** 知识库条目。 */
    public record FaqEntry(String id, String question, String answer, List<String> keywords) {
    }
}
