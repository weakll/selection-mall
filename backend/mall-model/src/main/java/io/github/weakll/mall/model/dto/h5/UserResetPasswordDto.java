package io.github.weakll.mall.model.dto.h5;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "重置密码对象")
public class UserResetPasswordDto {

    @Schema(description = "手机号")
    private String username;

    @Schema(description = "新密码")
    private String password;

    @Schema(description = "手机验证码")
    private String code;
}
