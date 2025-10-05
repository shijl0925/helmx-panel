package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DockerEnvCreateRequest {

    @ApiModelProperty(value = "名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @ApiModelProperty(value = "描述")
    private String remark;

    @ApiModelProperty(value = "地址")
    @NotBlank(message = "地址不能为空")
    private String host;

    // 新增TLS相关字段
    @ApiModelProperty(value = "是否启用TLS验证")
    private Boolean tlsVerify = false;
}
