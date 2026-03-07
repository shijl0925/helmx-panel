package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkContainerOperationRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID列表")
    @NotEmpty(message = "Container ID list cannot be empty")
    private List<String> containerIds;

    @ApiModelProperty(value = "操作类型: start, stop, restart, remove, pause, unpause, kill")
    @NotBlank(message = "Operation cannot be blank")
    private String operation;

    @ApiModelProperty(value = "强制删除 (仅remove操作时有效)")
    private boolean force = false;
}
