package com.helmx.tutorial.docker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.helmx.tutorial.entity.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@TableName("tb_template")
public class Template extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "模板ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "模板名称", required = true)
    private String name;

    @ApiModelProperty(value = "模板描述")
    private String remark;

    @ApiModelProperty(value = "模板内容")
    private String content;

    @ApiModelProperty(value = "模板类型")
    private String type = "Dockerfile";
}