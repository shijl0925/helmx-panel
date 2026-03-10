**English** | [中文](./README.zh-CN.md)

# Helmx Panel - Enterprise Docker Container Management Platform

Helmx Panel is a comprehensive container orchestration and management solution designed for DevOps teams and system administrators, providing full-stack container management capabilities from infrastructure to application layer.

## Core Features

### 1. Multi-dimensional Container Lifecycle Management
- **Fine-grained Container Configuration**: Supports complete `Docker` container configuration options including port mapping, network configuration, volume mounts, resource limits, environment variables, health checks, and security policies
- **Dynamic Container Operations**: Provides standard operations such as container start, stop, restart, pause, and resume, along with container snapshot (`commit`) functionality
- **Rolling Update Mechanism**: Implements zero-downtime application updates through container backup and replacement
- **Batch Operations Support**: Enables batch operations and management of multiple containers
- **Container Orchestration Support**: Provides Docker Compose orchestration functionality to support complex application deployment

### 2. Real-time Operations Monitoring
- **Streaming Log Processing**: Real-time log streaming via SSE technology with support for large-volume log handling
- **Interactive Terminal**: Full-featured TTY terminal based on WebSocket providing native shell experience
- **Performance Metrics Monitoring**: Real-time acquisition of key performance indicators including CPU, memory, network, and disk I/O
- **Process-level Monitoring**: View process lists and resource usage within containers

### 3. Enterprise-grade Image Registry Management
- **Multi-registry Support**: Seamless integration with Docker Hub, private registries, and various image repositories
- **Secure Authentication Mechanism**: Automatic handling of private registry authentication and permission validation
- **Incremental Build Optimization**: Image building based on Dockerfile with build cache and multi-stage build support
- **Image Version Control**: Comprehensive tag management and image version tracking mechanism
- **Image Import/Export**: Supports importing and exporting images via TAR files
- **Image Pull/Pus**: Uses asynchronous processing mechanism to complete image pull and push operations

### 4. Network Virtualization Management
- **Software-defined Networking**: Support for creating and managing custom networks for secure container communication
- **Multi-protocol Support**: Complete support for IPv4 and IPv6 dual-stack network configuration
- **Network Isolation**: Container network isolation through network namespaces
- **Service Discovery Integration**: Built-in DNS service supporting container service discovery
- **Network Connection Management**: Supports dynamic connection and disconnection of container networks

### 5. Persistent Storage Management
- **Volume Lifecycle Management**: Complete storage volume creation, usage, monitoring, and deletion workflow
- **Data Persistence Assurance**: Ensures data persistence during container restarts or migrations
- **Storage Driver Adaptation**: Support for multiple storage drivers and backend storage systems
- **Capacity Monitoring**: Real-time monitoring of storage volume usage and performance metrics

### 6. File System Operations
- **Bidirectional File Transfer**: Support for file upload and download between containers and hosts
- **Automatic Format Handling**: Transparent handling of TAR archive format for intuitive file operations
- **Large File Transfer Optimization**: Streaming mechanism ensuring stability and efficiency for large file transfers

### 7. Multi-environment Unified Management
- **Distributed Host Management**: Unified management of multiple Docker host environments
- **TLS Secure Connection**: Enterprise-grade secure transmission with certificate authentication support
- **Environment State Synchronization**: Real-time synchronization of operational status and resource configurations across environments
- **Space Reclamation**: Supports systematic cleanup of resources including images, containers, networks, and volumes

### 8. Permission and Security Management
- **RBAC Permission Model**: Role-based access control with fine-grained permission management
- **JWT Token Authentication**: Stateless authentication mechanism based on standard JWT
- **Menu Permission Control**: Dynamic menu rendering and button-level permission control
- **User Permission Management**: Role permission assignment and menu authorization
- **Operation Audit**: Key operation permission verification to ensure system security

## Overall Project Analysis and Advanced Feature Roadmap

Based on the current implementation, Helmx Panel already provides a fairly complete Docker control plane: containers support create/update/terminal/logs/file transfer/metrics, images support build/pull/push/import/export/tagging, and volumes and networks already cover lifecycle operations, relationships, and resource cleanup. The next stage should focus less on basic CRUD and more on enterprise governance, security compliance, observability, and policy-driven automation.

### 1. Advanced capabilities worth adding for container management
- **Declarative release and drift detection**: save the desired state of a container as a template, compare it with the live runtime configuration, and support one-click remediation or rollback.
- **Self-healing based on health signals**: connect health checks, restart policies, alerts, and automatic rollback into a closed loop of “detect -> recover -> escalate”.
- **Canary / blue-green deployment**: build on the existing update-and-replace flow to support label-based, batch-based, or weighted progressive delivery.
- **Resource governance and host protection**: add CPU/memory/IO baselines, noisy-neighbor alerts, oversubscription detection, and emergency throttling for problematic containers.
- **Startup diagnostics and audit trails**: provide structured diagnostics for startup failures, probe failures, env changes, and mount problems, along with searchable change history.

