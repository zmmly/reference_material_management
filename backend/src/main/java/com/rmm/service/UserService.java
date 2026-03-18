package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.User;
import com.rmm.entity.Role;
import com.rmm.mapper.UserMapper;
import com.rmm.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<User> list(Integer current, Integer size, String keyword, Long roleId, Integer status) {
        Page<User> page = new Page<>(current, size);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .eq(roleId != null, User::getRoleId, roleId)
               .eq(status != null, User::getStatus, status)
               .and(StringUtils.hasText(keyword), w -> w
                   .like(User::getUsername, keyword)
                   .or()
                   .like(User::getRealName, keyword)
               )
               .orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::fillRole);

        PageResult<User> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            fillRole(user);
        }
        return user;
    }

    public void create(User user) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
    }

    public void update(User user) {
        User existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(existing.getPassword());
        }
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setDeleted(1);
            userMapper.updateById(user);
        }
    }

    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private void fillRole(User user) {
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                user.setRole(role);
                user.setRoleName(role.getName());
            }
        }
    }
}
