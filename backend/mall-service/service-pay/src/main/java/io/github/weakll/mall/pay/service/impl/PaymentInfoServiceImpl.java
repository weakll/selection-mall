package io.github.weakll.mall.pay.service.impl;

import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.feign.order.OrderFeignClient;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.dto.product.SkuSaleDto;
import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.entity.order.OrderItem;
import io.github.weakll.mall.model.entity.pay.PaymentInfo;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.pay.mapper.PaymentInfoMapper;
import io.github.weakll.mall.pay.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper ;

    @Autowired
    private OrderFeignClient orderFeignClient ;

    @Autowired
    private ProductFeignClient productFeignClient ;

    @Override
    public PaymentInfo savePaymentInfo(String orderNo) {

        // 查询支付信息数据，如果已经已经存在了就不用进行保存(一个订单支付失败以后可以继续支付)
        PaymentInfo paymentInfo = paymentInfoMapper.getByOrderNo(orderNo);
        if(null == paymentInfo) {
            OrderInfo orderInfo = orderFeignClient.getOrderInfoByOrderNo(orderNo).getData();
            paymentInfo = new PaymentInfo();
            paymentInfo.setUserId(orderInfo.getUserId());
            paymentInfo.setPayType(orderInfo.getPayType());
            String content = "";
            for(OrderItem item : orderInfo.getOrderItemList()) {
                content += item.getSkuName() + " ";
            }
            paymentInfo.setContent(content);
            paymentInfo.setAmount(orderInfo.getTotalAmount());
            paymentInfo.setOrderNo(orderNo);
            paymentInfo.setPaymentStatus(0);
            paymentInfoMapper.save(paymentInfo);
        }
        return paymentInfo;
    }
    @Transactional
    @Override
    public void updatePaymentStatus(Map<String, String> map, Integer payType) {
        String orderNo = map.get("out_trade_no");
        if (!StringUtils.hasText(orderNo)) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        PaymentInfo paymentInfo = paymentInfoMapper.getByOrderNo(orderNo);
        if (paymentInfo == null) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }
        if (Integer.valueOf(1).equals(paymentInfo.getPaymentStatus())) {
            return;
        }

        int affectedRows = paymentInfoMapper.markPaid(
                orderNo,
                map.get("trade_no"),
                new Date(),
                JSON.toJSONString(map)
        );
        if (affectedRows != 1) {
            PaymentInfo latest = paymentInfoMapper.getByOrderNo(orderNo);
            if (latest != null && Integer.valueOf(1).equals(latest.getPaymentStatus())) {
                return;
            }
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        orderFeignClient.updateOrderStatus(orderNo, payType);

        OrderInfo orderInfo = orderFeignClient.getOrderInfoByOrderNo(orderNo).getData();
        if (!CollectionUtils.isEmpty(orderInfo.getOrderItemList())) {
            List<SkuSaleDto> skuSaleDtoList = orderInfo.getOrderItemList().stream().map(item -> {
                SkuSaleDto skuSaleDto = new SkuSaleDto();
                skuSaleDto.setSkuId(item.getSkuId());
                skuSaleDto.setNum(item.getSkuNum());
                return skuSaleDto;
            }).collect(Collectors.toList());
            productFeignClient.updateSkuSaleNum(skuSaleDtoList);
        }
    }
}
