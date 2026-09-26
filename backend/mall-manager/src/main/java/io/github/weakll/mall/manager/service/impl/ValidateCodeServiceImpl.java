package io.github.weakll.mall.manager.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import io.github.weakll.mall.manager.service.ValidateCodeService;
import io.github.weakll.mall.model.vo.system.ValidateCodeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ValidateCodeServiceImpl implements ValidateCodeService {
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Override
    public ValidateCodeVo generateValidateCode() {
        //使用hutoll生成图片验证码
        CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(150, 48, 4 ,20);
        String codeValue = circleCaptcha.getCode();
        String imageBase64 = circleCaptcha.getImageBase64();

        //生成一个随机的codeKey
        String codeKey = UUID.randomUUID().toString().replace("-","");
        //保存验证码到redis中
        redisTemplate.opsForValue().set("user:login:validatecode"+codeKey,codeValue,5, TimeUnit.MINUTES);

        //返回给前端,构建响应结果
        ValidateCodeVo validateCodeVo = new ValidateCodeVo();
        validateCodeVo.setCodeKey(codeKey);
        validateCodeVo.setCodeValue("data:image/png;base64," + imageBase64);

        //返回给前端
        return validateCodeVo;
    }
}
