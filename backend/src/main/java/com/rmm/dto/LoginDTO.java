package com.rmm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求参数")
public class LoginDTO {

    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "admin123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    @Schema(description = "验证码", example = "A1B2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
