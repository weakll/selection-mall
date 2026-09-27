package io.github.weakll.mall.pay.mapper;

import io.github.weakll.mall.model.entity.pay.PaymentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface PaymentInfoMapper {
    PaymentInfo getByOrderNo(String orderNo);

    void save(PaymentInfo paymentInfo);

    void updateById(PaymentInfo paymentInfo);

    int markPaid(
            @Param("orderNo") String orderNo,
            @Param("outTradeNo") String outTradeNo,
            @Param("callbackTime") Date callbackTime,
            @Param("callbackContent") String callbackContent
    );
}
