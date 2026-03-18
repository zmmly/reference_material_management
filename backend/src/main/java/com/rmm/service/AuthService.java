package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.common.BusinessException;
import com.rmm.dto.LoginDTO;
import com.rmm.entity.Role;
import com.rmm.entity.User;
import com.rmm.mapper.RoleMapper;
import com.rmm.mapper.UserMapper;
import com.rmm.util.JwtUtil;
import com.rmm.vo.LoginVO;
import com.rmm.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

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

        return vo;
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
}
