package io.github.weakll.mall.order.mapper;

import io.github.weakll.mall.model.entity.order.OrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderItemMapper {
    void save(OrderItem orderItem);

    List<OrderItem> findByOrderId(Long id);
}
