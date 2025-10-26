package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PruneRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "类型")
    @NotBlank(message = "Prune type cannot be blank")
    private String pruneType; // BUILD CONTAINERS IMAGES NETWORKS VOLUMES
}
