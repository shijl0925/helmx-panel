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

    @ApiModelProperty(value = "是否启用SSH远程连接")
    private Boolean sshEnabled;

    @ApiModelProperty(value = "SSH端口")
    private Integer sshPort;

    @ApiModelProperty(value = "SSH用户名")
    private String sshUsername;

    @ApiModelProperty(value = "SSH主机指纹")
    private String sshHostKeyFingerprint;

    @ApiModelProperty(value = "是否已配置SSH密码")
    private Boolean sshPasswordConfigured;

    @ApiModelProperty(value = "环境类型，如 dev/test/uat/prod 等")
    private String envType;

    @ApiModelProperty(value = "集群名称，用于将多个主机归组到同一集群")
    private String clusterName;

    public DockerEnvDTO(DockerEnv dockerEnv) {
        this.id = dockerEnv.getId();
        this.name = dockerEnv.getName();
        this.remark = dockerEnv.getRemark();
        this.host = dockerEnv.getHost();
        this.status = dockerEnv.getStatus();
        this.tlsVerify = dockerEnv.getTlsVerify();
        this.sshEnabled = dockerEnv.getSshEnabled();
        this.sshPort = dockerEnv.getSshPort();
        this.sshUsername = dockerEnv.getSshUsername();
        this.sshHostKeyFingerprint = dockerEnv.getSshHostKeyFingerprint();
        this.sshPasswordConfigured = dockerEnv.getSshPassword() != null && !dockerEnv.getSshPassword().isBlank();
        this.envType = dockerEnv.getEnvType();
        this.clusterName = dockerEnv.getClusterName();
    }
}
