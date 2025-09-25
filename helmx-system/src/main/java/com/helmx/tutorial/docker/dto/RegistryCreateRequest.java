package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RegistryCreateRequest {

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "URL")
    private String url;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "是否鉴权")
    private Boolean auth;
}