### 2. Advanced capabilities worth adding for image management
- **Image security scanning**: integrate CVE, malware, and sensitive file scanning, with automated risk reports before and after pull/build/push.
- **SBOM and supply-chain signing**: generate software bills of materials and support signature verification with tools such as Cosign or Notary.
- **Multi-architecture builds and cache acceleration**: extend the current build flow with Buildx, multi-arch images, and remote cache reuse for mixed ARM/x86 environments.
- **Image lifecycle policies**: support retention rules, protection for recently active tags, scheduled cleanup, and optional archival of unused images.
- **Image lineage and provenance tracking**: visualize base image sources, layer changes, build history, and release provenance for traceability.

### 3. Advanced capabilities worth adding for volume management
- **Snapshot / backup / restore**: add scheduled backups, cross-host restore, and restore point management to make storage protection production-ready.
- **Capacity trend analysis**: go beyond current usage and show growth trends, estimated remaining days, hot volumes, and abnormal growth alerts.
- **Volume migration and storage tiering**: support moving data across hosts, drivers, or storage tiers for scaling and hot/cold data strategies.
- **Data consistency validation**: add checksums, read-only recovery drills, and post-restore validation to reduce the risk of silent corruption.
- **Tag-driven volume governance**: enrich volumes with business labels, environment labels, owners, and retention rules for lifecycle and cost management.

### 4. Advanced capabilities worth adding for network management
- **Visual IPAM and subnet planning**: provide graphical views of subnets, conflicts, gateways, and container IP allocation to simplify operations.
- **Network policy and access control**: add east-west and north-south policy rules, allow/deny lists, and port-level controls on top of Docker networking.
- **Traffic observability and path tracing**: expose container-to-container and container-to-service traffic, latency, anomalous connections, and DNS timing.
- **Cross-host network diagnostics**: offer one-click troubleshooting for overlay, VXLAN, MTU mismatch, and DNS resolution issues.
- **Network baseline and drift alerts**: compare drivers, CIDRs, and attachment relationships against approved baselines and alert on unapproved changes.

### 5. Cross-cutting advanced features with the highest platform value
- **Unified event center**: aggregate operations and exceptions from containers, images, volumes, networks, and hosts into one searchable stream.
- **Workflow orchestration and approvals**: add approval flows, maintenance windows, and scheduled tasks for high-risk operations.
- **Project-level isolation and quotas**: extend RBAC with project spaces, quotas, and environment isolation for team-oriented platform operations.
- **Cost and capacity dashboards**: summarize utilization, growth, and cleanup gains across hosts, images, volumes, and networks for FinOps-style decisions.
- **Policy engine**: turn naming rules, trusted image sources, exposed port constraints, mount rules, and security baselines into enforceable platform policy.

### 6. Recommended delivery priority
1. **P0 (highest immediate value)**: image security scanning, volume backup/restore, unified event center, and resource governance alerts.
2. **P1 (strong platform uplift)**: canary delivery, network policy, capacity trend analysis, and approval workflows.
3. **P2 (scale and compliance oriented)**: SBOM/signing, multi-tenant isolation, cross-host network diagnostics, and a full policy engine.

## ScreenShots

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

## Technical Advantages

### High Availability Architecture Design
- **Asynchronous Task Processing**: Long-running operations (such as image building and pulling) use asynchronous processing mechanisms
- **Resource Auto-reclamation**: Comprehensive resource cleanup and connection management mechanisms
- **Self-healing Capability**: Automatic detection and recovery of connection anomalies

### Security Assurance
- **JWT Token Authentication**: Stateless authentication mechanism based on standard JWT
- **Granular Permission Control**: Role-based access control (RBAC) model
- **Input Parameter Validation**: Strict parameter validation and data filtering mechanisms

### Performance Optimization
- **Connection Pool Management**: Efficient Docker client connection management
- **Memory Optimization**: Memory optimization for large file handling and log streaming
- **Concurrent Processing**: Support for high-concurrency container operation requests

## Application Scenarios

### DevOps Automation
Ideal for DevOps teams requiring automated deployment and management of containerized applications, providing complete CI/CD integration capabilities.

### Hybrid Cloud Management
Supports simultaneous management of Docker hosts in both on-premises data centers and cloud environments, enabling unified hybrid cloud management.

### Microservices Architecture Support
Provides complete container orchestration and management capabilities for microservices architectures, supporting service registration discovery and load balancing.

### Development Environment Management
Provides consistent development environments for development teams, supporting rapid environment setup and teardown.

Helmx Panel delivers a powerful, secure, and scalable container management solution through its professional enterprise features and comprehensive API design.
