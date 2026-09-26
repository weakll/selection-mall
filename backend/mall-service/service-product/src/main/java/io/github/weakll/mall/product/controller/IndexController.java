package io.github.weakll.mall.product.controller;


import io.github.weakll.mall.model.entity.product.Category;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.h5.IndexVo;
import io.github.weakll.mall.product.service.CategoryService;
import io.github.weakll.mall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "首页接口管理")
@RestController
@RequestMapping(value = "/api/product/index")
@SuppressWarnings({"unchecked","rawtypes"})
//@CrossOrigin
public class IndexController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;

    @Operation(summary ="获取首页数据")
    @GetMapping
    public Result<IndexVo> findData(){
        List<Category> categoryList = categoryService.findOneCategory();
        List<ProductSku> ProductSkuList = productService.findProductSkuSale();
        IndexVo indexvo = new IndexVo();
        indexvo.setCategoryList(categoryList);
        indexvo.setProductSkuList(ProductSkuList);
        return Result.build(indexvo, ResultCodeEnum.SUCCESS);
    }
}
