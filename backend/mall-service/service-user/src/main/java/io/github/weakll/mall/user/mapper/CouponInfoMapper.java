package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.order.CouponInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouponInfoMapper {

    List<CouponInfo> findAllPublished();

    CouponInfo getById(Long id);

    void save(CouponInfo couponInfo);

    void updateReceiveCount(Long id);
}
