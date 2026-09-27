package io.github.weakll.mall.product.service.impl;

import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductSkuMapper productSkuMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void deductsEverySkuWhenStockIsSufficient() {
        SkuSaleDto first = skuSale(1L, 2);
        SkuSaleDto second = skuSale(2L, 1);
        when(productSkuMapper.deductStock(1L, 2)).thenReturn(1);
        when(productSkuMapper.deductStock(2L, 1)).thenReturn(1);

        Boolean result = productService.deductStock(List.of(first, second));

        assertTrue(result);
        verify(productSkuMapper).deductStock(1L, 2);
        verify(productSkuMapper).deductStock(2L, 1);
    }

    @Test
    void rejectsDeductionWhenAnySkuHasInsufficientStock() {
        SkuSaleDto first = skuSale(1L, 2);
        SkuSaleDto second = skuSale(2L, 5);
        when(productSkuMapper.deductStock(1L, 2)).thenReturn(1);
        when(productSkuMapper.deductStock(2L, 5)).thenReturn(0);

        assertThrows(
                MallException.class,
                () -> productService.deductStock(List.of(first, second))
        );
    }

    @Test
    void restoresEverySku() {
        SkuSaleDto first = skuSale(1L, 2);
        SkuSaleDto second = skuSale(2L, 1);
        when(productSkuMapper.restoreStock(1L, 2)).thenReturn(1);
        when(productSkuMapper.restoreStock(2L, 1)).thenReturn(1);

        Boolean result = productService.restoreStock(List.of(first, second));

        assertTrue(result);
        verify(productSkuMapper).restoreStock(1L, 2);
        verify(productSkuMapper).restoreStock(2L, 1);
    }

    private SkuSaleDto skuSale(Long skuId, Integer num) {
        SkuSaleDto skuSaleDto = new SkuSaleDto();
        skuSaleDto.setSkuId(skuId);
        skuSaleDto.setNum(num);
        return skuSaleDto;
    }
}
