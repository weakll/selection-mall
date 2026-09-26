package io.github.weakll.mall.common.Interceptor;

import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.model.entity.user.UserInfo;
import io.github.weakll.mall.utils.AuthContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class UserLoginAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String , String> redisTemplate ;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 如果token不为空，那么此时验证token的合法性
        String userInfoJSON = redisTemplate.opsForValue().get("mall:user:" + request.getHeader("token"));
        AuthContextUtil.setUserInfo(JSON.parseObject(userInfoJSON , UserInfo.class));
        return true ;

    }

}
