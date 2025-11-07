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
