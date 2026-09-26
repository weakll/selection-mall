package io.github.weakll.mall.model.entity.user;

import io.github.weakll.mall.model.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户浏览记录实体类")
public class UserBrowseHistory extends BaseEntity {

   private static final long serialVersionUID = 1L;

   @Schema(description = "用户ID")
   private Long userId;

   @Schema(description = "商品skuID")
   private Long skuId;

}
