package com.helmx.tutorial.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import com.helmx.tutorial.entity.BaseEntity;

@Getter
@Setter
@TableName("tb_rbac_roles")
public class Role extends BaseEntity implements Serializable {

    @Serial
    @TableField()
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "角色ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "角色名称", required = true)
    private String name;

    @ApiModelProperty(value = "角色描述")
    private String remark;

    @ApiModelProperty(value = "角色状态")
    private Integer status;

    @ApiModelProperty(value = "角色编码")
    private String code;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
