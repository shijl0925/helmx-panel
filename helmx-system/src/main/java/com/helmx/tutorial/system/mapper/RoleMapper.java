package com.helmx.tutorial.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.helmx.tutorial.system.dto.RoleQueryRequest;
import com.helmx.tutorial.system.entity.Menu;
import com.helmx.tutorial.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
//import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
//    /* 方法1：基于注解的 SQL 查询方式  */
//    @Select("SELECT r.* FROM tb_rbac_roles r INNER JOIN tb_rbac_user_roles ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
//    Set<Role> findRolesByUserId(@Param("userId") Long userId);

    /* 方法2：使用 XML 映射文件 */
    Set<Role> findRolesByUserId(Long userId);
    Set<Long> findMenuIdsByRoleId(Long roleId);
    Set<Menu> findMenusByRoleId(Long roleId);

    /*
     * 根据条件查询角色列表
     */
    List<Role> findRolesByConditions(@Param("criteria") RoleQueryRequest criteria);
}
