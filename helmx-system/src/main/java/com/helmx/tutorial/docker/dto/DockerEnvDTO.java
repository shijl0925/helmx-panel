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

    // 新增TLS相关字段
    @ApiModelProperty(value = "是否启用TLS验证")
    private Boolean tlsVerify;

    public DockerEnvDTO(DockerEnv dockerEnv) {
        this.id = dockerEnv.getId();
        this.name = dockerEnv.getName();
        this.remark = dockerEnv.getRemark();
        this.host = dockerEnv.getHost();
        this.status = dockerEnv.getStatus();
        this.tlsVerify = dockerEnv.getTlsVerify();
    }
}
