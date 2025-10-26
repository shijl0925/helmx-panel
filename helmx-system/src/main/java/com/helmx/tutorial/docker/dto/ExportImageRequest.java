package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExportImageRequest {

    @ApiModelProperty(value = "Docker主机地址", required = true)
    @NotBlank(message = "Docker host cannot be blank")
    private String host;

    @ApiModelProperty(value = "镜像名称", required = true)
    @NotBlank(message = "Image name cannot be blank")
    private String imageName;

    @ApiModelProperty(value = "文件名称")
    @NotBlank(message = "Filename cannot be blank")
    private String filename;
}
