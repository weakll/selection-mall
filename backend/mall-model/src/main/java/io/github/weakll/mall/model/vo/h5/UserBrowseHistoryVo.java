package io.github.weakll.mall.model.vo.h5;

import io.github.weakll.mall.model.entity.product.ProductSku;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "用户浏览记录视图对象")
public class UserBrowseHistoryVo {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品skuID")
    private Long skuId;

    @Schema(description = "浏览时间")
    private Date createTime;

    @Schema(description = "商品SKU信息")
    private ProductSku productSku;

}
