package io.github.weakll.mall.user.controller;

import io.github.weakll.mall.model.entity.user.UserAddress;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// io.github.weakll.mall.user.controller;
@Tag(name = "用户地址接口")
@RestController
@RequestMapping(value="/api/user/userAddress")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @Operation(summary = "获取用户地址列表")
    @GetMapping("auth/findUserAddressList")
    public Result<List<UserAddress>> findUserAddressList() {
        List<UserAddress> list = userAddressService.findUserAddressList();
        return Result.build(list , ResultCodeEnum.SUCCESS) ;
    }

    @Operation(summary = "获取地址信息")
    @GetMapping("getUserAddress/{id}")
    public UserAddress getUserAddress(@PathVariable Long id) {
        return userAddressService.getById(id);
    }

    @Operation(summary = "新增地址")
    @PostMapping("auth/save")
    public Result<Void> save(@RequestBody UserAddress userAddress) {
        userAddressService.save(userAddress);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "修改地址")
    @PutMapping("auth/updateById")
    public Result<Void> updateById(@RequestBody UserAddress userAddress) {
        userAddressService.updateById(userAddress);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("auth/removeById/{id}")
    public Result<Void> removeById(@PathVariable Long id) {
        userAddressService.removeById(id);
        return Result.build(null, ResultCodeEnum.SUCCESS);
    }
}
