package com.helmx.tutorial.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.system.entity.Role;
import com.helmx.tutorial.system.mapper.RoleMapper;
import com.helmx.tutorial.system.service.RoleService;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public boolean existsById(Long id) {
        return id != null && this.count(new QueryWrapper<Role>().eq("id", id)) > 0;
    }
}
