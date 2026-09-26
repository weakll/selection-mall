package io.github.weakll.mall.order.service;

import io.github.weakll.mall.model.dto.h5.OrderInfoDto;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.vo.h5.TradeVo;
import com.github.pagehelper.PageInfo;

public interface OrderInfoService {
    TradeVo getTrade();

    Long submitOrder(OrderInfoDto orderInfoDto);

    OrderInfo getOrderInfo(Long orderId);

    TradeVo buy(Long skuId);

    PageInfo<OrderInfo> findUserPage(Integer page, Integer limit, Integer orderStatus);
    // 业务接口
    OrderInfo getByOrderNo(String orderNo) ;

    void updateOrderStatus(String orderNo, Integer orderStatus);
}
