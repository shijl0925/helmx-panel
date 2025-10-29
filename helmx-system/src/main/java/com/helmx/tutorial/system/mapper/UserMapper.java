package com.helmx.tutorial.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.helmx.tutorial.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 检查用户是否是超级管理员
     *
     * @param userId 用户ID
     * @return 如果用户是超级管理员返回true，否则返回false
     */
    @Select("SELECT COUNT(*) > 0 FROM tb_users WHERE id = #{userId} AND is_super_admin = true AND status = 1")
    boolean isSuperAdmin(@Param("userId") Long userId);

    /**
     * 查询用户的所有权限
     */
    Set<String> selectUserPermissions(@Param("userId") Long userId);
}
