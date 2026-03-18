package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.common.BusinessException;
import com.rmm.dto.LoginDTO;
import com.rmm.entity.Role;
import com.rmm.entity.User;
import com.rmm.mapper.RoleMapper;
import com.rmm.mapper.UserMapper;
import com.rmm.util.JwtUtil;
import com.rmm.vo.CaptchaVO;
import com.rmm.vo.LoginVO;
import com.rmm.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;
    private static final long ERROR_DELAY_MS = 500;

    /**
     * 生成验证码
     */
    public CaptchaVO generateCaptcha() {
        HttpSession session = request.getSession(true);
        String captchaId = java.util.UUID.randomUUID().toString();

        // 生成4位数字+字母验证码
        com.wf.captcha.SpecCaptcha captcha = new com.wf.captcha.SpecCaptcha(130, 48, 4);
        captcha.setCharType(com.wf.captcha.SpecCaptcha.TYPE_NUM_AND_UPPER);

        String answer = captcha.text().toLowerCase();
        // toBase64() 方法已包含 data:image/png;base64, 前缀
        String base64Image = captcha.toBase64();

        // 存入Session，5分钟过期
        session.setAttribute("captcha:" + captchaId, answer);
        session.setMaxInactiveInterval(300);

        log.info("Generated captcha: id={}, answer={}, profile={}", captchaId, answer, activeProfile);

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setCaptchaImage(base64Image);
        // 仅在开发环境返回答案，用于E2E测试 (非prod环境都返回)
        if (activeProfile == null || !"prod".equals(activeProfile)) {
            vo.setCaptchaAnswer(answer);
        }
        return vo;
    }

    /**
     * 验证验证码
     * @throws BusinessException 验证失败时抛出异常
     */
    public void validateCaptcha(String captchaId, String captchaCode) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }

        String sessionKey = "captcha:" + captchaId;
        String storedAnswer = (String) session.getAttribute(sessionKey);

        if (storedAnswer == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }

        // 验证后立即删除，防止重放攻击
        session.removeAttribute(sessionKey);

        if (!storedAnswer.equals(captchaCode.toLowerCase())) {
            throw new BusinessException("验证码错误");
        }
    }

    /**
     * 检查账户是否被锁定
     * @return 剩余锁定分钟数，0表示未锁定
     */
    public long checkAccountLocked(String ip, String username) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return 0;
        }

        String lockKey = "login_failed:" + ip + ":" + username;
        LoginFailureInfo info = (LoginFailureInfo) session.getAttribute(lockKey);

        if (info == null || info.count() < MAX_LOGIN_ATTEMPTS) {
            return 0;
        }

        long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - info.firstFailTime());
        long remainingMinutes = LOCK_DURATION_MINUTES - elapsedMinutes;

        return remainingMinutes > 0 ? remainingMinutes : 0;
    }

    /**
     * 记录登录失败
     * @return 剩余尝试次数
     */
    public int recordLoginFailure(String ip, String username) {
        HttpSession session = request.getSession(true);
        String lockKey = "login_failed:" + ip + ":" + username;
        LoginFailureInfo info = (LoginFailureInfo) session.getAttribute(lockKey);

        if (info == null) {
            info = new LoginFailureInfo(1, System.currentTimeMillis());
        } else {
            // 检查是否超过锁定时间，如果是则重置
            long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - info.firstFailTime());
            if (elapsedMinutes >= LOCK_DURATION_MINUTES) {
                info = new LoginFailureInfo(1, System.currentTimeMillis());
            } else {
                info = new LoginFailureInfo(info.count() + 1, info.firstFailTime());
            }
        }

        session.setAttribute(lockKey, info);
        session.setMaxInactiveInterval((int) TimeUnit.MINUTES.toSeconds(LOCK_DURATION_MINUTES + 5));

        return Math.max(0, MAX_LOGIN_ATTEMPTS - info.count());
    }

    /**
     * 清除登录失败记录
     */
    public void clearLoginFailure(String ip, String username) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String lockKey = "login_failed:" + ip + ":" + username;
            session.removeAttribute(lockKey);
        }
    }

    /**
     * 添加延迟，防止时序攻击
     */
    private void delay() {
        try {
            Thread.sleep(ERROR_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public LoginVO login(LoginDTO dto) {
        String ip = getClientIp();
        String username = dto.getUsername();

        // 1. 验证码校验（不计入失败次数）
        try {
            validateCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        } catch (BusinessException e) {
            delay();
            throw e;
        }

        // 2. 检查账户锁定状态
        long lockedMinutes = checkAccountLocked(ip, username);
        if (lockedMinutes > 0) {
            delay();
            throw new BusinessException("账户已锁定，请 " + lockedMinutes + " 分钟后重试");
        }

        // 3. 验证用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            int remaining = recordLoginFailure(ip, username);
            delay();
            throw new BusinessException("用户名或密码错误，还剩 " + remaining + " 次尝试机会");
        }

        if (user.getStatus() != 1) {
            delay();
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            int remaining = recordLoginFailure(ip, username);
            delay();
            throw new BusinessException("用户名或密码错误，还剩 " + remaining + " 次尝试机会");
        }

        // 4. 登录成功，清除失败记录
        clearLoginFailure(ip, username);

        Role role = roleMapper.selectById(user.getRoleId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(token);

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setPhone(user.getPhone());
        userVO.setEmail(user.getEmail());
        userVO.setRoleId(user.getRoleId());
        if (role != null) {
            userVO.setRoleName(role.getName());
            userVO.setRoleCode(role.getCode());
        }
        vo.setUser(userVO);

        // 检查是否需要修改密码
        vo.setNeedChangePassword(user.getPasswordChanged() == null || !user.getPasswordChanged());

        log.info("User login success: username={}, ip={}", username, ip);
        return vo;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个IP（多层代理时）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Role role = roleMapper.selectById(user.getRoleId());

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setPhone(user.getPhone());
        userVO.setEmail(user.getEmail());
        userVO.setRoleId(user.getRoleId());
        if (role != null) {
            userVO.setRoleName(role.getName());
            userVO.setRoleCode(role.getCode());
        }

        return userVO;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChanged(true);
        userMapper.updateById(user);
    }

    /**
     * 登录失败信息记录
     */
    private record LoginFailureInfo(int count, long firstFailTime) {
    }
}
