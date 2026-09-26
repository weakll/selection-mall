package io.github.weakll.mall.model.vo.h5;

import io.github.weakll.mall.model.entity.product.ProductSku;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "用户收藏视图对象")
public class UserCollectVo {

    @Schema(description = "收藏ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品skuID")
    private Long skuId;

    @Schema(description = "收藏时间")
    private Date createTime;

    @Schema(description = "商品SKU信息")
    private ProductSku productSku;

}
