package io.github.weakll.mall.ai.tool;

/**
 * 工具执行结果。
 *
 * <p>{@code content} 是给模型读的文本或 JSON 字符串。刻意不倒推 AgentScope 的
 * {@code ToolResultBlock}：工具层保持框架无关，便于单测，也便于后续换 Agent 运行时。
 *
 * @param success 是否成功；失败时 content 为可读的失败原因，模型据此转述给用户
 * @param content 结果正文
 */
public record ToolResult(boolean success, String content) {

    public static ToolResult ok(String content) {
        return new ToolResult(true, content);
    }

    public static ToolResult fail(String reason) {
        return new ToolResult(false, reason);
    }
}
