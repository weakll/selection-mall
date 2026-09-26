package io.github.weakll.mall.user.service;

import io.github.weakll.mall.model.entity.user.UserCollect;
import io.github.weakll.mall.model.vo.h5.UserCollectVo;

import java.util.List;

public interface UserCollectService {
    void collect(Long skuId);
    void cancelCollect(Long skuId);
    Boolean isCollect(Long skuId);
    List<UserCollect> findCollectList();
    List<UserCollectVo> findCollectListVoList();
}
