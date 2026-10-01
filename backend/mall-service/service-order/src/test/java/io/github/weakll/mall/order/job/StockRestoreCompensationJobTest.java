package io.github.weakll.mall.order.job;

import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.order.mapper.StockRestoreCompensationMapper;
import io.github.weakll.mall.order.model.StockRestoreCompensation;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockRestoreCompensationJobTest {
    @Test
    void marksCompensationSuccessfulAfterStockRestore() {
        StockRestoreCompensationMapper mapper = mock(StockRestoreCompensationMapper.class);
        ProductFeignClient productFeignClient = mock(ProductFeignClient.class);
        StockRestoreCompensation item = item(1L, 0);
        when(mapper.markProcessing(any(), any())).thenReturn(1);
        when(productFeignClient.restoreStock(any())).thenReturn(true);

        new StockRestoreCompensationJob(mapper, productFeignClient, 50, 10).retryOne(item);

        verify(mapper).markSuccess(eq(1L), any());
        verify(mapper, never()).markRetry(any(), anyInt(), any(), any(), any());
    }

    @Test
    void schedulesRetryWhenProductServiceFails() {
        StockRestoreCompensationMapper mapper = mock(StockRestoreCompensationMapper.class);
        ProductFeignClient productFeignClient = mock(ProductFeignClient.class);
        StockRestoreCompensation item = item(2L, 1);
        when(mapper.markProcessing(any(), any())).thenReturn(1);
        when(productFeignClient.restoreStock(any())).thenReturn(false);

        new StockRestoreCompensationJob(mapper, productFeignClient, 50, 10).retryOne(item);

        verify(mapper).markRetry(eq(2L), eq(2), any(), any(), any());
    }

    private StockRestoreCompensation item(Long id, int retryCount) {
        StockRestoreCompensation item = new StockRestoreCompensation();
        item.setId(id);
        item.setRetryCount(retryCount);
        item.setStockItemsJson("[{\"skuId\":1,\"num\":2}]");
        return item;
    }
}
