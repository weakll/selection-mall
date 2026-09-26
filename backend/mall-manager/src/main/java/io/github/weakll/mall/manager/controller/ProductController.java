package io.github.weakll.mall.manager.controller;
import io.github.weakll.mall.manager.service.ProductService;
import io.github.weakll.mall.model.dto.product.ProductDto;
import io.github.weakll.mall.model.entity.product.Product;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/admin/product/product")
public class ProductController {

    @Autowired
    private ProductService productService ;
    @GetMapping(value = "/{page}/{limit}")
    public Result<PageInfo<Product>> findByPage(@PathVariable Integer page, @PathVariable Integer limit, ProductDto productDto) {
        PageInfo<Product> pageInfo = productService.findByPage(page, limit, productDto);
        return Result.build(pageInfo , ResultCodeEnum.SUCCESS) ;
    }
    //保存商品数据
    @PostMapping("/save")
    public Result save(@RequestBody Product product) {
        productService.save(product);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
    //修改商品信息
    @GetMapping("/getById/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return Result.build(product , ResultCodeEnum.SUCCESS) ;
    }
    //保存数据
    @PutMapping("/updateById")
    public Result updateById(@Parameter(name = "product", description = "请求参数实体类", required = true) @RequestBody Product product) {
            productService.updateById(product);
            return Result.build(null , ResultCodeEnum.SUCCESS) ;
}
//删除商品
@DeleteMapping("/deleteById/{id}")
public Result deleteById(@Parameter(name = "id", description = "商品id", required = true) @PathVariable Long id) {
    productService.deleteById(id);
    return Result.build(null , ResultCodeEnum.SUCCESS) ;
}
//审核商品
    @GetMapping("/updateAuditStatus/{id}/{auditStatus}")
    public Result updateAuditStatus(@PathVariable Long id, @PathVariable Integer auditStatus) {
        productService.updateAuditStatus(id, auditStatus);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
    //商品上架或下架
    @GetMapping("/updateStatus/{id}/{status}")
    public Result updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        productService.updateStatus(id, status);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
}
