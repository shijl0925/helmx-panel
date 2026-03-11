package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DockerEnvCreateRequest {

    @ApiModelProperty(value = "名称")
    @NotBlank(message = "Name cannot be blank")
    @NotBlank(message = "名称不能为空")
    private String name;

    @ApiModelProperty(value = "描述")
    private String remark;

    @ApiModelProperty(value = "地址")
    @NotBlank(message = "Address cannot be blank")
    private String host;

    // 新增TLS相关字段
    @ApiModelProperty(value = "是否启用TLS验证")
    private Boolean tlsVerify = false;

    @ApiModelProperty(value = "是否启用远程主机资源采集")
    private Boolean sshEnabled = false;

    @ApiModelProperty(value = "SSH端口")
    private Integer sshPort = 22;

    @ApiModelProperty(value = "SSH用户名")
    private String sshUsername;

    @ApiModelProperty(value = "SSH密码")
    private String sshPassword;

    @ApiModelProperty(value = "SSH主机指纹")
    private String sshHostKeyFingerprint;
}
