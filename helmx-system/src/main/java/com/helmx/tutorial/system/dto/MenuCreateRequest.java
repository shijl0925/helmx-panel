package com.helmx.tutorial.system.dto;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;


@Data
@ApiModel(description = "菜单创建请求")
public class MenuCreateRequest {
    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "父级菜单ID")
    private Long pid;

    @ApiModelProperty(value = "类型")
    private String type;

    @ApiModelProperty(value = "授权码")
    private String authCode;

    @ApiModelProperty(value = "路由地址")
    private String path;

    @ApiModelProperty(value = "组件")
    private String component;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "激活路径")
    private String activePath;

    @ApiModelProperty(value = "菜单Meta信息")
    private MenuMetaRequest meta;
}
