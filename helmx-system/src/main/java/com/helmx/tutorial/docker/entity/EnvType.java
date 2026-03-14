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
@TableName("tb_env_type")
public class EnvType extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "环境类型代码，如 dev/test/uat/prod", required = true)
    private String code;

    @ApiModelProperty(value = "描述")
    private String remark;

    @ApiModelProperty(value = "排序")
    private Integer sort;
}
