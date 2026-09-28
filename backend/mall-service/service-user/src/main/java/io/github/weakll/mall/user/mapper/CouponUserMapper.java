package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.user.CouponUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponUserMapper {

    List<CouponUser> findByUserId(Long userId);

    CouponUser findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    void save(CouponUser couponUser);

    void updateUsed(@Param("id") Long id, @Param("orderId") Long orderId);
}
