package io.github.weakll.mall.order.job;

import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.entity.order.OrderItem;
import io.github.weakll.mall.order.mapper.OrderInfoMapper;
import io.github.weakll.mall.order.mapper.OrderItemMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpiredOrderCloseJobTest {

    @Test
    void doesNotRestoreStockWhenAnotherRequestAlreadyClosedOrder() {
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        ProductFeignClient productFeignClient = mock(ProductFeignClient.class);
        ExpiredOrderCloseJob job = new ExpiredOrderCloseJob(orderInfoMapper, orderItemMapper, productFeignClient, 30, 100);
        OrderInfo order = order(1L, "order-1");
        when(orderInfoMapper.cancelExpiredOrder(any(), any(), any())).thenReturn(0);

        job.closeOne(order);

        verifyNoInteractions(orderItemMapper, productFeignClient);
    }

    @Test
    void restoresStockOnlyAfterOrderIsAtomicallyClosed() {
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        ProductFeignClient productFeignClient = mock(ProductFeignClient.class);
        ExpiredOrderCloseJob job = new ExpiredOrderCloseJob(orderInfoMapper, orderItemMapper, productFeignClient, 30, 100);
        OrderInfo order = order(1L, "order-1");
        OrderItem item = new OrderItem();
        item.setSkuId(10L);
        item.setSkuNum(2);
        when(orderInfoMapper.cancelExpiredOrder(any(), any(), any())).thenReturn(1);
        when(orderItemMapper.findByOrderId(1L)).thenReturn(List.of(item));
        when(productFeignClient.restoreStock(any())).thenReturn(true);

        job.closeOne(order);

        verify(productFeignClient).restoreStock(argThat(items -> items.size() == 1
                && items.get(0).getSkuId().equals(10L)
                && items.get(0).getNum().equals(2)));
    }

    private OrderInfo order(Long id, String orderNo) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setOrderNo(orderNo);
        return order;
    }
}
