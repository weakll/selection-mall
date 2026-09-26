package io.github.weakll.mall.product.controller;

import io.github.weakll.mall.model.entity.product.Category;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
//@CrossOrigin
@Tag(name = "分类接口管理")
@RestController
@RequestMapping(value = "/api/product/category")
@SuppressWarnings({"unchecked","rawtypes"})
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @Operation(summary ="获取分类数据")
    @GetMapping("findCategoryTree")
    public Result<Category> findCategoryTree(){
        List<Category>list = categoryService.findOneCategoryTree();
        return Result.build(list, ResultCodeEnum.SUCCESS);
    }
}
