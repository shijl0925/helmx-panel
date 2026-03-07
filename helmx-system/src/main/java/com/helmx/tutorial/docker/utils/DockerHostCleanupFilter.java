package com.helmx.tutorial.docker.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 清理Docker主机ThreadLocal的过滤器
 *
 * <p>确保每个HTTP请求结束后，{@link DockerClientUtil}中保存当前Docker主机的
 * ThreadLocal变量被及时清理，防止线程池中的线程复用时携带上一次请求的主机信息，
 * 导致路由到错误的Docker主机。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DockerHostCleanupFilter extends OncePerRequestFilter {

    private final DockerClientUtil dockerClientUtil;

    public DockerHostCleanupFilter(DockerClientUtil dockerClientUtil) {
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            dockerClientUtil.clearCurrentHost();
        }
    }
}
