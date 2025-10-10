package com.helmx.tutorial.security.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.helmx.tutorial.dto.Result;

import java.io.IOException;

/**
 * JWT异常处理过滤器
 * 用于捕获认证过程中抛出的异常，并返回统一格式的错误响应
 */
@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtExceptionFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 过滤器核心方法，处理每个HTTP请求
     * 捕获认证异常和其他异常，并返回相应的错误响应
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            logger.warn("认证失败: {}, 请求URL: {}", e.getMessage(), request.getRequestURL());
            handleUnauthorizedResponse(response);
        } catch (Exception e) {
            logger.error("处理请求时发生异常: {}", e.getMessage());
            handleInternalServerErrorResponse(response);
        }
    }

    /**
     * 处理认证失败的响应
     * 设置401状态码和统一格式的错误信息
     *
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    private void handleUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Result result = new Result();
        result.setMessage("The token has expired or is invalid. Please login again.");
        result.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        result.setData(null);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 处理服务器内部错误的响应
     * 设置500状态码和统一格式的错误信息
     *
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    private void handleInternalServerErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");

        Result result = new Result();
        result.setMessage("Internal server error");
        result.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        result.setData(null);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
