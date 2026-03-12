package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VolumeBackupRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "host cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "卷名称")
    @NotBlank(message = "name cannot be blank")
    private String name;

    @ApiModelProperty(value = "备份路径（卷内目录，默认为 /）")
    private String path = "/";
}
