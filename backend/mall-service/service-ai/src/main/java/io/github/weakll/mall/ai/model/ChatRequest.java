package io.github.weakll.mall.ai.model;

import java.util.List;

/**
 * 对话请求。
 *
 * <p>刻意不直接暴露 AgentScope 的类型：模型适配层是自有的窄接口，
 * 上层与第三方库解耦，后续替换模型或升级库版本时改动被限制在这一层。
 *
 * @param systemPrompt 系统提示词，为空表示不带
 * @param messages     历史消息，按时间正序
 * @param temperature  采样温度
 */
public record ChatRequest(String systemPrompt, List<Message> messages, double temperature) {

    public ChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static ChatRequest of(String systemPrompt, List<Message> messages) {
        return new ChatRequest(systemPrompt, messages, 0.3);
    }

    /** 单条消息。 */
    public record Message(Role role, String content) {

        public static Message user(String content) {
            return new Message(Role.USER, content);
        }

        public static Message assistant(String content) {
            return new Message(Role.ASSISTANT, content);
        }
    }

    /** 消息角色。当前只用到用户与助手两种，系统提示词单独传递。 */
    public enum Role {
        USER,
        ASSISTANT
    }
}
