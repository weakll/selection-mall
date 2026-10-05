package io.github.weakll.mall.ai;

import io.github.weakll.mall.common.anno.EnableUserWebMvcConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 客服服务。
 *
 * <p>独立服务，不并入商城既有进程：商城的 Spring Cloud Alibaba 2022.0.0.0-RC2 /
 * Boot 3.0.5 与 AI 侧需要的运行环境不同版本，独立部署是唯一可行形态
 * （见 docs/ai-service.md 的版本边界说明）。
 *
 * <p><b>关于数据源：</b>本服务不访问商城的业务库（商品、订单数据一律经 Feign 获取），
 * 但需要自己的数据源存放售后政策知识库的检索索引（{@code faq_knowledge} 表，
 * 含 MySQL ngram 全文索引）。因此这里仍排除 MyBatis 自动装配——本服务不使用 Mapper，
 * 只用 JdbcTemplate 访问这一张表。
 */
@SpringBootApplication(exclude = {
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
