package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistryConnectRequest {
    @ApiModelProperty(value = "URL")
    @NotBlank(message = "url cannot be blank")
    private String url;

    @ApiModelProperty(value = "用户名")
    @NotBlank(message = "username cannot be blank")
    private String username;

    @ApiModelProperty(value = "密码")
    @NotBlank(message = "password cannot be blank")
    private String password;
}
