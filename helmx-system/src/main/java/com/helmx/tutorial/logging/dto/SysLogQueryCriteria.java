package com.helmx.tutorial.logging.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SysLogQueryCriteria {

    @ApiModelProperty(value = "用户名称")
    private String username;

    @ApiModelProperty(value = "日志类型")
    private String logType;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数据量", example = "10")
    private Integer size = 10;
}
