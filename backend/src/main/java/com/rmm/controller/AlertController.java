package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.entity.AlertConfig;
import com.rmm.entity.AlertRecord;
import com.rmm.service.AlertService;
import com.rmm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<List<AlertRecord>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String type) {
        return Result.success(alertService.getAlerts(status, type));
    }

    @GetMapping("/stats")
    public Result<AlertService.AlertStats> stats() {
        return Result.success(alertService.getStats());
    }

    @GetMapping("/configs")
    public Result<List<AlertConfig>> getConfigs() {
        return Result.success(alertService.getAllConfigs());
    }

    @PutMapping("/configs/{type}")
    public Result<Void> updateConfig(
            @PathVariable String type,
            @RequestParam Integer threshold,
            @RequestParam Integer enabled) {
        alertService.updateConfig(type, threshold, enabled);
        return Result.success();
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handle(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        alertService.handleAlert(id, userId, remark);
        return Result.success();
    }

    @PutMapping("/{id}/ignore")
    public Result<Void> ignore(@PathVariable Long id) {
        alertService.ignoreAlert(id);
        return Result.success();
    }

    @PostMapping("/check")
    public Result<Void> triggerCheck() {
        alertService.checkAlerts();
        return Result.success();
    }
}
