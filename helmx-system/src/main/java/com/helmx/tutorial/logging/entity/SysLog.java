package com.helmx.tutorial.logging.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@TableName("tb_sys_log")
public class SysLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    @ApiModelProperty(value = "操作用户")
    private String username;

    @TableField("description")
    @ApiModelProperty(value = "描述")
    private String description;

    @TableField("method")
    @ApiModelProperty(value = "方法名")
    private String method;

    @TableField("params")
    @ApiModelProperty(value = "参数")
    private String params;

    @TableField("log_type")
    @ApiModelProperty(value = "日志类型")
    private String logType;

    @TableField("request_ip")
    @ApiModelProperty(value = "请求IP")
    private String requestIp;

    @TableField("address")
    @ApiModelProperty(value = "地址")
    private String address;

    @TableField("browser")
    @ApiModelProperty(value = "浏览器")
    private String browser;

    @TableField("user_agent")
    @ApiModelProperty(value = "User-Agent")
    private String userAgent;

    @TableField("time")
    @ApiModelProperty(value = "请求耗时(ms)")
    private Long time;

    @TableField("exception_detail")
    @ApiModelProperty(value = "异常详细")
    @JSONField(serialize = false)
    private String exceptionDetail;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建时间")
    @JsonProperty(value = "createTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createdAt;

    public SysLog(String logType, Long time) {
        this.logType = logType;
        this.time = time;
    }
}
