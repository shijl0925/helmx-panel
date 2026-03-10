# Helmx Panel - 企业级Docker容器管理平台

Helmx Panel 是一个功能完整的容器编排和管理解决方案，专为DevOps团队和系统管理员设计，提供从基础设施到应用层的全栈容器管理能力。

## 核心特性

### 1. 多维度容器生命周期管理
- **精细化容器配置**：支持完整的 `Docker` 容器配置选项，包括端口映射、网络配置、卷挂载、资源限制、环境变量、健康检查、安全策略等高级功能
- **动态容器操作**：提供容器的启动、停止、重启、暂停、恢复等标准操作，以及容器快照（commit）功能
- **滚动更新机制**：通过容器备份和替换实现零停机时间的应用更新
- **批量操作支持**：支持对多个容器进行批量操作和管理
- **容器编排支持**：提供 Docker Compose 编排功能，支持复杂应用部署

### 2. 实时运维监控
- **流式日志处理**：基于SSE技术的实时日志流推送，支持大容量日志处理
- **交互式终端**：基于WebSocket的全功能TTY终端，提供原生shell体验
- **性能指标监控**：实时获取CPU、内存、网络、磁盘IO等关键性能指标
- **进程级监控**：查看容器内运行的进程列表和资源占用情况

### 3. 企业级镜像仓库管理
- **多仓库支持**：无缝集成Docker Hub、私有Registry等各类镜像仓库
- **安全认证机制**：自动处理私有仓库的认证和权限验证
- **增量构建优化**：基于Dockerfile的镜像构建，支持构建缓存和多阶段构建
- **镜像版本控制**：完善的标签管理和镜像版本追踪机制
- **镜像导入导出**：支持镜像的 TAR 文件导入导出功能
- **镜像拉取、推送**：采用异步处理机制完成镜像拉取、推送等功能

### 4. 网络虚拟化管理
- **软件定义网络**：支持创建和管理自定义网络，实现容器间安全通信
- **多协议支持**：完整支持IPv4和IPv6双栈网络配置
- **网络隔离**：通过网络命名空间实现容器网络隔离
- **服务发现集成**：内置DNS服务，支持容器服务发现
- **网络连接管理**：支持动态连接和断开容器网络

### 5. 持久化存储管理
- **卷生命周期管理**：完整的存储卷创建、使用、监控和删除流程
- **数据持久化保障**：确保容器重启或迁移时数据不丢失
- **存储驱动适配**：支持多种存储驱动和后端存储系统
- **容量监控**：实时监控存储卷使用情况和性能指标

### 6. 文件系统操作
- **双向文件传输**：支持容器与主机间的文件上传下载
- **自动格式处理**：透明处理TAR归档格式，提供直观的文件操作体验
- **大文件传输优化**：流式处理机制确保大文件传输的稳定性和效率

### 7. 多环境统一管理
- **分布式主机管理**：统一管理多个Docker主机环境
- **TLS安全连接**：企业级安全传输，支持证书认证
- **环境状态同步**：实时同步各环境的运行状态和资源配置
- **空间回收**：支持对镜像、容器、网络、卷等资源进行系统性清理

### 8. 权限与安全管理
- **RBAC权限模型**：基于角色的访问控制，细粒度权限管理
- **JWT令牌认证**：基于标准JWT的无状态认证机制
- **菜单权限控制**：动态菜单渲染和按钮级权限控制
- **用户权限管理**：角色权限分配、菜单授权
- **操作审计**：关键操作权限验证，确保系统安全

## 项目整体分析与高级功能演进建议

从当前能力看，Helmx Panel 已经具备较完整的 Docker 控制面：容器侧覆盖创建、更新、终端、日志、文件传输、性能指标；镜像侧覆盖构建、拉取、推送、导入导出、标签管理；卷和网络侧也具备增删查、连接关系和资源清理能力。下一阶段更适合往“企业治理、安全合规、可观测性、自动化策略”四个方向继续增强。

### 1. 容器管理还可以继续增强的高级能力
- **声明式发布与配置对比**：支持保存容器期望状态模板，自动对比当前运行配置与期望配置的偏差（drift detection），一键回滚或修正。
- **健康检查联动自愈**：把健康检查、重启策略、告警、自动回滚串起来，形成“异常发现 -> 自动恢复 -> 失败升级人工处理”的闭环。
- **灰度/蓝绿/金丝雀发布**：当前已经具备更新和替换基础，可进一步扩展为按批次、按权重、按标签的渐进式发布能力。
- **资源治理与容量保护**：增加 CPU/内存/IO 配额基线、资源争抢预警、超卖检测、异常容器限流，避免单个容器影响整机。
- **启动诊断与变更审计**：为容器启动失败、探针失败、环境变量变更、挂载异常等场景提供结构化诊断与操作审计。

### 2. 镜像管理还可以继续增强的高级能力
- **镜像安全扫描**：接入 CVE、恶意软件、敏感文件扫描，在拉取、构建、推送前后自动生成风险报告。
- **SBOM 与供应链签名**：生成 SBOM（软件物料清单），支持 Cosign/Notary 等签名校验，提升镜像可信度与合规能力。
- **多架构构建与缓存加速**：在现有构建能力上增加 Buildx、多架构镜像、远程缓存复用，提升 ARM/x86 混部场景适配能力。
- **镜像生命周期策略**：支持未使用镜像自动归档、标签保留策略、最近活跃版本保护、仓库清理计划任务。
- **镜像依赖与来源追踪**：展示基础镜像来源、层级变化、最近构建记录、发布链路，方便定位风险和回溯版本。

