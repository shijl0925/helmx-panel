package com.helmx.tutorial.logging.aspect;

import com.helmx.tutorial.logging.entity.SysLog;
import com.helmx.tutorial.logging.service.SysLogService;
import com.helmx.tutorial.utils.RequestHolder;
import com.helmx.tutorial.utils.SecurityUtils;
import com.helmx.tutorial.utils.StringUtils;
import com.helmx.tutorial.utils.ThrowableUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class LogAspect {

    private final SysLogService sysLogService;

    ThreadLocal<Long> currentTime = new ThreadLocal<>();

    public LogAspect(SysLogService sysLogService) {
        this.sysLogService = sysLogService;
    }

    /**
     * 配置切入点
     */
    @Pointcut("@annotation(com.helmx.tutorial.logging.annotation.Log)")
    public void logPointcut() {
        // 该方法无方法体，主要为了让同类中其他方法使用此切入点
    }

    /**
     * 配置环绕通知，使用在方法 logPointcut() 上注册的切入点
     *
     * @param joinPoint join point for advice
     */
    @Around("logPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        currentTime.set(System.currentTimeMillis());
        result = joinPoint.proceed();

        long time = System.currentTimeMillis() - currentTime.get();
        SysLog sysLog = new SysLog("INFO", time);
        currentTime.remove();

        // 在请求线程中同步提取请求数据，避免异步线程中 Servlet 容器回收请求对象导致的竞争问题
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        String requestIp = StringUtils.getIp(request);
        String userAgent = StringUtils.getUserAgent(request);
        String browser = StringUtils.getBrowser(request);

        log.info("Request method: {}, path: {}", request.getMethod(), request.getRequestURI());

        sysLogService.save(getUsername(), requestIp, userAgent, browser, joinPoint, sysLog);
        return result;
    }

    /**
     * 配置异常通知
     *
     * @param joinPoint join point for advice
     * @param e         exception
     */
    @AfterThrowing(pointcut = "logPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        Long startTime = currentTime.get();
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : 0L;
        currentTime.remove();

        SysLog sysLog = new SysLog("ERROR", elapsed);
        sysLog.setExceptionDetail(ThrowableUtil.getStackTrace(e));

        // 在请求线程中同步提取请求数据，避免异步线程中 Servlet 容器回收请求对象导致的竞争问题
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        String requestIp = StringUtils.getIp(request);
        String userAgent = StringUtils.getUserAgent(request);
        String browser = StringUtils.getBrowser(request);

        log.warn("Request method: {}, path: {}", request.getMethod(), request.getRequestURI());

        sysLogService.save(getUsername(), requestIp, userAgent, browser, joinPoint, sysLog);
    }

    /**
     * 获取用户名
     *
     * @return 当前登录用户名，获取失败返回空字符串
     */
    private String getUsername() {
        try {
            return SecurityUtils.getCurrentUsername();
        } catch (Exception e) {
            return "";
        }
    }
}
