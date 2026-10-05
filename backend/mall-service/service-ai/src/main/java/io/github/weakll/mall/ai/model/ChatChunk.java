package io.github.weakll.mall.ai.model;

import java.util.List;

/**
 * 流式响应分片。
 *
 * @param type    分片类型
 * @param content 文本增量，仅 {@link Type#DELTA} 时有值
 * @param message 错误描述，仅 {@link Type#ERROR} 时有值
 */
public record ChatChunk(Type type, String content, String message) {

    public static ChatChunk delta(String text) {
        return new ChatChunk(Type.DELTA, text, null);
    }

    /** 结束分片，携带最终原因（如 stop / length）。 */
    public static ChatChunk done(String finishReason) {
        return new ChatChunk(Type.DONE, finishReason, null);
    }

    public static ChatChunk error(String message) {
        return new ChatChunk(Type.ERROR, null, message);
    }

    public enum Type {
        /** 正文增量 */
        DELTA,
        /** 本轮结束 */
        DONE,
        /** 失败，流终止 */
        ERROR
    }

    /** 供 SSE 传输用的事件名。 */
    public String eventName() {
        return switch (type) {
            case DELTA -> "delta";
            case DONE -> "done";
            case ERROR -> "error";
        };
    }

    /** 空分片：模型可能只返回元数据而无正文，这种不往客户端推。 */
    public boolean isEmptyDelta() {
        return type == Type.DELTA && (content == null || content.isEmpty());
    }

    public static List<ChatChunk> none() {
        return List.of();
    }
}
