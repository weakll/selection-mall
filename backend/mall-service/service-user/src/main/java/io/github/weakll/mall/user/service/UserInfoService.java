package io.github.weakll.mall.user.service;

import io.github.weakll.mall.model.dto.h5.UserLoginDto;
import io.github.weakll.mall.model.dto.h5.UserRegisterDto;
import io.github.weakll.mall.model.vo.h5.UserInfoVo;

public interface UserInfoService {
    void register(UserRegisterDto userRegisterDto);

    String login(UserLoginDto userLoginDto);

    UserInfoVo getCurrentUserInfo(String token);
}
