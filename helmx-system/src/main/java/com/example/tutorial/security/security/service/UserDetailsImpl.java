
package com.helmx.tutorial.security.security.service;

import com.helmx.tutorial.system.entity.ERole;
import com.helmx.tutorial.system.entity.User;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * UserDetailsImpl 类实现了 Spring Security 的 UserDetails 接口，
 * 用于封装用户认证和授权所需的信息。
 * 该类通过 User 实体构建，提供用户的 ID、用户名、邮箱、密码以及权限信息。
 * 所有账户状态字段（如是否过期、是否锁定等）默认为 true，表示账户正常可用。
 */
@Getter
public class UserDetailsImpl implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;

    private final String username;

    private final String email;

    private final String password;

    private final boolean enabled; // 新增字段，存储账户启用状态

    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String email, String password, boolean enabled,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsImpl.class);

    /**
     * 静态工厂方法，根据 User 实体对象构建 UserDetailsImpl 实例。
     * 该方法会将用户的角色转换为 GrantedAuthority 集合。如果角色名称匹配 ERole 枚举，
     * 则使用枚举值；否则直接使用原始字符串作为权限名。
     *
     * @param user User 实体对象
     * @return 构建好的 UserDetailsImpl 实例
     */
    public static UserDetailsImpl build(User user) {
        // 将用户的角色列表转换为 Spring Security 所需的权限列表
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> {
                    try {
                        ERole eRole = ERole.valueOf(role.getName());
                        return new SimpleGrantedAuthority(eRole.name());
                    } catch (IllegalArgumentException e) {
                        // 如果转换失败，直接使用原始字符串
                        return new SimpleGrantedAuthority(role.getName());
                    }
                })
                .collect(Collectors.toList());

        // 根据用户状态判断是否启用（status为1表示启用）
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                enabled,
                authorities);
    }

    /**
     * 账户是否未过期。
     *
     * @return true 表示账户未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定。
     *
     * @return true 表示账户未锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭证（密码）是否未过期。
     *
     * @return true 表示凭证未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否启用。
     *
     * @return true 表示账户已启用
     */
    @Override
    public boolean isEnabled() {
        return this.enabled; // 返回实际的启用状态
    }

    /**
     * 判断两个 UserDetailsImpl 对象是否相等。
     * 比较依据是它们的 id 是否相同。
     *
     * @param o 要比较的对象
     * @return 如果相等返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(id, that.id);
    }

    /**
     * 根据 id 计算哈希码。
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}