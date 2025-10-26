package com.helmx.tutorial.docker.dto;


import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerCopyRequest {

    @ApiModelProperty(value = "Docker主机地址", required = true)
    @NotBlank(message = "Host address cannot be blank")
    private String host;

    @ApiModelProperty(value = "容器ID", required = true)
    @NotBlank(message = "Container ID cannot be blank")
    private String containerId;

    @ApiModelProperty(value = "容器内路径", required = true)
    @NotBlank(message = "Container path cannot be blank")
    private String containerPath;
}
