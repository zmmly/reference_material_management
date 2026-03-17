package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.common.PageResult;
import com.rmm.entity.Role;
import com.rmm.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<Role> listAll() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getId)
        );
    }

    public Role getById(Long id) {
        return roleMapper.selectById(id);
    }

    public void create(Role role) {
        role.setStatus(1);
        roleMapper.insert(role);
    }

    public void update(Role role) {
        roleMapper.updateById(role);
    }

    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role != null) {
            role.setStatus(0);
            roleMapper.updateById(role);
        }
    }
}
