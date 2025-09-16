package com.helmx.tutorial.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus配置类
 * 用于配置MybatisPlus的相关插件和功能
 */
@Configuration
@MapperScan({"com.helmx.tutorial.*.mapper"})
public class MybatisPlusConfig {

    /**
     * 配置MybatisPlus拦截器
     * 主要用于添加分页功能拦截器
     *
     * @return MybatisPlusInterceptor MybatisPlus拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页拦截器，指定数据库类型为MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
