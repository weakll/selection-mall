package io.github.weakll.mall.order.service.impl;

import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.feign.cart.CartFeignClient;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.feign.user.UserFeignClient;
import io.github.weakll.mall.model.dto.h5.OrderInfoDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderInfoServiceImpl implements OrderInfoService {

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

        for (OrderItem orderItem : orderItemList) {
            ProductSku productSku = productFeignClient.getBySkuId(orderItem.getSkuId());
            if(null == productSku) {
                throw new MallException(ResultCodeEnum.DATA_ERROR);
            }
            //校验库存
            if(orderItem.getSkuNum().intValue() > productSku.getStockNum().intValue()) {
                throw new MallException(ResultCodeEnum.STOCK_LESS);
            }
        }

        // 构建订单数据，保存订单
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        OrderInfo orderInfo = new OrderInfo();
        //订单编号
        orderInfo.setOrderNo(String.valueOf(System.currentTimeMillis()));
        //用户id
        orderInfo.setUserId(userInfo.getId());
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

        // 清空购物车：由于 service-cart 接口返回 404，改为直接操作 Redis
        try {
            System.out.println("========== 开始清空购物车 ==========");
            Long userId = AuthContextUtil.getUserInfo().getId();
            System.out.println("当前用户ID: " + userId);

            String cartKey = "user:cart:" + userId;
            System.out.println("购物车Key: " + cartKey);

            List<Object> objectList = redisTemplate.opsForHash().values(cartKey);
            System.out.println("购物车商品总数: " + (objectList == null ? 0 : objectList.size()));

            if (!CollectionUtils.isEmpty(objectList)) {
                long deletedCount = objectList.stream()
                    .map(cartInfoJSON -> JSON.parseObject(cartInfoJSON.toString(), CartInfo.class))
                    .filter(cartInfo -> {
                        System.out.println("检查商品: " + cartInfo.getSkuName() + ", isChecked=" + cartInfo.getIsChecked());
                        return cartInfo.getIsChecked() == 1;
                    })
                    .peek(cartInfo -> System.out.println("删除选中商品: " + cartInfo.getSkuName()))
                    .mapToLong(cartInfo -> {
                        redisTemplate.opsForHash().delete(cartKey, String.valueOf(cartInfo.getSkuId()));
                        return 1;
                    })
                    .sum();
                System.out.println("成功删除 " + deletedCount + " 个选中的商品");
            } else {
                System.out.println("购物车为空，无需删除");
            }
            System.out.println("========== 购物车清空完成 ==========");
        } catch (Exception e) {
            System.err.println("清空购物车失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 6、返回订单id
        return orderInfo.getId();
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
    public void updateOrderStatus(String orderNo, Integer orderStatus) {
        // 更新订单状态
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        orderInfo.setOrderStatus(1);
        orderInfo.setPayType(orderStatus);
        orderInfo.setPaymentTime(new Date());
        orderInfoMapper.updateById(orderInfo);

        // 记录日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(1);
        orderLog.setNote("支付宝支付成功");
        orderLogMapper.save(orderLog);
    }

}
