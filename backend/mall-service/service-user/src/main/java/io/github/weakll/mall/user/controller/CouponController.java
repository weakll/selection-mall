package io.github.weakll.mall.user.controller;

import io.github.weakll.mall.model.entity.order.CouponInfo;
import io.github.weakll.mall.model.entity.user.CouponUser;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.user.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "优惠券接口")
@RestController
@RequestMapping("api/user/couponInfo/")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Operation(summary = "查询用户可用优惠券列表")
    @GetMapping("auth/findUserCouponList")
    public Result<List<CouponUser>> findUserCouponList() {
        return Result.build(couponService.findUserCouponList(), ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "查询所有发布的优惠券")
    @GetMapping("findAllPublished")
    public Result<List<CouponInfo>> findAllPublished() {
        return Result.build(couponService.findAllPublished(), ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "根据ID查询优惠券")
    @GetMapping("getById/{id}")
    public Result<CouponInfo> getById(@PathVariable Long id) {
        return Result.build(couponService.getById(id), ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("auth/claim/{id}")
    public Result<Void> claim(@PathVariable Long id) {
        couponService.claimCoupon(id);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }
}
