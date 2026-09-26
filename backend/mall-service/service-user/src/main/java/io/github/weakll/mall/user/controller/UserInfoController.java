package io.github.weakll.mall.user.controller;

import io.github.weakll.mall.model.dto.h5.UserLoginDto;
import io.github.weakll.mall.model.dto.h5.UserRegisterDto;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.model.vo.h5.UserBrowseHistoryVo;
import io.github.weakll.mall.model.vo.h5.UserCollectVo;
import io.github.weakll.mall.model.vo.h5.UserInfoVo;
import io.github.weakll.mall.user.service.UserBrowseHistoryService;
import io.github.weakll.mall.user.service.UserCollectService;
import io.github.weakll.mall.user.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会员用户接口")
@RestController
@RequestMapping("api/user/userInfo/")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserCollectService userCollectService;
    @Autowired
    private UserBrowseHistoryService userBrowseHistoryService;

    @Operation(summary = "会员注册")
    @PostMapping("register")
    public Result register(@RequestBody UserRegisterDto userRegisterDto) {
        userInfoService.register(userRegisterDto);
        return Result.build(null , ResultCodeEnum.SUCCESS) ;
    }
    @Operation(summary = "会员登录")
    @PostMapping("login")
    public Result login(@RequestBody UserLoginDto userLoginDto) {
        return Result.build(userInfoService.login(userLoginDto), ResultCodeEnum.SUCCESS);
    }
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("auth/getCurrentUserInfo")
    public Result<UserInfoVo> getCurrentUserInfo(HttpServletRequest request) {
        String token = request.getHeader("token");
        UserInfoVo userInfoVo = userInfoService.getCurrentUserInfo(token) ;
        return Result.build(userInfoVo , ResultCodeEnum.SUCCESS) ;
    }
    @Operation(summary = "添加收藏")
    @GetMapping("isCollect/{skuId}")
    public Result isCollect(@PathVariable Long skuId) {
        userCollectService.collect(skuId);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "取消收藏")
    @GetMapping("auth/cancelCollect/{skuId}")
    public Result cancelCollect(@PathVariable Long skuId) {
        userCollectService.cancelCollect(skuId);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "查询商品是否已收藏")
    @GetMapping("auth/isCollected/{skuId}")
    public Result<Boolean> isCollected(@PathVariable Long skuId) {
        return Result.build(userCollectService.isCollect(skuId), ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "查询用户收藏列表")
    @GetMapping("auth/findUserCollectList")
    public Result<List<UserCollectVo>> findUserCollectList() {
        return Result.build(userCollectService.findCollectListVoList(), ResultCodeEnum.SUCCESS);
    }
    @Operation(summary = "添加浏览记录")
    @GetMapping("auth/addBrowseHistory/{skuId}")
    public Result addBrowseHistory(@PathVariable Long skuId) {
        userBrowseHistoryService.addBrowseHistory(skuId);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "查询用户浏览记录列表")
    @GetMapping("auth/findBrowseHistoryList")
    public Result<List<UserBrowseHistoryVo>> findBrowseHistoryList() {
        return Result.build(userBrowseHistoryService.findBrowseHistoryList(), ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "清空浏览记录")
    @GetMapping("auth/clearBrowseHistory")
    public Result clearBrowseHistory() {
        userBrowseHistoryService.clearBrowseHistory();
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

}
