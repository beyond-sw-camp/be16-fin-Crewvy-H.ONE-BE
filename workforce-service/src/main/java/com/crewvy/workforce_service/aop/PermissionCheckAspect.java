package com.crewvy.workforce_service.aop;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.feignClient.MemberClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckAspect {

    private final MemberClient memberClient;

    @Before("@annotation(com.crewvy.common.aop.CheckPermission)")
    public void checkPermission(JoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CheckPermission checkPermission = method.getAnnotation(CheckPermission.class);

        String resource = checkPermission.resource();
        String action = checkPermission.action();
        String scope = checkPermission.scope();

        UUID memberPositionId = findAuthUserArgument(joinPoint);

        if (memberPositionId == null) {
            log.warn("권한 검사 실패 - @AuthUser 누락 (메서드: {})", method.getName());
            throw new IllegalArgumentException("권한 검사를 위한 @AuthUser 파라미터가 필요합니다.");
        }

        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(
                memberPositionId, resource, action, scope
        );

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            log.warn("권한 없음: memberPositionId={}, resource={}, action={}", memberPositionId, resource, action);
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }
    }

    private UUID findAuthUserArgument(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Annotation[] annotations = parameters[i].getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == AuthUser.class) {
                    if (args[i] instanceof UUID) {
                        return (UUID) args[i];
                    }
                    log.warn("@AuthUser 타입이 UUID가 아님. (타입: {})", args[i].getClass());
                    return null;
                }
            }
        }
        return null;
    }
}
