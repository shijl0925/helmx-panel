package com.helmx.tutorial.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor  // 为类提供一个无参的构造方法
@AllArgsConstructor // 为类提供一个全参的构造方法
public class Result implements Serializable {
    @ApiModelProperty(value = "返回信息")
    private String message;

    @ApiModelProperty(value = "返回数据")
    private Object data;

    @ApiModelProperty(value = "返回码")
    private Integer Code;
}
