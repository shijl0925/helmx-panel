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
@TableName("tb_stack")
public class Stack extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "编排ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "编排名称", required = true)
    private String name;

    @ApiModelProperty(value = "编排内容")
    private String content;
}