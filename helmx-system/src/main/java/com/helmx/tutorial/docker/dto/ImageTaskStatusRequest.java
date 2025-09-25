package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ImageTaskStatusRequest {

    @ApiModelProperty(value = "任务ID")
    private String taskId;
}
