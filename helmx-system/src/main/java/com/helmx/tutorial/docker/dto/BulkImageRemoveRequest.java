package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkImageRemoveRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "host cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "镜像ID列表")
    @NotEmpty(message = "imageIds cannot be empty")
    private List<String> imageIds;

    @ApiModelProperty(value = "是否强制删除")
    private Boolean force = false;
}
