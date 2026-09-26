package io.github.weakll.mall.feign.user;

import io.github.weakll.mall.model.entity.user.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-user")
public interface UserFeignClient {

    @GetMapping("/api/user/userAddress/getUserAddress/{id}")
    public abstract UserAddress getUserAddress(@PathVariable Long id) ;

}
