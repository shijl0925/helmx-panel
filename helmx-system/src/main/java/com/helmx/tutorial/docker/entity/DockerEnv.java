package com.helmx.tutorial.docker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
}
