package com.helmx.tutorial.docker.dto;

import com.alibaba.fastjson2.util.DateUtils;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerPort;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class ContainerDTO {
    @ApiModelProperty(value = "容器ID")
    private String id;

    @ApiModelProperty(value = "容器名称")
    private String name;

    @ApiModelProperty(value = "容器状态")
    private String state;

    @ApiModelProperty(value = "镜像ID")
    private String imageId;

    @ApiModelProperty(value = "镜像名称")
    private String image;

    @ApiModelProperty(value = "创建时间")
    private String createTime;

    @ApiModelProperty(value = "启动时间")
    private String status;

    @ApiModelProperty(value = "IP地址")
    private String ipAddress;

    @ApiModelProperty(value = "端口")
    private String ports;

    @ApiModelProperty(value = "容器资源状态")
    private ContainerStats stats;

    @ApiModelProperty(value = "网络")
    private List<String> networks;

    public ContainerDTO(Container container) {
        // 容器ID
        this.id = container.getId();
        // 容器名称
        this.name = container.getNames()[0].substring(1);
        // 容器状态
        this.state = container.getState();
        // 镜像ID
        this.imageId = Objects.requireNonNull(container.getImageId()).split(":")[1];
        // 镜像名称
        this.image = container.getImage();
        // 创建时间
        this.createTime = DateUtils.format(new Date(container.getCreated() * 1000L));
        // 运行状态
        this.status = container.getStatus();
        // 容器网络
        this.ipAddress = Objects.requireNonNull(container.getNetworkSettings()).getNetworks().values().stream()
                .map(ContainerNetwork::getIpAddress)
                .collect(Collectors.joining(", "));
        // 容器端口
        ContainerPort[] ports = container.getPorts();
        this.ports = Arrays.stream(ports).sequential()
                .map(containerPort -> {
                    String privatePort = containerPort.getPrivatePort() != null ? containerPort.getPrivatePort().toString() : "";
                    String type = containerPort.getType() != null ? containerPort.getType() : "tcp";

                    String result;

                    // 如果有公网端口映射，则显示完整映射信息
                    if (containerPort.getPublicPort() != null) {
                        String ip = containerPort.getIp() != null ? containerPort.getIp() : "0.0.0.0";
                        result = ip + ":" + containerPort.getPublicPort() + "->" + privatePort + "/" + type;
                    }
                    // 如果只有私有端口且有IP，则显示IP和私有端口
                    else if (containerPort.getIp() != null) {
                        result = containerPort.getIp() + ":" + privatePort + "/" + type;
                    }
                    // 如果只有私有端口且没有IP，则只显示私有端口
                    else {
                        result = privatePort + "/" + type;
                    }

                    return result;
                })
                .filter(port -> !port.isEmpty()) // 过滤掉空端口
                .collect(Collectors.joining(", "));

        // 网络
        this.networks = container.getNetworkSettings().getNetworks().keySet().stream().toList();
    }
}
