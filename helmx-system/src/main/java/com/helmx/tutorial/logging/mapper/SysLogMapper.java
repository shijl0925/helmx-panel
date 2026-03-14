package com.helmx.tutorial.logging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.logging.dto.SysLogQueryCriteria;
import com.helmx.tutorial.logging.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    /**
     * 查询所有日志数据
     *
     * @param criteria 查询参数
     * @return List<SysLog>
     */
    List<SysLog> queryAll(@Param("criteria") SysLogQueryCriteria criteria);

    /**
     * 分页查询日志数据
     *
     * @param criteria 查询参数
     * @param page     分页参数
     * @return Page<SysLog>
     */
    Page<SysLog> queryAll(@Param("criteria") SysLogQueryCriteria criteria, Page<SysLog> page);
}
