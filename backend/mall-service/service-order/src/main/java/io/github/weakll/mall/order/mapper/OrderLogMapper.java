package io.github.weakll.mall.order.mapper;

import io.github.weakll.mall.model.entity.order.OrderLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderLogMapper {
    void save(OrderLog orderLog);
}
