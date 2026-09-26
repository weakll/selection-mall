package io.github.weakll.mall.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import io.github.weakll.mall.common.exception.MallException;
import io.github.weakll.mall.manager.mapper.SysRoleUserMapper;
import io.github.weakll.mall.manager.mapper.SysUserMapper;
import io.github.weakll.mall.manager.service.SysUserService;
import io.github.weakll.mall.model.dto.system.AssginRoleDto;
import io.github.weakll.mall.model.dto.system.LoginDto;
import io.github.weakll.mall.model.dto.system.SysUserDto;
import io.github.weakll.mall.model.entity.system.SysUser;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.system.LoginVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserServiceimpl implements SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleUserMapper sysRoleUserMapper ;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Override
    public LoginVo login(LoginDto loginDto) {


        // 校验验证码是否正确
        String captcha = loginDto.getCaptcha();     // 用户输入的验证码
        String codeKey = loginDto.getCodeKey();     // redis中验证码的数据key

        // 从Redis中获取验证码
        String redisCode = redisTemplate.opsForValue().get("user:login:validatecode" + codeKey);//TODO:注意冒号，避免和Redis中的key冲突，不识别
        if(StrUtil.isEmpty(redisCode) || !StrUtil.equalsIgnoreCase(redisCode , captcha)) {
            throw new MallException(ResultCodeEnum.VALIDATECODE_ERROR) ;
        }

        // 验证通过删除redis中的验证码
        redisTemplate.delete("user:login:validatecode:" + codeKey) ;

        // 用户名查询
        SysUser sysUser = sysUserMapper.selectByUserName(loginDto.getUserName());
        if(sysUser == null){
            throw new RuntimeException("用户名或密码错误");

        }
        // 密码校验
        String inputPassword = loginDto.getPassword();
        String md5InputPassword = DigestUtils.md5DigestAsHex(inputPassword.getBytes());
        if(!md5InputPassword.equals(sysUser.getPassword())){
            throw new RuntimeException("用户名或密码错误");

        }
        // 生成token
        String token = UUID.randomUUID().toString().replaceAll("-","");
        redisTemplate.opsForValue().set("user:login:"+token, JSON.toJSONString(sysUser),30, TimeUnit.MINUTES);

        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        loginVo.setRefresh_token("");


        return loginVo;
    }

    @Override
    public SysUser getUserInfo(String token) {
        String userJSON = redisTemplate.opsForValue().get("user:login:" + token);
        return JSON.parseObject(userJSON, SysUser.class);
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete("user:login:" + token);
    }


    @Override
    public PageInfo<SysUser> findByPage(SysUserDto sysUserDto, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum , pageSize);
        List<SysUser> sysUserList = sysUserMapper.findByPage(sysUserDto) ;
        PageInfo pageInfo = new PageInfo(sysUserList) ;
        return pageInfo;
    }
    //添加
    @Override
    public void saveSysUser(SysUser sysUser) {
        SysUser dbSysUser = sysUserMapper.findByUserName(sysUser.getUserName()) ;
        if(dbSysUser != null) {
            throw new MallException(ResultCodeEnum.USER_NAME_IS_EXISTS) ;

        }
        // 对密码进⾏加密
        String password = sysUser.getPassword();
        String digestPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        sysUser.setPassword(digestPassword);
        sysUser.setStatus(0);
        sysUserMapper.saveSysUser(sysUser) ;
    }

    @Override
    public void updateSysUser(SysUser sysUser) {
        sysUserMapper.updateSysUser(sysUser) ;
    }

    @Override
    public void deleteById(Long userId) {
        sysUserMapper.deleteById(userId) ;
    }


    @Transactional
    @Override
    public void doAssign(AssginRoleDto assginRoleDto) {
        // 删除之前的所有的⽤⼾所对应的⻆⾊数据
        sysRoleUserMapper.deleteByUserId(assginRoleDto.getUserId()) ;
        // 分配新的⻆⾊数据
        List<Long> roleIdList = assginRoleDto.getRoleIdList();
        roleIdList.forEach(roleId->{
            sysRoleUserMapper.doAssign(assginRoleDto.getUserId(),roleId);
        });
    }
}
