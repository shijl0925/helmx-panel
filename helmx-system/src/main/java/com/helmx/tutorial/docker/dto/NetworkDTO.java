package com.helmx.tutorial.docker.dto;

import com.github.dockerjava.api.model.Network;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class NetworkDTO {

    @ApiModelProperty(value = "网络ID")
    private String id;

    @ApiModelProperty(value = "网络名称")
    private String name;

    @ApiModelProperty(value = "驱动")
    private String driver;

    @ApiModelProperty(value = "IPAM驱动")
    private String ipamDriver;

    @ApiModelProperty(value = "驱动参数")
    private Map<String, String> options;

    @ApiModelProperty(value = "子网")
    private String subnet;

    @ApiModelProperty(value = "网关")
    private String gateway;

    @ApiModelProperty(value = "IP范围")
    private String ipRange;

    @ApiModelProperty(value = "标签")
    private Map<String, String> labels;

    @ApiModelProperty(value = "作用域")
    private String scope;

    @ApiModelProperty(value = "是否启用IPv6")
    private Boolean enableIPv6;

    @ApiModelProperty(value = "是否可连接")
    private Boolean attachable;

    @ApiModelProperty(value = "创建时间")
    private String createdAt;

    @ApiModelProperty(value = "是否正在使用")
    private Boolean isUsed;

    @ApiModelProperty(value = "容器列表")
    private List<Map<String, String>> containers;

    // 线程安全的日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NetworkDTO(Network network) {
        log.info("NetworkDTO: {}", network);
        this.id = network.getId();
        this.name = network.getName();
        this.driver = network.getDriver();
        this.ipamDriver = network.getIpam().getDriver();
        this.attachable = network.isAttachable();

        List<Network.Ipam.Config> config = network.getIpam().getConfig();
        if (config != null && !config.isEmpty()) {
            Network.Ipam.Config ipamConfig = config.getFirst();
            this.gateway = ipamConfig.getGateway();
            this.subnet = ipamConfig.getSubnet();
            this.ipRange = ipamConfig.getIpRange();
        }

        this.labels = network.getLabels();
//        this.labels = network.getLabels().entrySet().stream()
//                .map(entry -> entry.getKey() + "=" + entry.getValue())
//                .toArray(String[]::new);

        this.options = network.getOptions();
//        this.options = network.getOptions().entrySet().stream()
//                .map(entry -> entry.getKey() + "=" + entry.getValue())
//                .toArray(String[]::new);

        // 格式化创建时间
        Date created = network.getCreated();
        if (created != null) {
            // 将 Date 转换为 LocalDateTime 并格式化
            Instant instant = created.toInstant();
            LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            this.createdAt = DATE_FORMATTER.format(localDateTime);
        } else {
            this.createdAt = null;
        }

        // 获取容器信息
        Map<String, Network.ContainerNetworkConfig> containers = network.getContainers();
        // 使用 Stream API 替代传统的 for 循环，代码更简洁易读
        this.containers = (containers != null) ? 
            containers.entrySet().stream()
                .map(entry -> {
                    Network.ContainerNetworkConfig networkConfig = entry.getValue();
                    Map<String, String> container = new HashMap<>();
                    container.put("id", entry.getKey());
                    container.put("name", Optional.ofNullable(networkConfig.getName()).orElse(""));
                    container.put("ipv4Address", Optional.ofNullable(networkConfig.getIpv4Address()).orElse(""));
                    container.put("ipv6Address", Optional.ofNullable(networkConfig.getIpv6Address()).orElse(""));
                    container.put("macAddress", Optional.ofNullable(networkConfig.getMacAddress()).orElse(""));
                    return container;
                })
                .collect(Collectors.toList()) : 
            new ArrayList<>();
    }
}