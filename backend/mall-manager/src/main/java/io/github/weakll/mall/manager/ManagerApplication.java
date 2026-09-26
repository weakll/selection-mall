package io.github.weakll.mall.manager;

import io.github.weakll.mall.common.log.annotation.EnableLogAspect;
import io.github.weakll.mall.manager.properties.MinioProperties;
import io.github.weakll.mall.manager.properties.UserAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableAsync
@EnableLogAspect
@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = "io.github.weakll.mall")
@EnableConfigurationProperties(value = {UserAuthProperties.class, MinioProperties.class})
public class
   ManagerApplication {
    public static void main(String[] args) {

        SpringApplication.run(ManagerApplication.class , args) ;

    }
}
