package io.github.weakll.mall.ai.controller;

import io.github.weakll.mall.ai.model.ChatChunk;
import io.github.weakll.mall.ai.model.ChatRequest;
import io.github.weakll.mall.ai.service.ChatService;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.utils.AuthContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 客服接口。
 *
 * <p>路径设计遵循网关既有约定：{@code /api/ai/**} 公开，
 * {@code /api/ai/auth/**} 由网关 {@code AuthGlobalFilter} 强制登录
 * （它匹配 {@code /api/**}{@code /auth/**}）。因此对话接口放在 auth 段下，
 * 保证"只能查到自己的订单"这条约束在最外层就成立。
 */
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 流式对话。
     *
     * <p>用服务端缓冲的 {@link SseEmitter} 而非返回 {@code Flux}：
     * Spring MVC 对后者的支持依赖额外的响应式桥接，服务端缓冲的写法在本项目的
     * 依赖组合下更可控，且便于在订阅线程外做超时与清理。
     */
    @GetMapping(value = "/auth/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam("message") String message,
            @RequestParam(value = "history", required = false) List<String> history) {

        Long userId = currentUserId();
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(10).toMillis());
        List<ChatRequest.Message> historyMessages = toHistory(history);

        Disposable subscription = chatService.chat(message, historyMessages, userId)
                .subscribe(
                        chunk -> send(emitter, chunk),
                        error -> {
                            log.warn("对话流出错 userId={}: {}", userId, error.getMessage());
                            send(emitter, ChatChunk.error("AI 服务异常，请稍后重试"));
                            emitter.complete();
                        },
                        emitter::complete);

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            log.info("对话流超时 userId={}", userId);
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(error -> subscription.dispose());

        return emitter;
    }

    /** 非流式探测：确认服务已装配、模型配置是否就绪。不返回任何密钥内容。 */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.build(Map.of(
                "service", "service-ai",
                "enabled", chatService.properties().isEnabled(),
                "client", chatService.clientDescription()), ResultCodeEnum.SUCCESS);
    }

    /** 诊断用：验证入参绑定，不调用模型。 */
    @PostMapping("/echo")
    public Result<Map<String, Object>> echo(@RequestBody(required = false) Map<String, Object> body) {
        return Result.build(Map.of("received", body == null ? Map.of() : body), ResultCodeEnum.SUCCESS);
    }

    private void send(SseEmitter emitter, ChatChunk chunk) {
        if (chunk.isEmptyDelta()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(chunk.eventName()).data(payloadOf(chunk)));
        } catch (IOException | IllegalStateException ex) {
            // 客户端已断开：静默结束，订阅会在 onCompletion/onError 里被释放
            log.debug("推送失败，客户端可能已断开: {}", ex.getMessage());
            emitter.complete();
        }
    }

    /** 统一分片载荷形状，便于前端按同一结构解析。 */
    private static Map<String, Object> payloadOf(ChatChunk chunk) {
        return switch (chunk.type()) {
            case DELTA -> Map.of("content", chunk.content() == null ? "" : chunk.content());
            case DONE -> Map.of("finishReason", chunk.content() == null ? "stop" : chunk.content());
            case ERROR -> Map.of("message", chunk.message() == null ? "未知错误" : chunk.message());
        };
    }

    private static List<ChatRequest.Message> toHistory(List<String> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        // 前端按正序传入，最后一条固定视为用户上一轮输入，其余按交替推断角色
        List<ChatRequest.Message> messages = new java.util.ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            String content = history.get(i);
            if (content == null || content.isBlank()) {
                continue;
            }
            boolean isLast = i == history.size() - 1;
            messages.add(isLast ? ChatRequest.Message.user(content) : ChatRequest.Message.assistant(content));
        }
        return messages;
    }

    private static Long currentUserId() {
        try {
            var userInfo = AuthContextUtil.getUserInfo();
            return userInfo == null ? null : userInfo.getId();
        } catch (Exception ex) {
            // 未经过拦截器时 ThreadLocal 为空，视为匿名
            return null;
        }
    }
}
