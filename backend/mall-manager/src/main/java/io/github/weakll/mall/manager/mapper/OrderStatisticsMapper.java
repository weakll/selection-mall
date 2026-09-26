package io.github.weakll.mall.manager.mapper;

import io.github.weakll.mall.model.dto.order.OrderStatisticsDto;
import io.github.weakll.mall.model.entity.order.OrderStatistics;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderStatisticsMapper {
    void insert(OrderStatistics orderStatistics);

    List<OrderStatistics> selectList(OrderStatisticsDto orderStatisticsDto);
}
