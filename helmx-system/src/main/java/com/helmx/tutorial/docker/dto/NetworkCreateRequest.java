package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NetworkCreateRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "名称")
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @ApiModelProperty(value = "驱动")
    private String driver;

    @ApiModelProperty(value = "驱动参数")
    private String[] driverOpts;

    @ApiModelProperty(value = "是否使用IPv4")
    private Boolean enableIpv4;

    @ApiModelProperty(value = "子网")
    private String subnet;

    @ApiModelProperty(value = "网关")
    private String gateway;

    @ApiModelProperty(value = "IP范围")
    private String ipRange;

    //auxAddress

    @ApiModelProperty(value = "是否使用IPv6")
    private Boolean enableIpv6;

    @ApiModelProperty(value = "子网V6")
    private String subnetV6;

    @ApiModelProperty(value = "网关V6")
    private String gatewayV6;

    @ApiModelProperty(value = "IP范围V6")
    private String ipRangeV6;

    @ApiModelProperty(value = "标签")

    // auxAddressV6

    private String[] labels;
}
