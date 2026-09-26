package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.user.UserCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCollectMapper {
    UserCollect findByUserIdAndSkuId(@Param("userId") Long userId, @Param("skuId") Long skuId);
    void save(UserCollect userCollect);
    void deleteByUserIdAndSkuId(@Param("userId") Long userId, @Param("skuId") Long skuId);
    List<UserCollect> findByUserId(Long userId);
}
