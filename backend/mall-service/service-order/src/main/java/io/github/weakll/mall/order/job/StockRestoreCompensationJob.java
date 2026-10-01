package io.github.weakll.mall.order.job;

import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.order.mapper.StockRestoreCompensationMapper;
import io.github.weakll.mall.order.model.StockRestoreCompensation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class StockRestoreCompensationJob {
    private final StockRestoreCompensationMapper mapper;
    private final ProductFeignClient productFeignClient;
    private final int batchSize;
    private final int maxRetries;

    public StockRestoreCompensationJob(
            StockRestoreCompensationMapper mapper,
            ProductFeignClient productFeignClient,
            @Value("${mall.order.restore-compensation-batch-size:50}") int batchSize,
            @Value("${mall.order.restore-compensation-max-retries:10}") int maxRetries) {
        this.mapper = mapper;
        this.productFeignClient = productFeignClient;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${mall.order.restore-compensation-scan-delay-ms:60000}")
    public void retryPendingRestores() {
        List<StockRestoreCompensation> items = mapper.findPending(batchSize);
        items.forEach(this::retryOne);
    }

    void retryOne(StockRestoreCompensation item) {
        if (mapper.markProcessing(item.getId(), new Date()) != 1) {
            return;
        }
        try {
            boolean restored = Boolean.TRUE.equals(productFeignClient.restoreStock(
                    JSON.parseArray(item.getStockItemsJson(), io.github.weakll.mall.model.dto.product.SkuSaleDto.class)));
            if (restored) {
                mapper.markSuccess(item.getId(), new Date());
                return;
            }
            retry(item, "商品服务拒绝库存回补");
        } catch (Exception exception) {
            retry(item, exception.getMessage());
        }
    }

    private void retry(StockRestoreCompensation item, String errorMessage) {
        int retryCount = item.getRetryCount() == null ? 0 : item.getRetryCount() + 1;
        long delayMinutes = Math.min(60, Math.max(1, 1L << Math.min(retryCount, 6)));
        Date nextRetryTime = new Date(System.currentTimeMillis() + Duration.ofMinutes(delayMinutes).toMillis());
        mapper.markRetry(item.getId(), retryCount, errorMessage, nextRetryTime, new Date());
        if (retryCount >= maxRetries) {
            log.error("Stock restore compensation reached retry limit. id={}, orderNo={}, retryCount={}",
                    item.getId(), item.getOrderNo(), retryCount);
        }
    }
}
