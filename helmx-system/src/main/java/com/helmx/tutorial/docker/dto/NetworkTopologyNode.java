package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NetworkTopologyNode {

    @ApiModelProperty(value = "网络ID")
    private String networkId;

    @ApiModelProperty(value = "网络名称")
    private String name;

    @ApiModelProperty(value = "驱动")
    private String driver;

    @ApiModelProperty(value = "作用域")
    private String scope;

    @ApiModelProperty(value = "子网")
    private String subnet;

    @ApiModelProperty(value = "网关")
    private String gateway;

    @ApiModelProperty(value = "已连接容器（containerId, name, ipv4Address, macAddress）")
    private List<Map<String, String>> containers;
}
