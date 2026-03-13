package com.helmx.tutorial.docker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import com.helmx.tutorial.entity.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@TableName("tb_docker_env")
public class DockerEnv extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "环境ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "环境名称", required = true)
    private String name;

    @ApiModelProperty(value = "环境描述")
    private String remark;

    @ApiModelProperty(value = "Docker连接地址", required = true)
    private String host;

    @ApiModelProperty(value = "状态")
    private Integer status;

    // TLS相关字段
    @ApiModelProperty(value = "是否启用TLS验证")
    private Boolean tlsVerify = false;

    @ApiModelProperty(value = "是否启用SSH远程连接")
    private Boolean sshEnabled = false;

    @ApiModelProperty(value = "SSH端口")
    private Integer sshPort = 22;

    @ApiModelProperty(value = "SSH用户名")
    private String sshUsername;

    @ApiModelProperty(value = "SSH加密密码，可为空；为空时尝试使用后端主机现有的SSH私钥", hidden = true)
    private String sshPassword;

    @ApiModelProperty(value = "SSH主机指纹")
    private String sshHostKeyFingerprint;

    @ApiModelProperty(value = "环境类型，如 dev/test/uat/prod 等")
    private String envType;

    @ApiModelProperty(value = "集群名称，用于将多个主机归组到同一集群")
    private String clusterName;
}
