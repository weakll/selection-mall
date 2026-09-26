package io.github.weakll.mall.model.entity.user;

import io.github.weakll.mall.model.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "优惠券领用记录")
public class CouponUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物券ID")
    private Long couponId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "购物券状态（1：未使用 2：已使用）")
    private Integer couponStatus;

    @Schema(description = "获取类型（1：后台赠送；2：主动获取）")
    private Integer getType;

    @Schema(description = "获取时间")
    private Date getTime;

    @Schema(description = "使用时间")
    private Date usingTime;

    @Schema(description = "支付时间")
    private Date usedTime;

    @Schema(description = "过期时间")
    private Date expireTime;

}
