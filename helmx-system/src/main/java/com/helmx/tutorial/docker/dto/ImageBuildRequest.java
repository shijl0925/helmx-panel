package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImageBuildRequest {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "dockerfile内容")
    private String dockerfile;

    @ApiModelProperty(value = "镜像构建参数")
    private String buildArgs;

    @ApiModelProperty(value = "是否拉取镜像")
    private Boolean pull;

    @ApiModelProperty(value = "是否使用缓存")
    private Boolean noCache;

    @ApiModelProperty(value = "Labels")
    private String labels;

    @ApiModelProperty(value = "镜像构建标签")
    private String[] tags;

    public ImageBuildRequest() {}
}
