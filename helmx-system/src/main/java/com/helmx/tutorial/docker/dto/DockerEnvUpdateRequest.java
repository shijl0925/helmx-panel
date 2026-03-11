package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DockerEnvUpdateRequest {

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "描述")
    private String remark;

    @ApiModelProperty(value = "地址")
    private String host;

    @ApiModelProperty(value = "状态")
    private Integer status;

    // TLS相关字段
    @ApiModelProperty(value = "是否启用TLS验证")
    private Boolean tlsVerify;

    @ApiModelProperty(value = "是否启用SSH远程连接")
    private Boolean sshEnabled;

    @ApiModelProperty(value = "SSH端口")
    private Integer sshPort;

    @ApiModelProperty(value = "SSH用户名")
    private String sshUsername;

    @ApiModelProperty(value = "SSH密码")
    private String sshPassword;

    @ApiModelProperty(value = "SSH主机指纹")
    private String sshHostKeyFingerprint;
}
