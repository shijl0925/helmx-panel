package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageHubSearchRequest {

    @ApiModelProperty(value = "主机地址")
    @NotBlank(message = "Host address cannot be blank")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "搜索关键词")
    @NotBlank(message = "Search term cannot be blank")
    private String term;

    @ApiModelProperty(value = "最大返回数量（默认25）")
    private Integer limit = 25;
}
