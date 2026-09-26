package io.github.weakll.mall.user.service.impl;

import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.feign.product.ProductFeignClient;
import io.github.weakll.mall.model.entity.product.ProductSku;
import io.github.weakll.mall.model.entity.user.UserBrowseHistory;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.h5.UserBrowseHistoryVo;
import io.github.weakll.mall.user.mapper.UserBrowseHistoryMapper;
import io.github.weakll.mall.user.service.UserBrowseHistoryService;
import io.github.weakll.mall.utils.AuthContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserBrowseHistoryServiceImpl implements UserBrowseHistoryService {

    @Autowired
    private UserBrowseHistoryMapper userBrowseHistoryMapper;
    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public void addBrowseHistory(Long skuId) {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            return;
        }
        Long userId = userInfo.getId();
        UserBrowseHistory exist = userBrowseHistoryMapper.findByUserIdAndSkuId(userId, skuId);
        if (exist != null) {
            userBrowseHistoryMapper.updateTimeById(exist.getId());
            return;
        }
        UserBrowseHistory history = new UserBrowseHistory();
        history.setUserId(userId);
        history.setSkuId(skuId);
        userBrowseHistoryMapper.save(history);
    }

    @Override
    public List<UserBrowseHistoryVo> findBrowseHistoryList() {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        List<UserBrowseHistory> list = userBrowseHistoryMapper.findByUserId(userInfo.getId());
        // 去重：同一个 skuId 只保留最新的一条
        Map<Long, UserBrowseHistory> distinctMap = new LinkedHashMap<>();
        for (UserBrowseHistory item : list) {
            if (!distinctMap.containsKey(item.getSkuId())) {
                distinctMap.put(item.getSkuId(), item);
            }
        }
        return distinctMap.values().stream().map(item -> {
            UserBrowseHistoryVo vo = new UserBrowseHistoryVo();
            BeanUtils.copyProperties(item, vo);
            try {
                ProductSku productSku = productFeignClient.getBySkuId(item.getSkuId());
                vo.setProductSku(productSku);
            } catch (Exception e) {
                vo.setProductSku(null);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void clearBrowseHistory() {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new MallException(ResultCodeEnum.LOGIN_AUTH);
        }
        userBrowseHistoryMapper.clearByUserId(userInfo.getId());
    }
}
