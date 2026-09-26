package io.github.weakll.mall.order;

import io.github.weakll.mall.common.anno.EnableUserTokenFeignInterceptor;
import io.github.weakll.mall.common.anno.EnableUserWebMvcConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@EnableUserTokenFeignInterceptor
@EnableUserWebMvcConfiguration
@SpringBootApplication
@EnableFeignClients(basePackages = {
        "io.github.weakll.mall.feign.cart",
        "io.github.weakll.mall.feign.user",
        "io.github.weakll.mall.feign.product"
})

public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class , args) ;
    }

}
