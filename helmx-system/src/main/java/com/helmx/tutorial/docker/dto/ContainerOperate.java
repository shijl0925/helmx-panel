package com.helmx.tutorial.docker.dto;

import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.api.model.Ports;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Data
public class ContainerOperate {

    @ApiModelProperty(value = "容器ID")
    private String id;

    @ApiModelProperty(value = "容器名称")
    private String name;

    @ApiModelProperty(value = "状态")
    private String status;

    @ApiModelProperty(value = "健康检查")
    private String health;

    @ApiModelProperty(value = "镜像名称")
    private String image;

    @ApiModelProperty(value = "网络")
    private HashMap<String, Object> networks;

    @ApiModelProperty(value = "命令")
    private String[] cmd;

    @ApiModelProperty(value = "是否打开标准输入")
    private Boolean stdinOpen;

    @ApiModelProperty(value = "是否分配TTY")
    private Boolean tty;

    @ApiModelProperty(value = "启动命令")
    private String[] entrypoint;

    @ApiModelProperty(value = "环境变量")
    private String[] env;

    @ApiModelProperty(value = "CPU权重")
    private int cpuShares;

    @ApiModelProperty(value = "CPU限制")
    private float nanoCPUs;

    @ApiModelProperty(value = "内存限制")
    private long memory;

    @ApiModelProperty(value = "标签")
    private String[] labels;

    @ApiModelProperty(value = "自动删除")
    private Boolean autoRemove;

    @ApiModelProperty(value = "特权模式")
    private Boolean privileged;

    @ApiModelProperty(value = "端口映射")
    private Boolean publishAllPorts;

    @ApiModelProperty(value = "重启策略")
    private Map<String, Object> restartPolicy;

    @ApiModelProperty(value = "端口映射")
    private PortHelper[] exposedPorts;

    @ApiModelProperty(value = "挂载卷")
    private VolumeHelper[] volumes;

    @ApiModelProperty(value = "工作目录")
    private String workingDir;

    @ApiModelProperty(value = "主机名")
    private String hostName;

    @ApiModelProperty(value = "域名")
    private String domainName;

    @ApiModelProperty(value = "MAC地址")
    private String macAddress;

//    @ApiModelProperty(value = "IPv4地址")
//    private String ipv4Address;

    private Map<String, Object> healthCheck;

    @ApiModelProperty(value = "创建时间")
    private String created;

    @ApiModelProperty(value = "启动时间")
    private String startedAt;

    // 线程安全的日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ContainerOperate(InspectContainerResponse cr) {
        this.id = cr.getId();
        this.name = cr.getName().substring(1);
        this.status = cr.getState() != null ? cr.getState().getStatus(): "";

        HealthState hs = cr.getState() != null ? cr.getState().getHealth() : null;
        if (hs != null) {
            this.health = hs.getStatus() != null ? hs.getStatus() : "";
        }

        this.hostName = cr.getConfig().getHostName();
        this.domainName = cr.getConfig().getDomainName();
        this.macAddress = cr.getNetworkSettings().getMacAddress();
//        this.ipv4Address = cr.getNetworkSettings().getIpAddress();

        Map<String, Object> hcMapping = new HashMap<>();
        HealthCheck hc = cr.getConfig() != null ? cr.getConfig().getHealthcheck() : null;
        if (hc != null) {
            if (hc.getTest() != null) {
                hcMapping.put("test", hc.getTest());
            } else {
                hcMapping.put("test", null);
            }
            if (hc.getInterval() != null) {
                hcMapping.put("interval", (hc.getInterval() / 1000000000) + "s"); // 纳秒转秒
            } else {
                hcMapping.put("interval", null);
            }
            if (hc.getTimeout() != null) {
                hcMapping.put("timeout", (hc.getTimeout() / 1000000000) + "s"); // 纳秒转秒
            } else {
                hcMapping.put("timeout", null);
            }
            if (hc.getRetries() != null) {
                hcMapping.put("retries", hc.getRetries());
            } else {
                hcMapping.put("retries", 0);
            }
            if (hc.getStartInterval() != null) {
                hcMapping.put("startInterval", (hc.getStartInterval() / 1000000000) + "s");
            } else {
                hcMapping.put("startInterval", null);
            }
            if (hc.getStartPeriod() != null) {
                hcMapping.put("startPeriod", (hc.getStartPeriod() / 1000000000) + "s");
            } else {
                hcMapping.put("startPeriod", null);
            }
        }
        this.healthCheck = hcMapping;

