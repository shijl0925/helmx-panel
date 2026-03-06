package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContainerResourceUpdateRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    @NotBlank(message = "Container ID cannot be blank")
    private String containerId;

    @ApiModelProperty(value = "CPU份额 (相对权重, 默认1024)")
    private Integer cpuShares;

    @ApiModelProperty(value = "CPU配额(微秒) -1表示无限制")
    private Long cpuQuota;

    @ApiModelProperty(value = "CPU周期(微秒), 默认100000")
    private Long cpuPeriod;

    @ApiModelProperty(value = "内存限制(字节), 0表示无限制")
    private Long memory;

    @ApiModelProperty(value = "内存+Swap限制(字节), -1表示无限制, 0表示与memory相同")
    private Long memorySwap;

    @ApiModelProperty(value = "内存软限制(字节)")
    private Long memoryReservation;

    @ApiModelProperty(value = "Block IO权重 (10-1000)")
    private Integer blkioWeight;
}
