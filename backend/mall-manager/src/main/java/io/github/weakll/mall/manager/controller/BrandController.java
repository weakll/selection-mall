package io.github.weakll.mall.manager.controller;

import io.github.weakll.mall.common.log.annotation.Log;
import io.github.weakll.mall.common.log.enums.OperatorType;
import io.github.weakll.mall.manager.service.BrandService;
import io.github.weakll.mall.model.entity.product.Brand;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/admin/product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService ;
    @Log(title = "品牌列表",businessType = 0,operatorType = OperatorType.MANAGE)
    @GetMapping("/{page}/{limit}")
    public Result<PageInfo<Brand>> findByPage(@PathVariable Integer page, @PathVariable Integer limit) {
        PageInfo<Brand> pageInfo = brandService.findByPage(page, limit);
        return Result.build(pageInfo , ResultCodeEnum.SUCCESS) ;
    }
    //添加
    @Log(title = "品牌添加",businessType = 0,operatorType = OperatorType.MANAGE)
    @PostMapping("save")
    public Result save(@RequestBody Brand brand) {
        brandService.save(brand);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
    //修改
    @PutMapping("updateById")
    public Result updateById(@RequestBody Brand brand) {
        brandService.updateById(brand);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
    //删除
    @DeleteMapping("/deleteById/{id}")
    public Result deleteById(@PathVariable Long id) {
        brandService.deleteById(id);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
    //查找所以品牌
    @GetMapping("/findAll")
    public Result findAll() {
        List<Brand> list = brandService.findAll();
        return Result.build(list , ResultCodeEnum.SUCCESS) ;
    }
}
