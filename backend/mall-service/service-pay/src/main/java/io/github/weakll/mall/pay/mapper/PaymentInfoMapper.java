package io.github.weakll.mall.pay.mapper;

import io.github.weakll.mall.model.entity.pay.PaymentInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentInfoMapper {
    PaymentInfo getByOrderNo(String orderNo);

    void save(PaymentInfo paymentInfo);

    void updateById(PaymentInfo paymentInfo);
}
