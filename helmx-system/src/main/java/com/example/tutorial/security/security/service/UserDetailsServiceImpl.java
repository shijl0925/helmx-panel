package com.helmx.tutorial.security.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.helmx.tutorial.system.entity.Role;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.mapper.RoleMapper;
import com.helmx.tutorial.system.mapper.UserMapper;
import org.slf4j.Logger;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 用户详情服务实现类
 * 用于Spring Security认证过程中加载用户详细信息
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserDetailsServiceImpl(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 根据用户名加载用户详情
     * 该方法是Spring Security认证的核心方法，用于根据用户名查询用户信息及其角色权限
     * @param username 用户名
     * @return UserDetails 用户详情对象，包含用户名、密码和权限信息
     * @throws UsernameNotFoundException 当用户不存在时抛出此异常
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));

        if (user == null) {
            logger.warn("User not found with username: {}", username);
            throw new UsernameNotFoundException("User Not Found with username: " + username);
        }
        // 单独查询用户角色
        Set<Role> roles = roleMapper.findRolesByUserId(user.getId());
        user.setRoles(roles);

        logger.debug("Loaded user: {} with {} roles", username, roles.size());

        return UserDetailsImpl.build(user);
    }
}
