package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.entity.order.OrderStatistics;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper {
    OrderStatistics selectOrderStatistics(String createTime);
}
