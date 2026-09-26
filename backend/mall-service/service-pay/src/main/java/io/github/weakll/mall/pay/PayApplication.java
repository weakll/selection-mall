package io.github.weakll.mall.pay;

import io.github.weakll.mall.common.anno.EnableUserWebMvcConfiguration;
import io.github.weakll.mall.pay.properties.AlipayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableConfigurationProperties(value = { AlipayProperties.class })
@EnableUserWebMvcConfiguration
@EnableFeignClients(basePackages = {
        "io.github.weakll.mall.feign.order"
        , "io.github.weakll.mall.feign.product"
})
public class PayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class , args) ;
    }

}
