package io.github.weakll.mall.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.weakll.mall.ai.config.MallAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于大模型的候选精排。
 *
 * <p><b>为什么需要它：</b>BM25 是词袋模型——只要查询词在文档里出现就加分，
 * 无法判断"这些词放在一起是否真的在回答这个问题"。实测后果是
 * "你们公司地址在哪里"这种知识库外的问题，因为零星撞上"公司""地址"等词也能拿到不低的分数，
 * 越界拒绝率从词表匹配的 100% 掉到 60%。用分数或覆盖率阈值都切不开
 * （两类查询的覆盖率区间重叠，见 {@link Bm25Retriever} 的说明）。
 *
 * <p>大模型逐对判断"这段答复能否回答这个问题"，正是补上词袋模型缺失的语义判断，
 * 因此它是解决越界问题的正解，而不是继续调阈值。
 *
 * <p><b>成本控制：</b>只对召回后的前若干条做精排（本实现取 8 条以内），
 * 不做全库打分。精排失败时退回原顺序，不阻断回答链路。
 */
public class LlmReranker {

    private static final Logger log = LoggerFactory.getLogger(LlmReranker.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 送入精排的最大候选数。候选越多判断质量越差，且 token 成本线性上升。 */
    private static final int MAX_CANDIDATES = 8;

    private static final String SYSTEM_PROMPT = """
            你是商城客服知识库的检索相关性评审。给你一个用户问题和若干条候选答复，
            请判断每条答复能否真正回答该问题。

            只在答复确实能回答问题时才保留它。若候选答复与问题无关（即使个别词相同），
            不要保留——用户问题超出知识库范围时，返回空列表是正确的做法。

            严格只输出 JSON，格式为 {"relevant":[候选编号,...]}，按相关度从高到低排列。
            不要输出任何解释文字。""";

    private final MallAiProperties.Model config;

    private final HttpClient httpClient;

    public LlmReranker(MallAiProperties properties) {
        this.config = properties.getModel();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** 是否可用（有密钥）。无密钥时 {@link #rerank} 直接返回原顺序。 */
    public boolean available() {
        return config.hasApiKey();
    }

    /**
     * 对候选精排。
     *
     * @param query      用户问法
     * @param candidateIds 候选条目 id，已按 BM25 分数降序
     * @param lookup     按 id 取答复文本，用于构造判断依据
     * @return 精排后的 id（只含判定为相关的），按相关度降序；
     *         调用失败或无密钥时返回原候选顺序
     */
    public List<String> rerank(String query, List<String> candidateIds, java.util.function.Function<String, String> lookup) {
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        if (!available()) {
            return candidateIds;
        }

        List<String> candidates = candidateIds.size() > MAX_CANDIDATES
                ? candidateIds.subList(0, MAX_CANDIDATES)
                : candidateIds;

        try {
            String userPrompt = buildUserPrompt(query, candidates, lookup);
            String content = callModel(userPrompt);
            List<String> ranked = parseRelevant(content, candidates);
            if (ranked.isEmpty()) {
                // 模型判定全部无关：视为知识库无对应政策，返回空 —— 这正是修越界问题的地方
                log.debug("精排判定无相关候选，query={}", query);
            }
            return ranked;
        } catch (Exception ex) {
            // 精排是增强而非必需：失败就退回原顺序，不能让客服答不出来
            log.warn("LLM 精排失败，退回原顺序：{}", ex.getMessage());
            return candidates;
        }
    }

    private static String buildUserPrompt(String query, List<String> ids,
                                          java.util.function.Function<String, String> lookup) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(query).append("\n\n候选答复：\n");
        for (int i = 0; i < ids.size(); i++) {
            String answer = lookup.apply(ids.get(i));
            sb.append(i + 1).append(". ").append(answer == null ? "" : answer).append("\n");
        }
        sb.append("\n请判断哪些候选能回答该问题，只输出 JSON。");
        return sb.toString();
    }

    /** 调用 OpenAI 兼容的对话接口（非流式，精排不需要流式）。 */
    private String callModel(String userPrompt) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", config.getModel());
        body.put("temperature", 0.0);
        // 判断任务不需要长输出，限制长度既省 token 也降低跑偏概率
        body.put("max_tokens", 256);

        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        String baseUrl = config.getBaseUrl();
        String endpoint = baseUrl.endsWith("/")
                ? baseUrl + "v1/chat/completions"
                : baseUrl + "/v1/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(config.getReadTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(
                        MAPPER.writeValueAsString(body), java.nio.charset.StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("精排接口返回 " + response.statusCode());
        }
        JsonNode root = MAPPER.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode()) {
            throw new IllegalStateException("精排响应缺少 content");
        }
        return content.asText();
    }

    /**
     * 解析模型返回的编号列表。
     *
     * <p>模型偶尔会用 markdown 代码块包裹 JSON，或输出额外文字，
     * 因此按"截取第一个 { 到最后一个 }"提取，而不是直接反序列化整段。
     */
    private static List<String> parseRelevant(String content, List<String> candidates) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("精排响应无法解析为 JSON: " + abbreviate(content));
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(content.substring(start, end + 1));
        } catch (Exception e) {
            throw new IllegalStateException("精排响应 JSON 解析失败: " + abbreviate(content));
        }
        JsonNode relevant = node.path("relevant");
        if (!relevant.isArray()) {
            throw new IllegalStateException("精排响应缺少 relevant 数组: " + abbreviate(content));
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : relevant) {
            int index = item.asInt(-1);
            // 模型可能给出越界编号，做一次范围校验而不是直接信任
            if (index >= 1 && index <= candidates.size()) {
                String id = candidates.get(index - 1);
                if (!result.contains(id)) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "null";
        }
        String flat = s.replace("\n", "\\n");
        return flat.length() > 160 ? flat.substring(0, 160) + "…" : flat;
    }
}
