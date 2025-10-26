package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerNetworkUpdateRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    @NotBlank(message = "Container ID cannot be blank")
    private String containerId;

    @ApiModelProperty(value = "网络")
    private String[] networks;
}
