package com.helmx.tutorial.security.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.helmx.tutorial.dto.Result;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * JWT认证入口点
 * 当用户尝试访问受保护的资源但未提供有效JWT令牌时，该类负责处理认证异常
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理认证异常的方法
     * 当认证失败时，返回401状态码和统一的错误响应格式
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param authException 认证异常信息
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        logger.warn("222 JWT认证失败: {}, 请求URL: {}", authException.getMessage(), request.getRequestURL());

        // 设置响应状态码为401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 返回统一的错误格式
        Result result = new Result();
        result.setMessage("222 Token已过期或无效，请重新登录");
        result.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        result.setData(null);

        try (PrintWriter out = response.getWriter()) {
            out.write(objectMapper.writeValueAsString(result));
        }
    }
}