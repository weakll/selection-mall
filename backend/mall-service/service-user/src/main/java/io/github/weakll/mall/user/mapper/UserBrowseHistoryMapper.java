package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.user.UserBrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBrowseHistoryMapper {

    UserBrowseHistory findByUserIdAndSkuId(@Param("userId") Long userId, @Param("skuId") Long skuId);

    void save(UserBrowseHistory userBrowseHistory);

    void updateTimeById(Long id);

    List<UserBrowseHistory> findByUserId(Long userId);

    void clearByUserId(Long userId);
}
