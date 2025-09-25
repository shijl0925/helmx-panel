package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import com.helmx.tutorial.docker.entity.DockerEnv;

@Data
public class DockerEnvDTO {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "描述")
    private String remark;

    @ApiModelProperty(value = "主机地址")
    private String host;

    @ApiModelProperty(value = "状态")
    private Integer status;

    public DockerEnvDTO(DockerEnv dockerEnv) {
        this.id = dockerEnv.getId();
        this.name = dockerEnv.getName();
        this.remark = dockerEnv.getRemark();
        this.host = dockerEnv.getHost();
        this.status = dockerEnv.getStatus();
    }
}
