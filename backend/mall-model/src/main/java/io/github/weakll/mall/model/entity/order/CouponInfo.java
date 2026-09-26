package io.github.weakll.mall.model.entity.order;

import io.github.weakll.mall.model.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "优惠券信息")
public class CouponInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠券类型 1 现金券 2 满减券")
    private Integer couponType;

    @Schema(description = "优惠券名字")
    private String couponName;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "使用门槛 0->没门槛")
    private BigDecimal conditionAmount;

    @Schema(description = "可以领取的开始日期")
    private Date startTime;

    @Schema(description = "可以领取的结束日期")
    private Date endTime;

    @Schema(description = "使用范围[1->全场通用；2->指定分类；3->指定商品]")
    private Integer rangeType;

    @Schema(description = "使用范围描述")
    private String rangeDesc;

    @Schema(description = "发行数量")
    private Integer publishCount;

    @Schema(description = "每人限领张数")
    private Integer perLimit;

    @Schema(description = "已使用数量")
    private Integer useCount;

    @Schema(description = "领取数量")
    private Integer receiveCount;

    @Schema(description = "过期时间")
    private Date expireTime;

    @Schema(description = "发布状态[0-未发布，1-已发布]")
    private Integer publishStatus;

}
