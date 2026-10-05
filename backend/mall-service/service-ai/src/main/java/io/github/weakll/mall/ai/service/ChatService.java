package io.github.weakll.mall.ai.service;

import io.github.weakll.mall.ai.config.MallAiProperties;
import io.github.weakll.mall.ai.model.ChatChunk;
import io.github.weakll.mall.ai.model.ChatClient;
import io.github.weakll.mall.ai.model.ChatRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 客服对话服务。
 *
 * <p>当前阶段：单轮问答 + 由前端携带的最近若干条上下文。
 * 服务端会话持久化属后续阶段（见 docs/ai-service.md 的落地边界）。
 *
 * <p><b>请求作用域是必需的：</b>登录凭据必须在 servlet 线程上取出，
 * 因为 Agent 的工具执行发生在 Reactor 调度器线程，那里没有请求上下文。
 * 若把本类做成单例、把取 token 的动作推迟到工具执行时，取到的永远是 null
 * （订单查询会报 AuthContextUtil.getUserInfo() is null）。
 */
@Service
@org.springframework.web.context.annotation.RequestScope
public class ChatService {

    /** 最多采纳的上下文条数，防止前端传入过长历史推高 token 成本。 */
    private static final int MAX_HISTORY_MESSAGES = 10;

    private final ChatClient chatClient;

    private final MallAiProperties properties;

    /** 在当前请求线程上捕获登录凭据，供工具执行线程使用。 */
    private final String token;

    public ChatService(ChatClient chatClient, MallAiProperties properties, HttpServletRequest request) {
        this.chatClient = chatClient;
        this.properties = properties;
        this.token = request.getHeader("token");
    }

    /**
     * 发起一轮流式对话。
     *
     * @param message 用户本轮输入
     * @param history 近期上下文，可为空
     * @param userId  当前登录用户 ID；本阶段仅用于标识与后续工具鉴权，未登录为 null
     */
    public Flux<ChatChunk> chat(String message, List<ChatRequest.Message> history, Long userId) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        if (history != null) {
            // 只保留最近若干条：越靠后的越新，取尾部
            int from = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
            messages.addAll(history.subList(from, history.size()));
        }
        messages.add(ChatRequest.Message.user(message));

        ChatRequest request = ChatRequest.of(properties.getAgent().getSystemPrompt(), messages);
        // token 在此（servlet 线程）随调用传入，最终绑定到工具执行线程供 Feign 使用
        return chatClient.stream(request, token);
    }

    /** 供诊断端点使用，不暴露密钥。 */
    public String clientDescription() {
        return chatClient.describe();
    }

    public MallAiProperties properties() {
        return properties;
    }
}
