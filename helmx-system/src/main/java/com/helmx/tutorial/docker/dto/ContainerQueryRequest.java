package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;


@Data
@ApiModel(description = "容器查询条件")
public class ContainerQueryRequest {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    private String containerId;

    @ApiModelProperty(value = "容器名称")
    private String name;

    @ApiModelProperty(value = "容器状态", example = "created, running, paused, restarting, removing, exited, dead")
    private String state;

    @ApiModelProperty(value = "过滤器")
    private Map<String, String> filters;

    @ApiModelProperty(value = "排序字段")
    private String sortBy;

    @ApiModelProperty(value = "排序顺序")
    private String sortOrder; // asc, desc

    @ApiModelProperty(value = "当前页码")
    private Integer page;

    @ApiModelProperty(value = "每页数量")
    private Integer pageSize;
}
