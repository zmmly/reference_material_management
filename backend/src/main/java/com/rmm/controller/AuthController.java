package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.dto.ChangePasswordDTO;
import com.rmm.dto.LoginDTO;
import com.rmm.service.AuthService;
import com.rmm.vo.CaptchaVO;
import com.rmm.vo.LoginVO;
import com.rmm.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "获取验证码", description = "获取登录验证码图片")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        return Result.success(authService.generateCaptcha());
    }

    @Operation(summary = "用户登录", description = "通过用户名和密码登录系统，返回JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/user-info")
    public Result<UserVO> getUserInfo() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(authService.getCurrentUser(userId));
    }

    @Operation(summary = "修改密码", description = "修改当前用户密码，首次登录强制修改")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}
