package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class StackUpdateRequest {

    @ApiModelProperty(value = "编排名称")
    private String name;

    @ApiModelProperty(value = "编排内容")
    private String content;
}