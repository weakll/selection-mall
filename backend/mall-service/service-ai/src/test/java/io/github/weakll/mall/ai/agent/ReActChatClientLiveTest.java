package io.github.weakll.mall.ai.agent;

import io.agentscope.core.model.Model;
import io.github.weakll.mall.ai.config.MallAiConfiguration;
import io.github.weakll.mall.ai.config.MallAiProperties;
import io.github.weakll.mall.ai.model.ChatChunk;
import io.github.weakll.mall.ai.model.ChatRequest;
import io.github.weakll.mall.ai.tool.FaqTools;
import io.github.weakll.mall.ai.tool.OrderTools;
import io.github.weakll.mall.ai.tool.ProductTools;
import io.github.weakll.mall.ai.tool.ToolSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 真实调用 DeepSeek 的联调测试（含 Agent 与工具注册）。
 *
 * <p>默认跳过：只有设置了环境变量 {@code DEEPSEEK_API_KEY} 时才执行，
 * 避免在无密钥环境（如 CI）失败，也避免无意中产生调用费用。
 *
 * <p>本测试承担两个此前无法验证的风险：
 * <ol>
 *   <li>AgentScope 编译时面向 reactor-core 3.8.2，实际运行在 Boot 3.0.5 管理的 3.5.4 上——
 *       只有真正订阅 Flux 才能确认低版本 reactor 具备其用到的全部 API。</li>
 *   <li>工具是否真的注册成功——模型能否看到工具、是否愿意调用，只能靠真实调用暴露。</li>
 * </ol>
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class ReActChatClientLiveTest {

    private static MallAiProperties liveProperties() {
        MallAiProperties properties = new MallAiProperties();
        MallAiProperties.Model model = properties.getModel();
        model.setApiKey(System.getenv("DEEPSEEK_API_KEY"));
        model.setBaseUrl(envOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
        model.setModel(envOrDefault("DEEPSEEK_MODEL", "deepseek-chat"));
        properties.getAgent().setSystemPrompt(
                "你是精选商城的在线客服小选。用简体中文回答，语气亲切简洁。"
                        + "涉及商品信息时调用 queryProduct，涉及用户订单时调用 queryMyOrders，"
                        + "涉及退换货等政策时调用 searchFaq。");
        return properties;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 构造被测客户端。
     *
     * <p>商品与订单工具用 mock：本测试关注"模型能否看到并调用工具"，
     * 不关注下游服务是否可用，避免测试依赖完整基础设施。
     * FAQ 工具用真实知识库，因为它只读本地资源文件。
     */
    private static ReActChatClient liveClient(MallAiProperties properties) {
        Model model = new MallAiConfiguration().chatModel(properties);
        ToolSupport support = new ToolSupport();
        // 脱离 Spring 容器构造时须显式载入知识库，否则 FAQ 工具会返回"不可用"
        FaqTools faqTools = new FaqTools(new ClassPathResource("ai/faq-knowledge.yml"), support);
        faqTools.reload();
        return new ReActChatClient(properties, model,
                new ProductTools(mock(io.github.weakll.mall.feign.product.ProductFeignClient.class), support),
                new OrderTools(mock(io.github.weakll.mall.feign.order.OrderFeignClient.class), support),
                faqTools);
    }

    @Test
    @DisplayName("真实流式调用：长回答产生多个增量，支撑逐字输出")
    void shouldStreamRealDeltas() {
        ReActChatClient client = liveClient(liveProperties());
        System.out.println("[live] " + client.describe());

        StringBuilder text = new StringBuilder();
        AtomicBoolean sawDone = new AtomicBoolean(false);
        AtomicBoolean sawError = new AtomicBoolean(false);

        List<ChatChunk> chunks = client
                .stream(ChatRequest.of(null, List.of(ChatRequest.Message.user(
                        "请分点介绍你自己，至少写 5 句话，每句独立成行。"))), null)
                .doOnNext(chunk -> {
                    switch (chunk.type()) {
                        case DELTA -> text.append(chunk.content());
                        case DONE -> sawDone.set(true);
                        case ERROR -> {
                            sawError.set(true);
                            System.out.println("[live] error: " + chunk.message());
                        }
                    }
                })
                .collectList()
                .block(Duration.ofSeconds(120));

        assertNotNull(chunks, "流应正常结束");
        long deltaCount = chunks.stream().filter(c -> c.type() == ChatChunk.Type.DELTA).count();
        System.out.println("[live] 增量数=" + deltaCount + " 正文字数=" + text.length());
        System.out.println("[live] 前 80 字=" + preview(text.toString()));

        assertFalse(sawError.get(), "不应出现错误分片");
        assertTrue(sawDone.get(), "应以 DONE 收尾");
        assertTrue(deltaCount > 1, "长回答应产生多个增量，实际 " + deltaCount);
    }

    @Test
    @DisplayName("工具确实注册给模型：问到售后政策时会调用 searchFaq")
    void shouldInvokeFaqToolWhenAskedPolicyQuestion() {
        ReActChatClient client = liveClient(liveProperties());

        StringBuilder text = new StringBuilder();
        client.stream(ChatRequest.of(null, List.of(ChatRequest.Message.user(
                        "你们支持七天无理由退货吗？大概几天能到货？"))), null)
                .filter(c -> c.type() == ChatChunk.Type.DELTA)
                .doOnNext(c -> text.append(c.content()))
                .blockLast(Duration.ofSeconds(120));

        String answer = text.toString();
        System.out.println("[live] 回答=" + preview(answer));

        // 工具被调用时 ReActChatClient 会推入进度提示，这是"工具真的注册了"的直接证据
        assertTrue(answer.contains("正在查询"),
                "应出现工具调用进度提示，否则说明工具未注册给模型。实际回答：" + preview(answer));
        // 知识库里的真实数字应出现在回答中，而不是模型编造的
        assertTrue(answer.contains("7 天") || answer.contains("7天"),
                "回答应包含知识库中的 7 天退换货政策");
    }

    @Test
    @DisplayName("describe 不泄露密钥")
    void describeShouldNotLeakApiKey() {
        MallAiProperties properties = liveProperties();
        String key = properties.getModel().getApiKey();
        assertNotNull(key);
        assertFalse(key.isEmpty());
        assertFalse(liveClient(properties).describe().contains(key), "describe() 不得含密钥明文");
    }

    private static String preview(String value) {
        String flat = value.replace("\n", "\\n");
        return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
    }
}
