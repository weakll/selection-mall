package io.github.weakll.mall.manager.controller;

import io.github.weakll.mall.manager.service.SysMenuService;
import io.github.weakll.mall.manager.service.SysUserService;
import io.github.weakll.mall.manager.service.ValidateCodeService;
import io.github.weakll.mall.model.dto.system.LoginDto;
import io.github.weakll.mall.model.entity.system.SysUser;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.system.LoginVo;
import io.github.weakll.mall.model.vo.system.SysMenuVo;
import io.github.weakll.mall.model.vo.system.ValidateCodeVo;
import io.github.weakll.mall.utils.AuthContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "登录接口")
@RestController
@RequestMapping(value = "/admin/system/index")
public class IndexController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ValidateCodeService validateCodeService;
    @Autowired
    private SysMenuService sysMenuService;
    @Operation(summary = "登录接口")
    @PostMapping(value = "/login")
    public Result<LoginVo> login(@RequestBody LoginDto loginDto) {
        LoginVo loginVo = sysUserService.login(loginDto);
        return Result.build(loginVo, ResultCodeEnum.SUCCESS);
    }

    /**
     * 图片验证
     */

    @GetMapping(value = "/generateValidateCode")
    public Result<ValidateCodeVo> generateValidateCode() {
        ValidateCodeVo validateCodeVo = validateCodeService.generateValidateCode();
        return Result.build(validateCodeVo, ResultCodeEnum.SUCCESS);
    }

    //获取用户登录信息
    @GetMapping(value = "/getUserInfo")
    public Result<SysUser> getUserInfo() {

        return Result.build(AuthContextUtil.get() , ResultCodeEnum.SUCCESS);
    }

    //退出登录
    @GetMapping(value = "/logout")
    public Result logout(@RequestHeader(value = "token") String token){
        sysUserService.logout(token);
        return Result.build(null,ResultCodeEnum.SUCCESS);

    }
    @GetMapping("/menus")
    public Result menus() {
        List<SysMenuVo> sysMenuVoList = sysMenuService.findUserMenuList() ;
        return Result.build(sysMenuVoList , ResultCodeEnum.SUCCESS) ;
    }
}
