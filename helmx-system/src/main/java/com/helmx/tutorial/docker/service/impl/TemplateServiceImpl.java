package com.helmx.tutorial.docker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helmx.tutorial.docker.entity.Template;
import com.helmx.tutorial.docker.mapper.TemplateMapper;
import com.helmx.tutorial.docker.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {

    @Autowired
    private TemplateMapper templateMapper;
}