package io.github.weakll.mall.manager.config;

import io.github.weakll.mall.manager.interceptor.LoginAuthInterceptor;
import io.github.weakll.mall.manager.properties.UserAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Autowired
    private LoginAuthInterceptor loginAuthInterceptor ;
    @Autowired
    private UserAuthProperties userAuthProperties ;
    //跨域配置
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")      // 添加路径规则
                    .allowCredentials(true)               // 是否允许在跨域的情况下传递Cookie
                    .allowedOriginPatterns("*")           // 允许请求来源的域规则
                    .allowedMethods("*")
                    .allowedHeaders("*") ;                // 允许所有的请求头
        }



        //添加拦截器
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(loginAuthInterceptor)
                    .excludePathPatterns(userAuthProperties.getNoAuthUrls())
                    .addPathPatterns("/**");
    }

}
