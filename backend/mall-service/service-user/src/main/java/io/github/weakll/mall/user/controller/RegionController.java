package io.github.weakll.mall.user.controller;

import io.github.weakll.mall.model.entity.base.Region;
import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.user.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "地区接口")
@RestController
@RequestMapping(value="/api/user/region")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RegionController {

    @Autowired
    private RegionService regionService;

    @Operation(summary = "根据父级编码获取地区列表")
    @GetMapping("/findByParentCode/{parentCode}")
    public Result<List<Region>> findByParentCode(@PathVariable Long parentCode) {
        List<Region> list = regionService.findByParentCode(parentCode);
        return Result.build(list, ResultCodeEnum.SUCCESS);
    }
}
