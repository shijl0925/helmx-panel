package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ContainerExecResponse {

    @ApiModelProperty(value = "执行ID")
    private String execId;

    @ApiModelProperty(value = "执行结果")
    private String output;

    @ApiModelProperty(value = "执行状态")
    private String status;

    @ApiModelProperty(value = "错误信息")
    private String error;
}