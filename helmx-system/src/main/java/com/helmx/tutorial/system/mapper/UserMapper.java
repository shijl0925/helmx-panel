package com.helmx.tutorial.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.helmx.tutorial.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 检查用户是否具有指定权限
     *
     * @param userId 用户ID
     * @param permissions 权限代码列表
     * @return 如果用户具有所有指定权限返回true，否则返回false
     */
    boolean checkUserPermissions(@Param("userId") Long userId, @Param("permissions") List<String> permissions);
}
