package com.helmx.tutorial.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.helmx.tutorial.system.entity.UserRole;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    // 插入用户角色关联
    @Insert("INSERT INTO tb_rbac_user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    // 删除用户的所有角色关联
    @Delete("DELETE FROM tb_rbac_user_roles WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
