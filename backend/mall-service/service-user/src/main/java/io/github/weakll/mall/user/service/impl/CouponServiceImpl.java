package io.github.weakll.mall.user.service.impl;

import io.github.weakll.mall.model.entity.order.CouponInfo;
import io.github.weakll.mall.model.entity.user.CouponUser;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.user.mapper.CouponInfoMapper;
import io.github.weakll.mall.user.mapper.CouponUserMapper;
import io.github.weakll.mall.user.service.CouponService;
import io.github.weakll.mall.utils.AuthContextUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponInfoMapper couponInfoMapper;
    @Autowired
    private CouponUserMapper couponUserMapper;

    @PostConstruct
    public void init() {
        try {
            initCouponData();
        } catch (Exception e) {
            System.err.println("[CouponService] 启动时自动初始化优惠券失败: " + e.getMessage());
        }
    }

    @Override
    public void initCouponData() {
        List<CouponInfo> list = couponInfoMapper.findAllPublished();
        if (list != null && !list.isEmpty()) {
            return;
        }
        // 初始化3张模拟优惠券
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 3);
        Date expire = cal.getTime();

        // 5元无门槛现金券
        CouponInfo c1 = new CouponInfo();
        c1.setCouponType(1);
        c1.setCouponName("新人5元现金券");
        c1.setAmount(new BigDecimal("5"));
        c1.setConditionAmount(new BigDecimal("0"));
        c1.setRangeType(1);
        c1.setRangeDesc("全场通用");
        c1.setPublishCount(9999);
        c1.setPerLimit(1);
        c1.setUseCount(0);
        c1.setReceiveCount(0);
        c1.setExpireTime(expire);
        c1.setPublishStatus(1);
        couponInfoMapper.save(c1);

        // 10元无门槛现金券
        CouponInfo c2 = new CouponInfo();
        c2.setCouponType(1);
        c2.setCouponName("新人10元现金券");
        c2.setAmount(new BigDecimal("10"));
        c2.setConditionAmount(new BigDecimal("0"));
        c2.setRangeType(1);
        c2.setRangeDesc("全场通用");
        c2.setPublishCount(9999);
        c2.setPerLimit(1);
        c2.setUseCount(0);
        c2.setReceiveCount(0);
        c2.setExpireTime(expire);
        c2.setPublishStatus(1);
        couponInfoMapper.save(c2);

        // 满50减15满减券
        CouponInfo c3 = new CouponInfo();
        c3.setCouponType(2);
        c3.setCouponName("满50减15券");
        c3.setAmount(new BigDecimal("15"));
        c3.setConditionAmount(new BigDecimal("50"));
        c3.setRangeType(1);
        c3.setRangeDesc("全场通用");
        c3.setPublishCount(9999);
        c3.setPerLimit(1);
        c3.setUseCount(0);
        c3.setReceiveCount(0);
        c3.setExpireTime(expire);
        c3.setPublishStatus(1);
        couponInfoMapper.save(c3);
    }

    @Override
    public void giveNewUserCoupon(Long userId) {
        List<CouponInfo> list = couponInfoMapper.findAllPublished();
        for (CouponInfo coupon : list) {
            CouponUser exist = couponUserMapper.findByUserIdAndCouponId(userId, coupon.getId());
            if (exist != null) {
                continue;
            }
            CouponUser cu = new CouponUser();
            cu.setCouponId(coupon.getId());
            cu.setUserId(userId);
            cu.setCouponStatus(1);
            cu.setGetType(1);
            cu.setGetTime(new Date());
            cu.setExpireTime(coupon.getExpireTime());
            couponUserMapper.save(cu);
            couponInfoMapper.updateReceiveCount(coupon.getId());
        }
    }

    @Override
    public List<CouponUser> findUserCouponList() {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            return null;
        }
        return couponUserMapper.findByUserId(userInfo.getId());
    }

    @Override
    public List<CouponInfo> findAllPublished() {
        return couponInfoMapper.findAllPublished();
    }

    @Override
    public CouponInfo getById(Long id) {
        return couponInfoMapper.getById(id);
    }
}
