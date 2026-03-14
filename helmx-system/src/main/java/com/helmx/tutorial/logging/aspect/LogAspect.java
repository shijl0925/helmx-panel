package com.helmx.tutorial.logging.aspect;

import com.helmx.tutorial.logging.annotation.Log;
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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Component
@Aspect
@Slf4j
public class LogAspect {

    private final SysLogService sysLogService;

    ThreadLocal<Long> currentTime = new ThreadLocal<>();

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAM_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

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

        // 解析 resourceName SpEL 表达式
        sysLog.setResourceName(resolveResourceName(joinPoint));

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

        // 解析 resourceName SpEL 表达式
        sysLog.setResourceName(resolveResourceName(joinPoint));

        sysLogService.save(getUsername(), requestIp, userAgent, browser, joinPoint, sysLog);
    }

    /**
     * 解析 @Log 注解上 resourceName 属性中的 SpEL 表达式，返回资源名称字符串。
     * 若表达式为空或解析失败，则返回 null。
     *
     * @param joinPoint 切入点
     * @return 解析出的资源名称，或 null
     */
    private String resolveResourceName(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log aopLog = method.getAnnotation(Log.class);

        if (aopLog == null || aopLog.resourceName().isBlank()) {
            return null;
        }

        try {
            // 构建 SpEL 评估上下文，将方法参数按参数名注入
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = PARAM_NAME_DISCOVERER.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            } else {
                // 兜底：使用反射中的参数名（需要 -parameters 编译选项）
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    context.setVariable(parameters[i].getName(), args[i]);
                }
            }

            Expression expression = SPEL_PARSER.parseExpression(aopLog.resourceName());
            Object value = expression.getValue(context);
            if (value == null) {
                return null;
            }
            if (value instanceof Object[] arr) {
                return java.util.Arrays.stream(arr)
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(", "));
            }
            if (value instanceof java.util.Collection<?> col) {
                return col.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(", "));
            }
            return String.valueOf(value);
        } catch (Exception e) {
            log.warn("Failed to evaluate resourceName SpEL expression '{}': {}", aopLog.resourceName(), e.getMessage());
            return null;
        }
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
