package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.user.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper {
    UserInfo getByUsername(String username);

    void save(UserInfo userInfo);
}
