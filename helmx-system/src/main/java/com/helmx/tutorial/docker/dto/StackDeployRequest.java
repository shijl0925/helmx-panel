package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class StackDeployRequest {

    @ApiModelProperty(value = "编排内容")
    private String content;
}