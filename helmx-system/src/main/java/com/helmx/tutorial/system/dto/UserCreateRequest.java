package com.helmx.tutorial.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Set;

@Data
@ApiModel(description = "用户更新请求")
public class UserCreateRequest {

    @ApiModelProperty(value = "用户名", required = true)
    private String username;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "密码", required = true)
    private String password;

    @ApiModelProperty(value = "昵称")
    private String nickName;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "角色")
    private Set<Integer> role;
}

