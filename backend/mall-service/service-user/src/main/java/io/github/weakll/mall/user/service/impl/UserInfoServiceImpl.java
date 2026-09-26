package io.github.weakll.mall.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.StringUtils;
import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.model.dto.h5.UserLoginDto;
import io.github.weakll.mall.model.dto.h5.UserRegisterDto;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.h5.UserInfoVo;
import io.github.weakll.mall.user.mapper.UserInfoMapper;
import io.github.weakll.mall.user.service.UserInfoService;
import io.github.weakll.mall.utils.AuthContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private RedisTemplate<String , String> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterDto userRegisterDto) {

        // 获取数据
        String username = userRegisterDto.getUsername();
        String password = userRegisterDto.getPassword();
        String nickName = userRegisterDto.getNickName();
        String code = userRegisterDto.getCode();

        //校验参数
        if(StringUtils.isEmpty(username) ||
                StringUtils.isEmpty(password) ||
                StringUtils.isEmpty(nickName) ||
                StringUtils.isEmpty(code)) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        //校验校验验证码
        String codeValueRedis = redisTemplate.opsForValue().get(username);
        if(!code.equals(codeValueRedis)) {
            throw new MallException(ResultCodeEnum.VALIDATECODE_ERROR);
        }

        UserInfo userInfo = userInfoMapper.getByUsername(username);
        if(userInfo != null ) {
            throw new MallException(ResultCodeEnum.USER_NAME_IS_EXISTS);
        }

        //保存用户信息
        userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setNickName(nickName);
        userInfo.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        userInfo.setPhone(username);
        userInfo.setStatus(1);
        userInfo.setSex(0);
        userInfo.setAvatar("http://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83eoj0hHXhgJNOTSOFsS4uZs8x1ConecaVOB8eIl115xmJZcT4oCicvia7wMEufibKtTLqiaJeanU2Lpg3w/132");
        userInfoMapper.save(userInfo);

        // 删除Redis中的数据
        redisTemplate.delete(username);

    }

    @Override
    public String login(UserLoginDto userLoginDto) {
        String username = userLoginDto.getUsername();
        String password = userLoginDto.getPassword();

        //校验参数
        if(StringUtils.isEmpty(username) ||
                StringUtils.isEmpty(password)) {
            throw new MallException(ResultCodeEnum.DATA_ERROR);
        }

        UserInfo userInfo = userInfoMapper.getByUsername(username);
        if(null == userInfo) {
            throw new MallException(ResultCodeEnum.LOGIN_ERROR);
        }

        //校验密码
        String md5InputPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if(!md5InputPassword.equals(userInfo.getPassword())) {
            throw new MallException(ResultCodeEnum.LOGIN_ERROR);
        }

        //校验是否被禁用
        if(userInfo.getStatus() == 0) {
            throw new MallException(ResultCodeEnum.ACCOUNT_STOP);
        }

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set("mall:user:" + token, JSON.toJSONString(userInfo), 30, TimeUnit.DAYS);
        return token;
    }

    @Override
    public UserInfoVo getCurrentUserInfo(String token) {
//        String userInfoJSON = redisTemplate.opsForValue().get("mall:user:" + token);
//        if(StringUtils.isEmpty(userInfoJSON)) {
//            throw new MallException(ResultCodeEnum.LOGIN_AUTH) ;
//        }
//        UserInfo userInfo = JSON.parseObject(userInfoJSON , UserInfo.class) ;
//        UserInfoVo userInfoVo = new UserInfoVo();
        // io.github.weakll.mall.user.service.impl.UserInfoServiceImpl
            UserInfo userInfo = AuthContextUtil.getUserInfo();
            UserInfoVo userInfoVo = new UserInfoVo();
            BeanUtils.copyProperties(userInfo, userInfoVo);
            return userInfoVo ;
        }
}
