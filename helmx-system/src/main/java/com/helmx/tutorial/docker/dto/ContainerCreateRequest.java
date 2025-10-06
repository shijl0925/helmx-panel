package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

@Data
public class ContainerCreateRequest {

    @ApiModelProperty(value = "主机地址")
    private String host = "unix:///var/run/docker.sock";

    @ApiModelProperty(value = "容器ID")
    private String containerId;

    @ApiModelProperty(value = "名称")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.-]*$", message = "Container names can only contain letters, numbers, underscores, dots, and hyphens, and must start with a letter or number.")
    private String name;

    @ApiModelProperty(value = "命令")
    private String[] cmd;

    @ApiModelProperty(value = "启动命令")
    private String[] entrypoint;

    @ApiModelProperty(value = "环境变量")
    private String[] env;

    @ApiModelProperty(value = "端口映射")
    private PortHelper[] exposedPorts; // hostPort containerPort protocol

    @ApiModelProperty(value = "卷")
    private VolumeHelper[] volumes;

    @ApiModelProperty(value = "镜像")
    private String image;

    @ApiModelProperty(value = "是否打开标准输入")
    private Boolean stdinOpen;

    @ApiModelProperty(value = "是否使用TTY")
    private Boolean tty;

    @ApiModelProperty(value = "是否特权模式")
    private Boolean privileged;

    @ApiModelProperty(value = "退出时自动删除容器")
    private Boolean autoRemove;

    @ApiModelProperty(value = "标签")
    private String[] labels; // e.g key=value

    @ApiModelProperty(value = "CPU权重")
    private Integer cpuShares;

    @ApiModelProperty(value = "CPU核数")
    private Float cpuNano;

    @ApiModelProperty(value = "内存限制")
    private Float memory; // 单位MB

    @ApiModelProperty(value = "网络模式")
    private String networkMode; // e.g bridge host none

    @ApiModelProperty(value = "网络名称数组")
    private String[] networks;

    @ApiModelProperty(value = "重启策略")
    private RestartPolicyRequest restartPolicy;

    @ApiModelProperty(value = "工作目录")
    private String workingDir;

    @ApiModelProperty(value = "用户")
    private String user;

    @ApiModelProperty(value = "Hostname")
    private String hostName;

    private String domainName;

    private String macAddress;

    private String[] dns;

    @ApiModelProperty(value = "Devices")
    private Map<String, String> devices;
}
