package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StackCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "编排名称", required = true)
    private String name;

    @ApiModelProperty(value = "编排内容")
    private String content;
}
