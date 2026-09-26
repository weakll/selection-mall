package io.github.weakll.mall.feign.cart;

import io.github.weakll.mall.model.entity.h5.CartInfo;
import io.github.weakll.mall.model.vo.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(value = "service-cart")
@Component
public interface CartFeignClient {

    @GetMapping(value = "/api/order/cart/auth/getAllCkecked")
    public abstract List<CartInfo> getAllCkecked() ;

    @GetMapping(value = "/api/order/cart/auth/deleteChecked")
    public abstract Result<Void> deleteChecked() ;
}
