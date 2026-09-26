package io.github.weakll.mall.user.service;

import io.github.weakll.mall.model.vo.h5.UserBrowseHistoryVo;

import java.util.List;

public interface UserBrowseHistoryService {

    void addBrowseHistory(Long skuId);

    List<UserBrowseHistoryVo> findBrowseHistoryList();

    void clearBrowseHistory();
}
