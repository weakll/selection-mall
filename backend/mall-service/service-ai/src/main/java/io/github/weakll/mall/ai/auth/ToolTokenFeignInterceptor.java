package io.github.weakll.mall.ai.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * 把 {@link ToolAuthContext} 中的登录凭据注入 Feign 请求。
 *
 * <p>替代商城通用的 {@code UserTokenFeignInterceptor}：后者从
 * {@code RequestContextHolder} 取 token，在 Agent 的工具执行线程上取不到
 * （原因见 {@link ToolAuthContext} 的说明）。
 *
 * <p>注意：本服务已在 {@code AiApplication} 上移除
 * {@code @EnableUserTokenFeignInterceptor}，避免两个拦截器都往
 * {@code token} 头写值——其中一个是 null 时会把正确的值盖掉。
 */
@Component
public class ToolTokenFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String token = ToolAuthContext.get();
        if (token != null && !token.isBlank()) {
            template.header("token", token);
        }
    }
}
