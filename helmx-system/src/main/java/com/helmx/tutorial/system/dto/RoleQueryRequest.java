package com.helmx.tutorial.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "角色查询条件")
public class RoleQueryRequest {
    @ApiModelProperty(value = "角色名称", example = "Admin")
    private String name;

    @ApiModelProperty(value = "角色编码", example = "admin")
    private String code;

    @ApiModelProperty(value = "角色ID")
    private Integer id;

    @ApiModelProperty(value = "角色描述")
    private String remark;

    @ApiModelProperty(value = "角色状态")
    private Integer status;

    @ApiModelProperty(value = "起始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;
}
