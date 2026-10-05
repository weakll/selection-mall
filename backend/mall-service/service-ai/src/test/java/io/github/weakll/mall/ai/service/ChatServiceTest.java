package io.github.weakll.mall.ai.service;

import io.agentscope.core.model.Model;
import io.github.weakll.mall.ai.agent.ReActChatClient;
import io.github.weakll.mall.ai.config.MallAiConfiguration;
import io.github.weakll.mall.ai.config.MallAiProperties;
import io.github.weakll.mall.ai.model.ChatChunk;
import io.github.weakll.mall.ai.model.ChatClient;
import io.github.weakll.mall.ai.model.ChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ChatService} 单测。
 *
 * <p>不触网、不花钱：用桩客户端替换真实模型，验证消息组装与流桥接。
 */
class ChatServiceTest {

    /** 记录收到的请求，并回放固定的分片序列。 */
    private static final class StubChatClient implements ChatClient {

        private final AtomicReference<ChatRequest> lastRequest = new AtomicReference<>();

        private final AtomicReference<String> lastToken = new AtomicReference<>();

        @Override
        public Flux<ChatChunk> stream(ChatRequest request, String token) {
            lastToken.set(token);
            lastRequest.set(request);
            return Flux.just(ChatChunk.delta("你"), ChatChunk.delta("好"), ChatChunk.done("stop"));
        }

        @Override
        public String describe() {
            return "stub";
        }
    }

    private static MallAiProperties propertiesWithPrompt(String prompt) {
        MallAiProperties properties = new MallAiProperties();
        properties.getAgent().setSystemPrompt(prompt);
        return properties;
    }

    @Test
    @DisplayName("本轮输入被追加在历史之后，且带上系统提示词")
    void shouldAppendCurrentMessageAfterHistory() {
        StubChatClient client = new StubChatClient();
        ChatService service = newService(client, "你是客服", "tok-1");

        List<ChatRequest.Message> history = List.of(
                ChatRequest.Message.user("我要查订单"),
                ChatRequest.Message.assistant("请提供订单号"));

        List<ChatChunk> received = service.chat("订单号是 A123", history, 42L).collectList().block();

        assertNotNull(received);
        assertEquals(3, received.size(), "应收到 2 个增量 + 1 个结束分片");
        assertEquals(ChatChunk.Type.DONE, received.get(2).type());

        ChatRequest sent = client.lastRequest.get();
        assertNotNull(sent);
        assertEquals("你是客服", sent.systemPrompt());
        assertEquals(3, sent.messages().size(), "历史 2 条 + 本轮 1 条");
        assertEquals("订单号是 A123", sent.messages().get(2).content());
        assertEquals(ChatRequest.Role.USER, sent.messages().get(2).role());
    }

    @Test
    @DisplayName("历史超长时只保留最近 10 条，控制 token 成本")
    void shouldTrimHistoryToLimit() {
        StubChatClient client = new StubChatClient();
        ChatService service = newService(client, "p", null);

        List<ChatRequest.Message> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            history.add(ChatRequest.Message.assistant("历史第 " + i + " 条"));
        }

        service.chat("最新问题", history, null).collectList().block();

        ChatRequest sent = client.lastRequest.get();
        assertNotNull(sent);
        // 10 条历史 + 本轮 1 条
        assertEquals(11, sent.messages().size());
        // 保留的是尾部：最早的应轮到第 15 条
        assertEquals("历史第 15 条", sent.messages().get(0).content());
        assertEquals("最新问题", sent.messages().get(10).content());
    }

    @Test
    @DisplayName("流正常收尾时会发出 DONE，且增量内容按序拼接")
    void shouldEmitDeltasInOrderThenDone() {
        ChatService service = newService(new StubChatClient(), "p", null);

        List<ChatChunk> chunks = service.chat("hi", List.of(), null).collectList().block();

        assertNotNull(chunks);
        StringBuilder text = new StringBuilder();
        chunks.stream()
                .filter(c -> c.type() == ChatChunk.Type.DELTA)
                .forEach(c -> text.append(c.content()));
        assertEquals("你好", text.toString());
        assertEquals(ChatChunk.Type.DONE, chunks.get(chunks.size() - 1).type());
        assertFalse(chunks.stream().anyMatch(c -> c.type() == ChatChunk.Type.ERROR));
    }

    @Test
    @DisplayName("未配置密钥时给出可读错误而非抛异常")
    void shouldReportMissingApiKeyAsErrorChunk() {
        MallAiProperties properties = propertiesWithPrompt("p");
        // apiKey 默认为空；模型 bean 由装配层在缺密钥时返回 null
        Model model = new MallAiConfiguration().chatModel(properties);
        assertNull(model, "缺密钥时不应构建出模型");

        ChatClient client = reactClientWithMocks(properties, model);

        List<ChatChunk> chunks = client.stream(ChatRequest.of("p", List.of()), null).collectList().block();

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(ChatChunk.Type.ERROR, chunks.get(0).type());
        assertTrue(chunks.get(0).message().contains("DEEPSEEK_API_KEY"));
    }

    @Test
    @DisplayName("登录凭据从请求头捕获并传给 ChatClient")
    void shouldPropagateTokenToChatClient() {
        StubChatClient client = new StubChatClient();
        ChatService service = newService(client, "p", "tok-abc");

        service.chat("查询我的订单", List.of(), 7L).collectList().block();

        assertEquals("tok-abc", client.lastToken.get(),
                "token 必须在 servlet 线程捕获后传给 ChatClient，否则工具线程拿不到");
    }

    /**
     * 构造被测服务。
     *
     * <p>用 mock 的 HttpServletRequest 模拟 servlet 线程：真实场景下 token 就是在这里
     * 从请求头取出并被服务捕获的，之后要一路传到工具执行线程。
     */
    private static ChatService newService(ChatClient client, String systemPrompt, String token) {
        jakarta.servlet.http.HttpServletRequest request =
                org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        org.mockito.Mockito.when(request.getHeader("token")).thenReturn(token);
        return new ChatService(client, propertiesWithPrompt(systemPrompt), request);
    }

    /** 供缺密钥用例构造客户端；工具不会被真正调用。 */
    private static io.github.weakll.mall.ai.agent.ReActChatClient reactClientWithMocks(
            MallAiProperties properties, Model model) {
        io.github.weakll.mall.ai.tool.ToolSupport support = new io.github.weakll.mall.ai.tool.ToolSupport();
        return new io.github.weakll.mall.ai.agent.ReActChatClient(
                properties, model,
                new io.github.weakll.mall.ai.tool.ProductTools(
                        org.mockito.Mockito.mock(io.github.weakll.mall.feign.product.ProductFeignClient.class), support),
                new io.github.weakll.mall.ai.tool.OrderTools(
                        org.mockito.Mockito.mock(io.github.weakll.mall.feign.order.OrderFeignClient.class), support),
                new io.github.weakll.mall.ai.tool.FaqTools(
                        new org.springframework.core.io.ClassPathResource("ai/faq-knowledge.yml"), support));
    }
}
