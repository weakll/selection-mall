package io.github.weakll.mall.ai;

import io.github.weakll.mall.common.anno.EnableUserWebMvcConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 客服服务。
 *
 * <p>独立服务，不并入商城既有进程：商城的 Spring Cloud Alibaba 2022.0.0.0-RC2 /
 * Boot 3.0.5 与 AI 侧需要的运行环境不同版本，独立部署是唯一可行形态
 * （见 docs/ai-service.md 的版本边界说明）。
 *
 * <p>排除数据源自动装配：本服务不直接访问任何数据库，
 * 商品与订单数据一律通过 Feign 调用对应服务获取。
 * 而父模块 mall-service 为各业务服务引入了 MyBatis 与 MySQL 驱动，
 * 那些依赖会触发 DataSource 自动装配，导致启动时因缺少 url 而失败。
 * 排除而非补配置，是为了让"不碰数据库"这件事在代码里可读。
 *
 * <p><b>刻意不加 {@code @EnableUserTokenFeignInterceptor}：</b>
 * 那个拦截器从 {@code RequestContextHolder} 取 token，而 Agent 的工具执行在
 * Reactor 调度器线程上，取到 null 并会把 null 写进 {@code token} 头，
 * 覆盖掉正确值。本服务改用 {@code ToolTokenFeignInterceptor}
 * 从 {@code ToolAuthContext} 取（见该类的说明）。
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        MybatisAutoConfiguration.class
})
@EnableUserWebMvcConfiguration
@EnableFeignClients(basePackages = {
        "io.github.weakll.mall.feign.product",
        "io.github.weakll.mall.feign.order"
})
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
