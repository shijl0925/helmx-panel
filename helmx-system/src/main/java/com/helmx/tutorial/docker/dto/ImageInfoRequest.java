package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ImageInfoRequest {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "镜像ID")
    private String imageId;
}
