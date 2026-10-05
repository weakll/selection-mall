package io.github.weakll.mall.ai.model;

import reactor.core.publisher.Flux;

/**
 * 对话模型客户端。
 *
 * <p>自有的窄接口：上层（Agent、Controller）只依赖它，不直接依赖 AgentScope，
 * 后续换模型或升级第三方库时改动被限制在实现类。
 */
public interface ChatClient {

    /**
     * 流式对话。
     *
     * <p>返回的流以 {@link ChatChunk.Type#DONE} 正常收尾；
     * 出错时发出 {@link ChatChunk.Type#ERROR} 并终止，不抛给订阅者。
     *
     * @param request 对话请求
     * @param token   当前登录凭据，供工具调用下游服务使用；未登录取自 null
     */
    Flux<ChatChunk> stream(ChatRequest request, String token);

    /** 实现标识，用于诊断输出。 */
    String describe();
}
