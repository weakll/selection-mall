package io.github.weakll.mall.ai.auth;

import java.util.function.Supplier;

/**
 * 工具调用期的登录凭据上下文。
 *
 * <p><b>为什么需要它：</b>商城的 {@code UserTokenFeignInterceptor} 通过
 * {@code RequestContextHolder} 从当前 HTTP 请求取 {@code token} 头，
 * 再透传给下游服务。该机制依赖 servlet 线程绑定，而 Agent 的工具执行发生在
 * Reactor 的 {@code boundedElastic} 调度器线程上——那里没有请求上下文，
 * 取到的是 null，导致订单查询报 "requestAttributes is null"。
 *
 * <p>因此改为：请求进入时在 servlet 线程上把 token 捕获到本上下文，
 * 工具执行线程上的 Feign 拦截器再从这里取。用显式传递替代隐式线程绑定。
 *
 * <p><b>并发约束：</b>基于 ThreadLocal，而 Reactor 会在线程间切换任务，
 * 因此本上下文只在"一次请求对应一个工具执行线程"这一前提下成立。
 * 当前实现一次请求只由一个订阅驱动串行执行工具，满足该前提；
 * 若将来改为多用户并发复用同一订阅，必须改为随响应式上下文显式传递，
 * 否则存在把 A 的 token 用到 B 的请求上的风险。
 */
public final class ToolAuthContext {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private ToolAuthContext() {
    }

    /** 在当前线程绑定 token。 */
    public static void set(String token) {
        if (token == null || token.isBlank()) {
            TOKEN.remove();
        } else {
            TOKEN.set(token);
        }
    }

    /** 取当前线程绑定的 token；无绑定返回 null。 */
    public static String get() {
        return TOKEN.get();
    }

    /** 清理绑定，避免线程复用造成的串号。 */
    public static void clear() {
        TOKEN.remove();
    }

    /**
     * 在绑定 token 的前提下执行动作，结束后必定清理。
     *
     * <p>工具调用线程上使用：进入时绑定，退出时无条件清理。
     */
    public static <T> T callWith(String token, Supplier<T> action) {
        String previous = TOKEN.get();
        set(token);
        try {
            return action.get();
        } finally {
            // 恢复而非直接清空：支持嵌套调用
            if (previous == null) {
                TOKEN.remove();
            } else {
                TOKEN.set(previous);
            }
        }
    }
}
