package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class removeImageRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "host cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "是否强制")
    private Boolean force;

    @ApiModelProperty(value = "镜像ID")
    @NotBlank(message = "imageId cannot be blank")
    private String imageId;
}
