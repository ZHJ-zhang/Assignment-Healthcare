package com.xunfang.healthcare.aspect;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.entity.SysOperationLog;
import com.xunfang.healthcare.mapper.SysOperationLogMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        String errorMsg = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            save(joinPoint, operationLog, cost, errorMsg);
        }
    }

    @Async
    protected void save(ProceedingJoinPoint joinPoint, OperationLog annotation, long cost, String errorMsg) {
        try {
            SysOperationLog entity = new SysOperationLog();
            entity.setUserId(SecurityUtils.getUserIdOrNull());
            entity.setUsername(SecurityUtils.getLoginUser().map(u -> u.getUsername()).orElse(null));
            entity.setModule(annotation.module());
            entity.setOperation(annotation.operation());
            entity.setMethod(joinPoint.getSignature().getDeclaringTypeName()
                    + "." + joinPoint.getSignature().getName());
            if (annotation.saveParams()) {
                entity.setParams(Arrays.stream(joinPoint.getArgs())
                        .map(this::safeString)
                        .collect(Collectors.joining(", ")));
            }
            HttpServletRequest request = currentRequest();
            if (request != null) {
                entity.setIp(clientIp(request));
                entity.setUserAgent(request.getHeader("User-Agent"));
            }
            entity.setStatus(errorMsg == null ? 1 : 0);
            entity.setCostTime((int) cost);
            entity.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
    }

    private String safeString(Object arg) {
        if (arg == null) {
            return "null";
        }
        String text = String.valueOf(arg);
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        return ip != null && !ip.isBlank() ? ip : request.getRemoteAddr();
    }
}
