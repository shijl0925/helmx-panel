package com.helmx.tutorial.docker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.helmx.tutorial.docker.entity.EnvType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EnvTypeMapper extends BaseMapper<EnvType> {
}
