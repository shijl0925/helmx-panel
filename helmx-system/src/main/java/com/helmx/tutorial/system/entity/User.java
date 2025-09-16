package com.helmx.tutorial.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import com.helmx.tutorial.entity.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@TableName("tb_users")
public class User extends BaseEntity implements Serializable {

    @Serial
    @TableField()
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "用户ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "用户名", required = true)
    private String username;

    @NotBlank
    @ApiModelProperty(value = "密码", required = true, hidden = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @TableField(value = "nickname")
    @ApiModelProperty(value = "昵称")
    @JsonProperty(value = "nickName")
    private String nickname;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @Email
    @NotBlank
    @ApiModelProperty(value = "邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @NotNull
    @ApiModelProperty(value = "是否是超级管理员")
    private boolean isSuperAdmin;

    @TableField(exist = false)
    @ApiModelProperty(value = "用户角色")
    private Set<Role> roles;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
