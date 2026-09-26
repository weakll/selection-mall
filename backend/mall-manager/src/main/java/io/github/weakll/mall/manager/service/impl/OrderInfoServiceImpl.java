package io.github.weakll.mall.manager.service.impl;

import cn.hutool.core.date.DateUtil;
import io.github.weakll.mall.manager.mapper.OrderStatisticsMapper;
import io.github.weakll.mall.manager.service.OrderInfoService;
import io.github.weakll.mall.model.dto.order.OrderStatisticsDto;
import io.github.weakll.mall.model.entity.order.OrderStatistics;
import io.github.weakll.mall.model.vo.order.OrderStatisticsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderInfoServiceImpl implements OrderInfoService {
    @Autowired
    private OrderStatisticsMapper orderStatisticsMapper ;
    @Override
    public OrderStatisticsVo getOrderStatisticsData(OrderStatisticsDto orderStatisticsDto)
    {
        // 查询统计结果数据
        List<OrderStatistics> orderStatisticsList = orderStatisticsMapper.selectList(orderStatisticsDto) ;
        //⽇期列表
        List<String> dateList = orderStatisticsList.stream().map(orderStatistics -> DateUtil.format(orderStatistics.getOrderDate(), "yyyy-MM-dd")).collect(Collectors.toList());
        //统计⾦额列表
        List<BigDecimal> amountList = orderStatisticsList.stream().map(OrderStatistics::getTotalAmount).collect(Collectors.toList());
        // 创建OrderStatisticsVo对象封装响应结果数据
        OrderStatisticsVo orderStatisticsVo = new OrderStatisticsVo() ;
        orderStatisticsVo.setDateList(dateList);
        orderStatisticsVo.setAmountList(amountList);
        // 返回数据
        return orderStatisticsVo;
    }
}
