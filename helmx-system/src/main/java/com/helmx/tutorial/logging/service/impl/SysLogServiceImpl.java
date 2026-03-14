package com.helmx.tutorial.logging.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helmx.tutorial.dto.PageResult;
import com.helmx.tutorial.logging.annotation.Log;
import com.helmx.tutorial.logging.dto.SysLogQueryCriteria;
import com.helmx.tutorial.logging.entity.SysLog;
import com.helmx.tutorial.logging.mapper.SysLogMapper;
import com.helmx.tutorial.logging.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    private final SysLogMapper sysLogMapper;

    private static final String[] SENSITIVE_KEYS = {"password"};

    @Override
    public PageResult<SysLog> queryAll(SysLogQueryCriteria criteria, Page<SysLog> page) {
        Page<SysLog> result = sysLogMapper.queryAll(criteria, page);
        return new PageResult<>(result);
    }

    @Override
    public List<SysLog> queryAll(SysLogQueryCriteria criteria) {
        return sysLogMapper.queryAll(criteria);
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String username, String requestIp, String userAgent, String browser, JoinPoint joinPoint, SysLog sysLog) {
        if (sysLog == null) {
            throw new IllegalArgumentException("Log 不能为 null!");
        }

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log aopLog = method.getAnnotation(Log.class);

        // 方法路径
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        sysLog.setMethod(className + "." + methodName + "()");

        // 获取参数
        JSONObject params = getParameter(method, joinPoint.getArgs());
        sysLog.setParams(JSON.toJSONString(params));

        // 请求IP
        sysLog.setRequestIp(requestIp);

        // 用户名
        sysLog.setUsername(username);

        // 请求UA
        sysLog.setUserAgent(userAgent);

        // 浏览器
        sysLog.setBrowser(browser);

        sysLog.setDescription(aopLog.value());

        // 保存
        save(sysLog);
    }

    /**
     * 根据方法和传入的参数获取请求参数
     */
    private JSONObject getParameter(Method method, Object[] args) {
        JSONObject params = new JSONObject();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            // 过滤掉 MultipartFile
            if (args[i] instanceof MultipartFile) {
                continue;
            }
            // 过滤掉 HttpServletResponse
            if (args[i] instanceof HttpServletResponse) {
                continue;
            }
            // 过滤掉 HttpServletRequest
            if (args[i] instanceof HttpServletRequest) {
                continue;
            }
            // 将 RequestBody 注解修饰的参数作为请求参数
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null) {
                Object json = JSON.toJSON(args[i]);
                if (json instanceof JSONArray) {
                    params.put("reqBodyList", json);
                } else {
                    params.putAll((JSONObject) json);
                }
            } else {
                String key = parameters[i].getName();
                params.put(key, args[i]);
            }
        }
        // 遍历敏感字段并脱敏
        Set<String> keys = params.keySet();
        for (String key : SENSITIVE_KEYS) {
            if (keys.contains(key)) {
                params.put(key, "******");
            }
        }
        return params;
    }
}
