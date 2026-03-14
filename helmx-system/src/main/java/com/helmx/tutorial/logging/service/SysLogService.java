package com.helmx.tutorial.logging.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.helmx.tutorial.dto.PageResult;
import com.helmx.tutorial.logging.dto.SysLogQueryCriteria;
import com.helmx.tutorial.logging.entity.SysLog;
import org.aspectj.lang.JoinPoint;

import java.util.List;

public interface SysLogService extends IService<SysLog> {

    /**
     * 分页查询日志
     *
     * @param criteria 查询条件
     * @param page     分页参数
     * @return /
     */
    PageResult<SysLog> queryAll(SysLogQueryCriteria criteria, Page<SysLog> page);

    /**
     * 查询全部日志数据
     *
     * @param criteria 查询条件
     * @return /
     */
    List<SysLog> queryAll(SysLogQueryCriteria criteria);

    /**
     * 保存日志数据
     *
     * @param username   操作用户名
     * @param requestIp  请求IP地址
     * @param userAgent  请求UserAgent
     * @param browser    浏览器信息
     * @param joinPoint  切入点
     * @param sysLog     日志实体
     */
    void save(String username, String requestIp, String userAgent, String browser, JoinPoint joinPoint, SysLog sysLog);
}
