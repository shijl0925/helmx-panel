package com.helmx.tutorial.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_rbac_user_roles")
@ApiModel(description = "用户角色关联")
public class UserRole {

    @TableField("user_id")
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @TableField("role_id")
    @ApiModelProperty(value = "角色ID")
    private Long roleId;
}
