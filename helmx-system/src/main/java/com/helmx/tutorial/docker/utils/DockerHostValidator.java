package com.helmx.tutorial.docker.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import org.springframework.stereotype.Component;

@Component
public class DockerHostValidator {

    // tb_docker_env.status = 1 means the Docker environment is enabled/active.
    private static final int ACTIVE_STATUS = 1;

    private final DockerEnvMapper dockerEnvMapper;

    public DockerHostValidator(DockerEnvMapper dockerEnvMapper) {
        this.dockerEnvMapper = dockerEnvMapper;
    }

    public void validateHostAllowlist(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Docker host must not be blank");
        }

        LambdaQueryWrapper<DockerEnv> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DockerEnv::getHost, host)
                .eq(DockerEnv::getStatus, ACTIVE_STATUS);

        if (!dockerEnvMapper.exists(queryWrapper)) {
            throw new IllegalArgumentException("Access to host [" + host + "] is not allowed");
        }
    }
}
