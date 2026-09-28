package io.github.weakll.mall.order.service.impl;

import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.feign.cart.CartFeignClient;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.feign.user.UserFeignClient;
import io.github.weakll.mall.model.dto.h5.OrderInfoDto;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.model.entity.h5.CartInfo;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.entity.order.OrderItem;
import io.github.weakll.mall.model.entity.order.OrderLog;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.entity.user.UserAddress;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.h5.TradeVo;
import io.github.weakll.mall.order.mapper.OrderInfoMapper;
import io.github.weakll.mall.order.mapper.OrderItemMapper;
import io.github.weakll.mall.order.mapper.OrderLogMapper;
import io.github.weakll.mall.order.service.OrderInfoService;
import io.github.weakll.mall.utils.AuthContextUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderInfoServiceImpl implements OrderInfoService {

    private static final String ORDER_SUBMIT_KEY_PREFIX = "mall:order:submit:";
    private static final String PENDING_VALUE = "PENDING";

    @Autowired
    private CartFeignClient cartFeignClient ;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public TradeVo getTrade() {

        // 获取当前登录的用户的id
        //Long userId = AuthContextUtil.getUserInfo().getId();

        // 获取选中的购物项列表数据
        List<CartInfo> cartInfoList = cartFeignClient.getAllCkecked() ;
        List<OrderItem> orderItemList = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {        // 将购物项数据转换成功订单明细数据
            OrderItem orderItem = new OrderItem();
            orderItem.setSkuId(cartInfo.getSkuId());
            orderItem.setSkuName(cartInfo.getSkuName());
            orderItem.setSkuNum(cartInfo.getSkuNum());
            orderItem.setSkuPrice(cartInfo.getCartPrice());
            orderItem.setThumbImg(cartInfo.getImgUrl());
            orderItemList.add(orderItem);
        }

        // 计算总金额
        BigDecimal totalAmount = new BigDecimal(0);
        for(OrderItem orderItem : orderItemList) {
            totalAmount = totalAmount.add(orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum())));
        }
        TradeVo tradeVo = new TradeVo();
        tradeVo.setTotalAmount(totalAmount);
        tradeVo.setOrderItemList(orderItemList);
        return tradeVo;

    }
    @Transactional
    @Override
    public Long submitOrder(OrderInfoDto orderInfoDto) {
        List<OrderItem> orderItemList = orderInfoDto.getOrderItemList();
        if (CollectionUtils.isEmpty(orderItemList)) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        String requestId = orderInfoDto.getRequestId();
        if (!StringUtils.hasText(requestId)) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        UserInfo userInfo = AuthContextUtil.getUserInfo();
        Long userId = userInfo.getId();
        String idempotencyKey = ORDER_SUBMIT_KEY_PREFIX + userId + ":" + requestId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                idempotencyKey,
                PENDING_VALUE,
                Duration.ofMinutes(2)
        );
        if (!Boolean.TRUE.equals(acquired)) {
            String existingOrderId = redisTemplate.opsForValue().get(idempotencyKey);
            if (existingOrderId != null && !PENDING_VALUE.equals(existingOrderId)) {
                try {
                    return Long.valueOf(existingOrderId);
                } catch (NumberFormatException exception) {
                    throw new MallException(ResultCodeEnum.DATA_ERROR);
                }
            }
            throw new MallException(ResultCodeEnum.REPEAT_SUBMIT);
        }

        List<SkuSaleDto> stockItems = new ArrayList<>(orderItemList.size());
        boolean stockDeducted = false;
        try {
            for (OrderItem orderItem : orderItemList) {
                ProductSku productSku = productFeignClient.getBySkuId(orderItem.getSkuId());
                if (null == productSku
                        || orderItem.getSkuNum() == null
                        || orderItem.getSkuNum() <= 0) {
                    throw new MallException(ResultCodeEnum.DATA_ERROR);
                }
                SkuSaleDto stockItem = new SkuSaleDto();
                stockItem.setSkuId(orderItem.getSkuId());
                stockItem.setNum(orderItem.getSkuNum());
                stockItems.add(stockItem);
            }

            // 构建订单数据，保存订单
            OrderInfo orderInfo = new OrderInfo();
            //订单编号
            orderInfo.setOrderNo(String.valueOf(System.currentTimeMillis()));
            //用户id
            orderInfo.setUserId(userId);
            //用户昵称
            orderInfo.setNickName(userInfo.getNickName());
            //用户收货地址信息
            UserAddress userAddress = userFeignClient.getUserAddress(orderInfoDto.getUserAddressId());
            orderInfo.setReceiverName(userAddress.getName());
            orderInfo.setReceiverPhone(userAddress.getPhone());
            orderInfo.setReceiverTagName(userAddress.getTagName());
            orderInfo.setReceiverProvince(userAddress.getProvinceCode());
            orderInfo.setReceiverCity(userAddress.getCityCode());
            orderInfo.setReceiverDistrict(userAddress.getDistrictCode());
            orderInfo.setReceiverAddress(userAddress.getFullAddress());
            //订单金额
            BigDecimal totalAmount = new BigDecimal(0);
            for (OrderItem orderItem : orderItemList) {
                totalAmount = totalAmount.add(orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum())));
            }
            orderInfo.setTotalAmount(totalAmount);
            orderInfo.setCouponAmount(new BigDecimal(0));
            orderInfo.setOriginalTotalAmount(totalAmount);
            orderInfo.setFeightFee(orderInfoDto.getFeightFee());
            orderInfo.setPayType(2);
            orderInfo.setOrderStatus(0);

            Boolean deducted = productFeignClient.deductStock(stockItems);
            if (!Boolean.TRUE.equals(deducted)) {
                throw new MallException(ResultCodeEnum.STOCK_LESS);
            }
            stockDeducted = true;

            orderInfoMapper.save(orderInfo);

            //保存订单明细
            for (OrderItem orderItem : orderItemList) {
                orderItem.setOrderId(orderInfo.getId());
                orderItemMapper.save(orderItem);
            }

            //记录日志
            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(orderInfo.getId());
            orderLog.setProcessStatus(0);
            orderLog.setNote("提交订单");
            orderLogMapper.save(orderLog);

            clearCheckedCart(userId);
            registerIdempotencyResult(idempotencyKey, orderInfo.getId());

            // 6、返回订单id
            return orderInfo.getId();
        } catch (RuntimeException | Error exception) {
            if (stockDeducted) {
                restoreReservedStock(stockItems, exception);
            }
            releaseOrderSubmitKey(idempotencyKey);
            throw exception;
        }
    }

    private void clearCheckedCart(Long userId) {
        try {
            String cartKey = "user:cart:" + userId;
            List<Object> objectList = redisTemplate.opsForHash().values(cartKey);
            if (CollectionUtils.isEmpty(objectList)) {
                return;
            }

            objectList.stream()
                    .map(cartInfoJSON -> JSON.parseObject(cartInfoJSON.toString(), CartInfo.class))
                    .filter(cartInfo -> Integer.valueOf(1).equals(cartInfo.getIsChecked()))
                    .forEach(cartInfo -> redisTemplate.opsForHash().delete(
                            cartKey,
                            String.valueOf(cartInfo.getSkuId())
                    ));
        } catch (Exception exception) {
            log.warn("Failed to clear checked cart items. userId={}", userId, exception);
        }
    }

    private void registerIdempotencyResult(String idempotencyKey, Long orderId) {
        Runnable complete = () -> {
            try {
                redisTemplate.opsForValue().set(
                        idempotencyKey,
                        String.valueOf(orderId),
                        Duration.ofHours(24)
                );
            } catch (Exception exception) {
                log.error("Failed to persist order idempotency result. key={}", idempotencyKey, exception);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    complete.run();
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        releaseOrderSubmitKey(idempotencyKey);
                    }
                }
            });
        } else {
            complete.run();
        }
    }

    private void releaseOrderSubmitKey(String idempotencyKey) {
        try {
            redisTemplate.delete(idempotencyKey);
        } catch (Exception exception) {
            log.warn("Failed to release order idempotency key. key={}", idempotencyKey, exception);
        }
    }

    private void restoreReservedStock(List<SkuSaleDto> stockItems, Throwable cause) {
        try {
            Boolean restored = productFeignClient.restoreStock(stockItems);
            if (!Boolean.TRUE.equals(restored)) {
                log.error("Product service rejected stock restoration. items={}", stockItems, cause);
            }
        } catch (Exception restoreException) {
            log.error("Failed to restore reserved stock after order persistence failure. items={}",
                    stockItems, restoreException);
        }
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return orderInfoMapper.getById(orderId);
    }

    @Override
    public TradeVo buy(Long skuId) {
        ProductSku productSku = productFeignClient.getBySkuId(skuId);
        List<OrderItem> orderItemList = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setSkuId(skuId);
        orderItem.setSkuName(productSku.getSkuName());
        orderItem.setSkuNum(1);
        orderItem.setSkuPrice(productSku.getSalePrice());
        orderItem.setThumbImg(productSku.getThumbImg());
        orderItemList.add(orderItem);

        // 计算总金额
        BigDecimal totalAmount = productSku.getSalePrice();
        TradeVo tradeVo = new TradeVo();
        tradeVo.setTotalAmount(totalAmount);
        tradeVo.setOrderItemList(orderItemList);

        // 返回
        return tradeVo;
    }

    @Override
    public PageInfo<OrderInfo> findUserPage(Integer page, Integer limit, Integer orderStatus) {
        PageHelper.startPage(page, limit);
        Long userId = AuthContextUtil.getUserInfo().getId();
        List<OrderInfo> orderInfoList = orderInfoMapper.findUserPage(userId, orderStatus);

        orderInfoList.forEach(orderInfo -> {
            List<OrderItem> orderItem = orderItemMapper.findByOrderId(orderInfo.getId());
            orderInfo.setOrderItemList(orderItem);
        });

        return new PageInfo<>(orderInfoList);
    }

    // 业务接口实现类
    @Override
    public OrderInfo getByOrderNo(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        List<OrderItem> orderItem = orderItemMapper.findByOrderId(orderInfo.getId());
        orderInfo.setOrderItemList(orderItem);
        return orderInfo;
    }

    @Transactional
    @Override
    public void updateOrderStatus(String orderNo, Integer payType) {
        int affectedRows = orderInfoMapper.markPaid(orderNo, payType, new Date());
        if (affectedRows != 1) {
            OrderInfo existing = orderInfoMapper.getByOrderNo(orderNo);
            if (existing == null) {
                throw new MallException(ResultCodeEnum.DATA_ERROR);
            }
            if (Integer.valueOf(1).equals(existing.getOrderStatus())) {
                return;
            }
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(1);
        orderLog.setNote("支付宝支付成功");
        orderLogMapper.save(orderLog);
    }

    @Transactional
    @Override
    public void cancelOrder(String orderNo) {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null || userInfo.getId() == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        if (orderInfo == null || !userInfo.getId().equals(orderInfo.getUserId())) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }
        if (!Integer.valueOf(0).equals(orderInfo.getOrderStatus())) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }
        int affectedRows = orderInfoMapper.cancelPendingOrder(orderNo, userInfo.getId(), "用户取消订单", new Date());
        if (affectedRows != 1) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }
        List<SkuSaleDto> stockItems = orderItemMapper.findByOrderId(orderInfo.getId()).stream().map(item -> {
            SkuSaleDto stockItem = new SkuSaleDto();
            stockItem.setSkuId(item.getSkuId());
            stockItem.setNum(item.getSkuNum());
            return stockItem;
        }).collect(Collectors.toList());
        if (!stockItems.isEmpty() && !Boolean.TRUE.equals(productFeignClient.restoreStock(stockItems))) {
            throw new MallException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }

}
