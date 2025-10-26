package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerRenameRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    @NotBlank(message = "Container ID cannot be blank")
    private String containerId;

    @ApiModelProperty(value = "名字")
    @NotBlank(message = "Name cannot be blank")
    private String newName;
}
