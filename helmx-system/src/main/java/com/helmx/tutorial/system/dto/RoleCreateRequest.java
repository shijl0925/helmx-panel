package com.helmx.tutorial.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "角色创建请求")
public class RoleCreateRequest {
    @ApiModelProperty(value = "角色名称")
    private String name;

    @ApiModelProperty(value = "角色描述")
    private String remark;

    @ApiModelProperty(value = "角色状态")
    private Integer status;

    @ApiModelProperty(value = "角色编码")
    private String code;

    @ApiModelProperty(value = "菜单权限")
    private List<Integer> permissions;

    @ApiModelProperty(value = "接口权限")
    private List<Integer> resources;
}
