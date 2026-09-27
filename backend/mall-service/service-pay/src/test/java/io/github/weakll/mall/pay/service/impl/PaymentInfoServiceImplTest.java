package io.github.weakll.mall.pay.service.impl;

import io.github.weakll.mall.feign.order.OrderFeignClient;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.entity.pay.PaymentInfo;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.pay.mapper.PaymentInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentInfoServiceImplTest {

    @Mock
    private PaymentInfoMapper paymentInfoMapper;

    @Mock
    private OrderFeignClient orderFeignClient;

    @Mock
    private ProductFeignClient productFeignClient;

    @InjectMocks
    private PaymentInfoServiceImpl paymentInfoService;

    @Test
    void returnsDirectlyWhenPaymentIsAlreadyPaid() {
        PaymentInfo paymentInfo = paymentInfo(1);
        when(paymentInfoMapper.getByOrderNo("ORDER-1")).thenReturn(paymentInfo);

        paymentInfoService.updatePaymentStatus(callback("ORDER-1"), 2);

        verify(paymentInfoMapper, never()).markPaid(anyString(), anyString(), any(), anyString());
        verifyNoInteractions(orderFeignClient, productFeignClient);
    }

    @Test
    void concurrentLoserReturnsAfterConditionalUpdateFails() {
        when(paymentInfoMapper.getByOrderNo("ORDER-1"))
                .thenReturn(paymentInfo(0), paymentInfo(1));
        when(paymentInfoMapper.markPaid(eq("ORDER-1"), anyString(), any(), anyString())).thenReturn(0);

        paymentInfoService.updatePaymentStatus(callback("ORDER-1"), 2);

        verifyNoInteractions(orderFeignClient, productFeignClient);
    }

    @Test
    void winnerUpdatesOrderAndProductSale() {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderItemList(java.util.List.of());
        when(paymentInfoMapper.getByOrderNo("ORDER-1")).thenReturn(paymentInfo(0));
        when(paymentInfoMapper.markPaid(eq("ORDER-1"), anyString(), any(), anyString())).thenReturn(1);
        when(orderFeignClient.getOrderInfoByOrderNo("ORDER-1"))
                .thenReturn(Result.build(orderInfo, ResultCodeEnum.SUCCESS));

        paymentInfoService.updatePaymentStatus(callback("ORDER-1"), 2);

        verify(orderFeignClient).updateOrderStatus("ORDER-1", 2);
        verify(orderFeignClient).getOrderInfoByOrderNo("ORDER-1");
    }

    private Map<String, String> callback(String orderNo) {
        return Map.of(
                "out_trade_no", orderNo,
                "trade_no", "TRADE-1",
                "trade_status", "TRADE_SUCCESS"
        );
    }

    private PaymentInfo paymentInfo(Integer status) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo("ORDER-1");
        paymentInfo.setPaymentStatus(status);
        return paymentInfo;
    }
}
