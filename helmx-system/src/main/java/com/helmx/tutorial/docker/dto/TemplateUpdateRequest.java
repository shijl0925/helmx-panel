package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TemplateUpdateRequest {

    @ApiModelProperty(value = "模板名称")
    private String name;

    @ApiModelProperty(value = "环境描述")
    private String remark;

    @ApiModelProperty(value = "模板内容")
    private String content;

    @ApiModelProperty(value = "模板类型")
    private String type;
}