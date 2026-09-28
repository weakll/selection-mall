package io.github.weakll.mall.user.service.impl;

import io.github.weakll.mall.model.entity.order.CouponInfo;
import io.github.weakll.mall.model.entity.user.CouponUser;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.user.mapper.CouponInfoMapper;
import io.github.weakll.mall.user.mapper.CouponUserMapper;
import io.github.weakll.mall.utils.AuthContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponInfoMapper couponInfoMapper;
    @Mock
    private CouponUserMapper couponUserMapper;
    @InjectMocks
    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(42L);
        AuthContextUtil.setUserInfo(userInfo);
    }

    @AfterEach
    void clearAuthContext() {
        AuthContextUtil.removeUserInfo();
    }

    @Test
    void rejectsClaimWhenUserIsNotAuthenticated() {
        AuthContextUtil.removeUserInfo();

        assertThrows(IllegalStateException.class, () -> couponService.claimCoupon(7L));
        verify(couponInfoMapper, never()).getByIdForUpdate(any());
    }

    @Test
    void rejectsUnavailableCoupon() {
        when(couponInfoMapper.getByIdForUpdate(7L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> couponService.claimCoupon(7L));
        verify(couponUserMapper, never()).save(any());
    }

    @Test
    void savesClaimAndIncrementsReceivedCount() {
        CouponInfo coupon = new CouponInfo();
        coupon.setId(7L);
        coupon.setPublishStatus(1);
        coupon.setPublishCount(10);
        coupon.setReceiveCount(2);
        coupon.setPerLimit(1);
        coupon.setExpireTime(new Date(System.currentTimeMillis() + 60_000));
        when(couponInfoMapper.getByIdForUpdate(7L)).thenReturn(coupon);
        when(couponUserMapper.countByUserIdAndCouponId(42L, 7L)).thenReturn(0);

        couponService.claimCoupon(7L);

        verify(couponUserMapper).save(any(CouponUser.class));
        verify(couponInfoMapper).updateReceiveCount(7L);
    }
}
