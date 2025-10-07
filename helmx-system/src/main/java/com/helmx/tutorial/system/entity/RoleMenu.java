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
@TableName("tb_rbac_role_menus")
@ApiModel(description = "角色菜单关联")
public class RoleMenu {

    @TableField("role_id")
    @ApiModelProperty(value = "角色ID")
    private Long roleId;

    @TableField("menu_id")
    @ApiModelProperty(value = "菜单ID")
    private Long menuId;
}
