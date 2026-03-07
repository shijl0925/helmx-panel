package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NetworkSearchRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "网络名称关键字")
    private String name;

    @ApiModelProperty(value = "驱动类型（bridge/overlay/host/macvlan）")
    private String driver;

    @ApiModelProperty(value = "作用域（local/swarm/global）")
    private String scope;

    @ApiModelProperty(value = "排序顺序（asc/desc）")
    private String sortOrder;

    @ApiModelProperty(value = "当前页码")
    private Integer page;

    @ApiModelProperty(value = "每页数量")
    private Integer pageSize;
}
