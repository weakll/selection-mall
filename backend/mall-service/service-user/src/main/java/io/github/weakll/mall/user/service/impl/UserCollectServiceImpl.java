package io.github.weakll.mall.user.service.impl;

import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.entity.user.UserCollect;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.h5.UserCollectVo;
import io.github.weakll.mall.user.mapper.UserCollectMapper;
import io.github.weakll.mall.user.service.UserCollectService;
import io.github.weakll.mall.utils.AuthContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserCollectServiceImpl implements UserCollectService {
    @Autowired
    private UserCollectMapper userCollectMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Override
    public void collect(Long skuId) {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        Long userId = userInfo.getId();
        UserCollect exist = userCollectMapper.findByUserIdAndSkuId(userId, skuId);
        if (exist != null) {
            return;
        }
        UserCollect userCollect = new UserCollect();
        userCollect.setUserId(userId);
        userCollect.setSkuId(skuId);
        userCollectMapper.save(userCollect);
    }

    @Override
    public void cancelCollect(Long skuId) {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        userCollectMapper.deleteByUserIdAndSkuId(userInfo.getId(), skuId);
    }

    @Override
    public Boolean isCollect(Long skuId) {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
           return false;
        }
        return userCollectMapper.findByUserIdAndSkuId(userInfo.getId(), skuId) != null;
    }

    @Override
    public List<UserCollect> findCollectList() {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        return userCollectMapper.findByUserId(userInfo.getId());
    }

    @Override
    public List<UserCollectVo> findCollectListVoList() {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        List<UserCollect> list = userCollectMapper.findByUserId(userInfo.getId());
        return list.stream().map(item -> {
           UserCollectVo vo = new UserCollectVo();
            BeanUtils.copyProperties(item, vo);
            try{
                ProductSku productSku = productFeignClient.getBySkuId(item.getSkuId());
                vo.setProductSku(productSku);
            }catch (Exception e) {
                //如果商品下架异常或商品已下架，不影响列表展示
                vo.setProductSku(null);
            }
            return vo;
       }).collect(Collectors.toList());

    }
}
