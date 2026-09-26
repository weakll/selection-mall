package io.github.weakll.mall.user.service;

import io.github.weakll.mall.model.entity.order.CouponInfo;
import io.github.weakll.mall.model.entity.user.CouponUser;

import java.util.List;

public interface CouponService {

    void initCouponData();

    void giveNewUserCoupon(Long userId);

    List<CouponUser> findUserCouponList();

    List<CouponInfo> findAllPublished();

    CouponInfo getById(Long id);
}
