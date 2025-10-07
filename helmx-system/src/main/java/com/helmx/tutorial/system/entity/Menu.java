package com.helmx.tutorial.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.helmx.tutorial.entity.BaseEntity;

@Getter
@Setter
@TableName("tb_rbac_menus")
public class Menu extends BaseEntity implements Serializable {

    @Serial
    @TableField()
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO, value = "id")
    @ApiModelProperty(value = "菜单ID", hidden = true)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "菜单名称", required = true)
    private String name;

    @ApiModelProperty(value = "父级菜单ID")
    @JsonProperty(value = "pid")
    private Long parentId;

    @ApiModelProperty(value = "类型")
    private String type;

    @ApiModelProperty(value = "授权码")
    private String authCode;

    @ApiModelProperty(value = "路由地址")
    private String path;

    @ApiModelProperty(value = "组件")
    private String component;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "激活路径")
    private String activePath;

    @ApiModelProperty(value = "图标")
    private String icon;

    @ApiModelProperty(value = "排序")
    private Integer sort;

    @ApiModelProperty(value = "标题")
    private String title;

    // 添加子菜单字段（不在数据库表中）
    @TableField(exist = false)
    @ApiModelProperty(value = "子菜单列表")
    private List<Menu> children;

    @TableField(exist = false)
    @ApiModelProperty(value = "Meta")
    private Map<String, Object> meta;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Menu menu = (Menu) o;
        return Objects.equals(id, menu.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
