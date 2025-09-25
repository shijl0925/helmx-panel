package com.helmx.tutorial.docker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helmx.tutorial.docker.entity.Registry;
import com.helmx.tutorial.docker.mapper.RegistryMapper;
import com.helmx.tutorial.docker.service.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegistryServiceImpl extends ServiceImpl<RegistryMapper, Registry> implements RegistryService {

    @Autowired
    private RegistryMapper registryMapper;
}
