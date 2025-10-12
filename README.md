**English** | [中文](./README.zh-CN.md)

# Helmx Panel - Enterprise Docker Container Management Platform

Helmx Panel is a comprehensive container orchestration and management solution designed for DevOps teams and system administrators, providing full-stack container management capabilities from infrastructure to application layer.

## Core Features

### 1. Multi-dimensional Container Lifecycle Management
- **Fine-grained Container Configuration**: Supports complete `Docker` container configuration options including port mapping, network configuration, volume mounts, resource limits, environment variables, health checks, and security policies
- **Dynamic Container Operations**: Provides standard operations such as container start, stop, restart, pause, and resume, along with container snapshot (`commit`) functionality
- **Rolling Update Mechanism**: Implements zero-downtime application updates through container backup and replacement
- **Batch Operations Support**: Enables batch operations and management of multiple containers

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

### 4. Network Virtualization Management
- **Software-defined Networking**: Support for creating and managing custom networks for secure container communication
- **Multi-protocol Support**: Complete support for IPv4 and IPv6 dual-stack network configuration
- **Network Isolation**: Container network isolation through network namespaces
- **Service Discovery Integration**: Built-in DNS service supporting container service discovery

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

## ScreenShots

<table>
    <tr>
        <td><img alt="Login" src="https://raw.githubusercontent.com/shijl0925/helmx-panel/refs/heads/main/screenshot/Login.png"/></td>
        <td><img alt="Docker-Overview" src="https://raw.githubusercontent.com/shijl0925/helmx-panel/refs/heads/main/screenshot/Docker-Overview.png"/></td>
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
