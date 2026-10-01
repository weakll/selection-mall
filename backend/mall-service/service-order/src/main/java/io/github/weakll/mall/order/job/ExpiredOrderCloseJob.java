package io.github.weakll.mall.order.job;

import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.order.mapper.OrderInfoMapper;
import io.github.weakll.mall.order.mapper.OrderItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExpiredOrderCloseJob {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductFeignClient productFeignClient;
    private final int timeoutMinutes;
    private final int batchSize;

    public ExpiredOrderCloseJob(
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductFeignClient productFeignClient,
            @Value("${mall.order.unpaid-timeout-minutes:30}") int timeoutMinutes,
            @Value("${mall.order.expire-batch-size:100}") int batchSize) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productFeignClient = productFeignClient;
        this.timeoutMinutes = timeoutMinutes;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${mall.order.expire-scan-delay-ms:60000}")
    public void closeExpiredOrders() {
        Date expireTime = new Date(System.currentTimeMillis() - Duration.ofMinutes(timeoutMinutes).toMillis());
        List<OrderInfo> orders = orderInfoMapper.findPendingOrdersBefore(expireTime, batchSize);
        orders.forEach(this::closeOne);
    }

    @Transactional
    void closeOne(OrderInfo order) {
        int affectedRows = orderInfoMapper.cancelExpiredOrder(
                order.getOrderNo(),
                "订单超时未支付",
                new Date()
        );
        if (affectedRows != 1) {
            return;
        }

        List<SkuSaleDto> stockItems = orderItemMapper.findByOrderId(order.getId()).stream()
                .map(item -> {
                    SkuSaleDto stockItem = new SkuSaleDto();
                    stockItem.setSkuId(item.getSkuId());
                    stockItem.setNum(item.getSkuNum());
                    return stockItem;
                })
                .collect(Collectors.toList());
        if (!stockItems.isEmpty() && !Boolean.TRUE.equals(productFeignClient.restoreStock(stockItems))) {
            log.error("Failed to restore stock for expired order. orderNo={}, items={}", order.getOrderNo(), stockItems);
        }
    }
}
