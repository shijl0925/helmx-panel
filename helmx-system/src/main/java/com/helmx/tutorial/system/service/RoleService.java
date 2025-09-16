package com.helmx.tutorial.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import com.helmx.tutorial.system.entity.Role;

@Service
public interface RoleService extends IService<Role> {
    boolean existsById(Long id);
}
