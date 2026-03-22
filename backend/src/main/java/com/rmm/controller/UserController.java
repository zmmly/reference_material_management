package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.User;
import com.rmm.service.UserService;
import com.rmm.util.JwtUtil;
import com.rmm.util.OperationLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final OperationLogUtil operationLogUtil;

    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.list(current, size, keyword, roleId, status));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody User user, HttpServletRequest request) {
        userService.create(user);

        // 记录操作日志
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            operationLogUtil.log(request, userId, username, "user", "新增",
                "用户", "新增用户: " + user.getUsername());
        }

        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user, HttpServletRequest request) {
        user.setId(id);
        userService.update(user);

        // 记录操作日志
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            operationLogUtil.log(request, userId, username, "user", "编辑",
                "用户", "编辑用户: " + user.getUsername());
        }

        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        userService.delete(id);

        // 记录操作日志
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            operationLogUtil.log(request, userId, username, "user", "删除",
                "用户", "删除用户ID: " + id);
        }

        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword, HttpServletRequest request) {
        userService.resetPassword(id, newPassword);

        // 记录操作日志
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            operationLogUtil.log(request, userId, username, "user", "重置密码",
                "用户", "重置用户密码ID: " + id);
        }

        return Result.success();
    }

    @GetMapping("/all")
    public Result<List<User>> listAll() {
        return Result.success(userService.listAll());
    }
}
