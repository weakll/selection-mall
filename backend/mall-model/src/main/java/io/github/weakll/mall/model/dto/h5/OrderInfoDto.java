package io.github.weakll.mall.model.dto.h5;

import io.github.weakll.mall.model.entity.order.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderInfoDto {

    //送货地址id
    private Long userAddressId;

    //运费
    private BigDecimal feightFee;

    //备注
    private String remark;

    //订单明细
    private List<OrderItem> orderItemList;
    //使用的优惠券ID
    private Long couponId;

    //优惠券减免金额
    private BigDecimal couponAmount;
}
