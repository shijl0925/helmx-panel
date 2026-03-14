package com.helmx.tutorial.docker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helmx.tutorial.docker.entity.EnvType;
import com.helmx.tutorial.docker.mapper.EnvTypeMapper;
import com.helmx.tutorial.docker.service.EnvTypeService;
import org.springframework.stereotype.Service;

@Service
public class EnvTypeServiceImpl extends ServiceImpl<EnvTypeMapper, EnvType> implements EnvTypeService {
}
