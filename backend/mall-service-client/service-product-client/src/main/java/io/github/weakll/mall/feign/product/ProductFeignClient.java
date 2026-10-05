package io.github.weakll.mall.feign.product;


import io.github.weakll.mall.model.dto.h5.ProductSkuDto;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.vo.common.PageResult;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.h5.ProductItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "service-product")
public interface ProductFeignClient {

    @GetMapping("/api/product/getBySkuId/{skuId}")
    public abstract ProductSku getBySkuId(@PathVariable Long skuId) ;

    /**
     * 按关键词搜索商品（取首页结果）。
     *
     * <p>复用商品服务既有的 {@code GET /api/product/{page}/{limit}}，其查询对象
     * {@link ProductSkuDto} 已支持 keyword / brandId / 三级分类过滤，
     * 因此新增 AI 能力无需改动商品服务任何代码。
     *
     * <p>返回 {@code Result<PageResult<ProductSku>>}：该接口底层同样返回 pagehelper 的
     * {@code PageInfo}，即 {@code data:{"total":n,"list":[...]}}。
     * 写成 {@code Result<List<ProductSku>>} 会导致 Jackson 反序列化失败。
     *
     * @param page    页码，从 1 开始
     * @param limit   每页条数
     * @param keyword 商品名称关键词，可为空
     */
    @GetMapping("/api/product/{page}/{limit}")
    Result<PageResult<ProductSku>> searchByPage(@PathVariable("page") Integer page,
                                                @PathVariable("limit") Integer limit,
                                                @RequestParam(value = "keyword", required = false) String keyword);

    /** 商品详情（含 SKU 列表、属性、图片），用于回答规格类问题。 */
    @GetMapping("/api/product/item/{skuId}")
    Result<ProductItemVo> getItem(@PathVariable("skuId") Long skuId);

    @PostMapping("/internal/product/updateSkuSaleNum")
    Boolean updateSkuSaleNum(@RequestBody List<SkuSaleDto> skuSaleDtoList);

    @PostMapping("/internal/product/deductStock")
    Boolean deductStock(@RequestBody List<SkuSaleDto> skuSaleDtoList);

    @PostMapping("/internal/product/restoreStock")
    Boolean restoreStock(@RequestBody List<SkuSaleDto> skuSaleDtoList);
}
