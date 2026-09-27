package io.github.weakll.mall.product.controller;

import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/product")
public class ProductInternalController {

    @Autowired
    private ProductService productService;

    @PostMapping("updateSkuSaleNum")
    public Boolean updateSkuSaleNum(@RequestBody List<SkuSaleDto> skuSaleDtoList) {
        return productService.updateSkuSaleNum(skuSaleDtoList);
    }

    @PostMapping("deductStock")
    public Boolean deductStock(@RequestBody List<SkuSaleDto> skuSaleDtoList) {
        return productService.deductStock(skuSaleDtoList);
    }

    @PostMapping("restoreStock")
    public Boolean restoreStock(@RequestBody List<SkuSaleDto> skuSaleDtoList) {
        return productService.restoreStock(skuSaleDtoList);
    }
}
