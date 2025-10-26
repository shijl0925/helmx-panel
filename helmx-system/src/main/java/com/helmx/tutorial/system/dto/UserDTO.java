package com.helmx.tutorial.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import com.helmx.tutorial.system.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Data
public class UserDTO {

    @ApiModelProperty(value = "用户ID")
    private Long id;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "昵称")
    @JsonProperty("nickName") // 指定序列化时的字段名
    private String nickname;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    @JsonProperty("createTime")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @JsonProperty("updateTime")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "角色名称集合")
    private Set<String> roles;

    @ApiModelProperty(value = "角色ID集合")
    private Set<Long> role;


    @ApiModelProperty(value = "是否在线")
    private Boolean online;

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        this.status = user.getStatus();

        // 添加空值检查
        if (user.getCreatedAt() != null) {
            this.createdAt = user.getCreatedAt().toLocalDateTime();
        }
        if (user.getUpdatedAt() != null) {
            this.updatedAt = user.getUpdatedAt().toLocalDateTime();
        }
    }
}
