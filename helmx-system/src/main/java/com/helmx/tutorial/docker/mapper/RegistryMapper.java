package com.helmx.tutorial.docker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.helmx.tutorial.docker.entity.Registry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistryMapper extends BaseMapper<Registry> {
}
