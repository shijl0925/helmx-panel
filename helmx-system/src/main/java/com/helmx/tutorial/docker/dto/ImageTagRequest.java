package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageTagRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "镜像ID")
    @NotBlank(message = "Image ID cannot be blank")
    private String imageId;

    @ApiModelProperty(value = "镜像名称")
    @NotBlank(message = "Image name cannot be blank")
    private String imageName;
}
