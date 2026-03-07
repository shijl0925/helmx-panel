package com.helmx.tutorial.docker.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import org.springframework.stereotype.Component;

@Component
public class DockerHostValidator {

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
                .eq(DockerEnv::getStatus, 1);

        if (!dockerEnvMapper.exists(queryWrapper)) {
            throw new IllegalArgumentException("Access to the specified host is not allowed");
        }
    }
}
