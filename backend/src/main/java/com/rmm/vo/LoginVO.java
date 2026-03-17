package com.rmm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应结果")
public class LoginVO {

    @Schema(description = "JWT访问令牌")
    private String token;

    @Schema(description = "用户信息")
    private UserVO user;
}
