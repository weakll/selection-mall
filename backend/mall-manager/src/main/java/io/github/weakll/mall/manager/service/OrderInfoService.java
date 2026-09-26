package io.github.weakll.mall.manager.service;

import io.github.weakll.mall.model.dto.order.OrderStatisticsDto;
import io.github.weakll.mall.model.vo.order.OrderStatisticsVo;
import org.springframework.stereotype.Service;


public interface OrderInfoService {
    OrderStatisticsVo getOrderStatisticsData(OrderStatisticsDto orderStatisticsDto);
}