### 3. 存储卷管理还可以继续增强的高级能力
- **卷快照/备份/恢复**：支持定时备份、跨主机恢复、恢复点管理，形成真正可用的数据保护方案。
- **容量趋势分析**：不仅展示当前使用量，还要分析增长趋势、剩余可用天数、热点卷排行和突增预警。
- **卷迁移与存储分层**：支持卷在不同主机、不同驱动、不同存储层之间迁移，满足扩容和冷热数据分层需求。
- **数据一致性校验**：在备份、迁移、恢复过程中增加校验和、只读恢复演练、恢复结果比对，降低数据损坏风险。
- **卷标签化治理**：增加业务标签、环境标签、保留策略、责任人归属，便于做存储成本核算和生命周期管理。

### 4. 网络管理还可以继续增强的高级能力
- **可视化 IPAM 与子网规划**：图形化展示子网、网段冲突、网关、容器 IP 分配情况，降低网络配置复杂度。
- **网络策略与访问控制**：在 Docker 网络基础上增加南北向/东西向访问控制、白名单、黑名单、端口级策略。
- **流量观测与链路追踪**：展示容器到容器、容器到外部服务的流量、延迟、异常连接和 DNS 解析耗时。
- **跨主机网络诊断**：针对 overlay、VXLAN、MTU 不匹配、DNS 失败等问题提供一键诊断，提升排障效率。
- **网络基线与漂移告警**：对驱动、网段、容器接入关系做基线比对，发现未经审批的网络变更时主动告警。

### 5. 建议优先补齐的跨模块高级能力
- **统一事件中心**：把容器、镜像、卷、网络、主机的操作与异常统一汇总，支持搜索、订阅和告警分发。
- **任务编排与审批流**：为高风险操作（批量删除、强制移除、网络切换、镜像推送）增加审批、计划任务、变更窗口。
- **多租户/项目级隔离**：在 RBAC 之上增加项目空间、资源配额、环境隔离，更适合团队化和平台化运营。
- **成本与容量看板**：从主机、镜像、卷、网络四个维度汇总资源占用、趋势和回收收益，帮助做 FinOps 管理。
- **策略引擎**：把命名规范、镜像来源限制、端口暴露规则、卷挂载规则、安全基线做成可执行策略，而不是只靠人工约束。

### 6. 推荐实现优先级
1. **优先级 P0（最值得先做）**：镜像安全扫描、卷备份恢复、统一事件中心、资源治理告警。
2. **优先级 P1（提升平台化能力）**：灰度发布、网络策略、容量趋势分析、审批流。
3. **优先级 P2（面向大规模与合规场景）**：SBOM/签名、多租户隔离、跨主机网络诊断、策略引擎。

## 截图

<table>
    <tr>
        <td><img alt="Login" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Login.png?raw=true"/></td>
        <td><img alt="Docker-Overview" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Overview.png?raw=true"/></td>
        <td><img alt="Docker-Add-Environment" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Add-Environment.png?raw=true"/></td>
    </tr>
    <tr>
        <td><img alt="Docker-Containers-List" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Containers-List.png?raw=true"/></td>
        <td><img alt="Docker-Create-Container" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Create-Container.png?raw=true"/></td>
        <td><img alt="Docker-Update-Container" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Update-Container.png?raw=true"/></td>
    </tr>
    <tr>
        <td><img alt="Docker-Container-Details" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Container-Details2.png?raw=true"/></td>
        <td><img alt="Docker-Container-Statistics" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Container-Statistics.png?raw=true"/></td>
        <td><img alt="Docker-Container-Logs" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Container-Logs.png?raw=true"/></td>
    </tr>
    <tr>
        <td><img alt="Docker-Image-List" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Image-List.png?raw=true"/></td>
        <td><img alt="Docker-Image-Details" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Image-Details.png?raw=true"/></td>
        <td><img alt="Docker-Build-Image" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Build-Image.png?raw=true"/></td>
    </tr>
    <tr>
        <td><img alt="Docker-Volumes-List" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Volumes-List.png?raw=true"/></td>
        <td><img alt="Docker-Networks-List" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Networks-List.png?raw=true"/></td>
        <td><img alt="Docker-Network-Details" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Network-Details.png?raw=true"/></td>
    </tr>
    <tr>
        <td><img alt="Docker-Container-Terminal" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Container-Terminal.png?raw=true"/></td>
        <td><img alt="Docker-Registries" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Registries.png?raw=true"/></td>
        <td><img alt="Docker-Create-Registry" src="https://github.com/shijl0925/helmx-panel/blob/main/screenshot/Docker-Create-Registry.png?raw=true"/></td>
    </tr>
</table>

## 技术优势

### 高可用架构设计
- **异步任务处理**：长时间运行的操作（如镜像构建、拉取）采用异步处理机制
- **资源自动回收**：完善的资源清理和连接管理机制
- **故障自愈能力**：自动检测和恢复连接异常

### 安全性保障
- **JWT令牌认证**：基于标准JWT的无状态认证机制
- **权限细粒度控制**：基于角色的访问控制（RBAC）模型
- **输入参数校验**：严格的参数验证和数据过滤机制

### 性能优化
- **连接池管理**：高效的Docker客户端连接管理
- **内存优化**：大文件处理和日志流推送的内存优化
- **并发处理**：支持高并发的容器操作请求

## 适用场景

### DevOps自动化
适合需要自动化部署和管理容器化应用的DevOps团队，提供完整的CI/CD集成能力。

### 混合云管理
支持同时管理本地数据中心和云环境中的Docker主机，实现混合云统一管理。

### 微服务架构支撑
为微服务架构提供完整的容器编排和管理能力，支持服务注册发现和负载均衡。

### 开发环境管理
为开发团队提供一致的开发环境，支持快速环境搭建和销毁。

Helmx Panel 通过其专业的企业级功能和完善的API设计，为用户提供了一个功能强大、安全可靠、易于扩展的容器管理解决方案。
