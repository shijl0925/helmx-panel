package com.helmx.tutorial.docker.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helmx.tutorial.docker.entity.Stack;
import com.helmx.tutorial.docker.mapper.StackMapper;
import com.helmx.tutorial.docker.service.StackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StackServiceImpl extends ServiceImpl<StackMapper, Stack> implements StackService {

    @Autowired
    private StackMapper stackMapper;
}