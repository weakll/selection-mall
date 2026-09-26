package io.github.weakll.mall.pay.service;

import io.github.weakll.mall.model.entity.pay.PaymentInfo;

import java.util.Map;

//业务接口
public interface PaymentInfoService {
    PaymentInfo savePaymentInfo(String orderNo);
    void updatePaymentStatus(Map<String, String> map, Integer payType);
}