        this.image = cr.getConfig().getImage() + "@" + cr.getImageId();
        if (cr.getNetworkSettings() != null) {
            Map<String, ContainerNetwork> networks = cr.getNetworkSettings().getNetworks();

            if (networks != null) {
                this.networks = new HashMap<>();

                for (Map.Entry<String, ContainerNetwork> entry : networks.entrySet()) {
                    // 网络模式
                    String networkName = entry.getKey();
                    if (networkName != null) {
                        String ipAddress = entry.getValue().getIpAddress() != null ? entry.getValue().getIpAddress() : "";
                        String gateway = entry.getValue().getGateway() != null ? entry.getValue().getGateway() : "";
                        String macAddress = entry.getValue().getMacAddress() != null ? entry.getValue().getMacAddress() : "";
                        this.networks.put(networkName, new HashMap<>(Map.of(
                                "ipAddress", ipAddress,
                                "gateway", gateway,
                                "macAddress", macAddress
                        )));
                    }
                }
            }
        }
        this.cmd = cr.getConfig().getCmd();
        this.stdinOpen = cr.getConfig().getStdinOpen();
        this.tty = cr.getConfig().getTty();
        this.entrypoint = cr.getConfig().getEntrypoint();
        this.env = cr.getConfig().getEnv();
        this.cpuShares = cr.getHostConfig().getCpuShares();

        Map<String, String> labels = cr.getConfig().getLabels();
        if (labels != null) {
            this.labels = labels.entrySet().stream().map(
                    entry -> entry.getKey() + "=" + entry.getValue()
            ).toArray(String[]::new);
        }

        List<PortHelper> portHelpers = new ArrayList<>();
        if (cr.getHostConfig() != null &&
                cr.getHostConfig().getPortBindings() != null &&
                cr.getHostConfig().getPortBindings().getBindings() != null) {
            cr.getHostConfig().getPortBindings().getBindings().forEach((key, bindings) -> {
                if (key != null && bindings != null) {
                    for (Ports.Binding binding : bindings) {
                        if (binding != null) {
                            PortHelper ph = new PortHelper();
                            ph.setHostIP(binding.getHostIp());
                            ph.setHostPort(binding.getHostPortSpec());
                            ph.setContainerPort(String.valueOf(key.getPort()));
                            ph.setProtocol(String.valueOf(key.getProtocol()));

                            portHelpers.add(ph);
                        }
                    }

                }
            });
        }
        this.exposedPorts = portHelpers.toArray(PortHelper[]::new);
//        this.exposedPorts = portHelpers.toArray(new PortHelper[0]);

        this.autoRemove = cr.getHostConfig().getAutoRemove();
        this.privileged = cr.getHostConfig().getPrivileged();
        this.publishAllPorts = cr.getHostConfig().getPublishAllPorts();
        // 重启策略
        String restartPolicyName = cr.getHostConfig().getRestartPolicy().getName();
        Integer restartPolicyMaximumRetryCount = cr.getHostConfig().getRestartPolicy().getMaximumRetryCount();
        this.restartPolicy = new HashMap<>(Map.of(
                        "name", restartPolicyName,
                        "maximumRetryCount", restartPolicyMaximumRetryCount
        ));
        if (cr.getHostConfig().getNanoCPUs() != null) {
            this.nanoCPUs = (float) cr.getHostConfig().getNanoCPUs() / 1000000000;
        }
        if (cr.getHostConfig().getMemory() != null) {
            this.memory = cr.getHostConfig().getMemory() / 1024 / 1024;
        }

        // 使用Set来存储唯一的挂载卷信息
        Set<VolumeHelper> volumeSet = new HashSet<>();

        Bind[] binds = cr.getHostConfig().getBinds();
        if (binds != null) {
            Arrays.stream(binds).forEach(b -> {
                if (b.getPath() == null) {
                    return;
                }
                VolumeHelper vh = new VolumeHelper();
                vh.setHostPath(b.getPath());  // 宿主机路径
                vh.setContainerPath(b.getVolume().getPath()); // 容器内路径
                vh.setMode(b.getAccessMode().name()); // 访问模式，默认为 r
                volumeSet.add(vh);
            });
        }

        List<InspectContainerResponse.Mount> mounts = cr.getMounts();
        if (mounts != null) {
            mounts.forEach(m -> {
                if (m.getName() == null) {
                    return;
                }
                VolumeHelper vh = new VolumeHelper();
                vh.setHostPath(m.getName());
                vh.setContainerPath(Objects.requireNonNull(m.getDestination()).getPath());
                vh.setMode(Boolean.TRUE.equals(m.getRW()) ? "rw" : "ro");
                volumeSet.add(vh);
            });
        }

        this.volumes = volumeSet.toArray(VolumeHelper[]::new);

        this.workingDir = cr.getConfig().getWorkingDir();

        // 处理 created 时间
        if (cr.getCreated() != null) {
            Instant createdInstant = Instant.parse(cr.getCreated());
            this.created = createdInstant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
        } else {
            this.created = "";
        }

        // 处理 startedAt 时间
        startedAt = cr.getState().getStartedAt();
        if (startedAt != null) {
            Instant startedAtInstant = Instant.parse(startedAt);
            this.startedAt = startedAtInstant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
        } else {
            this.startedAt = "";
        }
    }
}
