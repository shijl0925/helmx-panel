package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VolumeQueryRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "host cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "存储卷名称")
    private String name;

    @ApiModelProperty(value = "排序顺序")
    private String sortOrder; // asc, desc

    @ApiModelProperty(value = "当前页码")
    private Integer page;

    @ApiModelProperty(value = "每页数量")
    private Integer pageSize;
}
