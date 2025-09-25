package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
public class RestartPolicyRequest {
    @ApiModelProperty(value = "策略名称")
    private String name;

    @ApiModelProperty(value = "重启次数")
    private Integer maximumRetryCount;
}

