package io.github.weakll.mall.order.mapper;

import io.github.weakll.mall.model.entity.order.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface OrderInfoMapper {
    void save(OrderInfo orderInfo);

    OrderInfo getById(Long orderId);

    List<OrderInfo> findUserPage(Long userId, Integer orderStatus);
    OrderInfo getByOrderNo(String orderNo) ;

    void updateById(OrderInfo orderInfo);

    int markPaid(
            @Param("orderNo") String orderNo,
            @Param("payType") Integer payType,
            @Param("paymentTime") Date paymentTime
    );

    int cancelPendingOrder(@Param("orderNo") String orderNo,
                           @Param("userId") Long userId,
                           @Param("cancelReason") String cancelReason,
                           @Param("cancelTime") Date cancelTime);

    List<OrderInfo> findPendingOrdersBefore(@Param("expireTime") Date expireTime,
                                            @Param("limit") int limit);

    int cancelExpiredOrder(@Param("orderNo") String orderNo,
                           @Param("cancelReason") String cancelReason,
                           @Param("cancelTime") Date cancelTime);
}
