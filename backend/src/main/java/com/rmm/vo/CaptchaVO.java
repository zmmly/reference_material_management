package com.rmm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证码响应")
public class CaptchaVO {

    @Schema(description = "验证码ID")
    private String captchaId;

    @Schema(description = "验证码图片(Base64)")
    private String captchaImage;

    @Schema(description = "验证码答案(仅开发环境返回)")
    private String captchaAnswer;
}
