package com.helmx.tutorial.docker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helmx.tutorial.docker.entity.DockerEnv;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import com.helmx.tutorial.docker.service.DockerEnvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DockerEnvServiceImpl extends ServiceImpl<DockerEnvMapper, DockerEnv> implements DockerEnvService {

    @Autowired
    private DockerEnvMapper dockerEnvMapper;
}
