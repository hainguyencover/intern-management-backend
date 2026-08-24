package com.holaho.intern.shared.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holaho.intern.security.SecurityAuditContext;
import com.holaho.intern.shared.annotation.Auditable;
import com.holaho.intern.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "passwordHash", "newPassword", "oldPassword", "secret", "token", "accessToken", "refreshToken", "apiKey", "clientSecret"));

    // --- Pointcuts ---

    @Pointcut("execution(* com.holaho.intern.service.UserService.createUser(..)) || " +
            "execution(* com.holaho.intern.service.UserService.updateUser(..)) || " +
            "execution(* com.holaho.intern.service.UserService.updateUserStatus(..)) || " +
            "execution(* com.holaho.intern.service.UserService.deleteUser(..))")
    public void userServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.ProgramService.createProgram(..)) || " +
            "execution(* com.holaho.intern.service.ProgramService.updateProgram(..)) || " +
            "execution(* com.holaho.intern.service.ProgramService.updateProgramStatus(..)) || " +
            "execution(* com.holaho.intern.service.ProgramService.deleteProgram(..))")
    public void programServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.ApplicationService.submit(..)) || " +
            "execution(* com.holaho.intern.service.ApplicationService.reviewApplication(..))")
    public void applicationServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.EvaluationService.create(..))")
    public void evaluationServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.LeaveRequestService.createLeaveRequest(..)) || " +
            "execution(* com.holaho.intern.service.LeaveRequestService.approveLeaveRequest(..)) || " +
            "execution(* com.holaho.intern.service.LeaveRequestService.rejectLeaveRequest(..))")
    public void leaveRequestServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.task.service.TaskService.createTask(..)) || " +
            "execution(* com.holaho.intern.task.service.TaskService.updateTaskProgress(..)) || " +
            "execution(* com.holaho.intern.task.service.TaskService.update(..)) || " +
            "execution(* com.holaho.intern.task.service.TaskService.updateStatus(..)) || " +
            "execution(* com.holaho.intern.task.service.TaskService.deleteTask(..))")
    public void taskServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.WeeklyReportService.internSubmit(..)) || " +
            "execution(* com.holaho.intern.service.WeeklyReportService.mentorReview(..)) || " +
            "execution(* com.holaho.intern.service.WeeklyReportService.updateStatus(..))")
    public void reportServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.intern.service.InternProfileService.updateIntern(..)) || " +
            "execution(* com.holaho.intern.intern.service.InternProfileService.updateMyProfile(..)) || " +
            "execution(* com.holaho.intern.mentor.service.MentorService.updateMentor(..))")
    public void profileServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.ContractService.uploadContract(..)) || " +
            "execution(* com.holaho.intern.service.ContractService.signContract(..))")
    public void contractServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.intern.service.InternDocumentService.uploadForIntern(..)) || " +
            "execution(* com.holaho.intern.intern.service.InternDocumentService.approve(..)) || " +
            "execution(* com.holaho.intern.intern.service.InternDocumentService.reject(..)) || " +
            "execution(* com.holaho.intern.intern.service.InternDocumentService.confirmContract(..))")
    public void documentServiceMethods() {}

    @Pointcut("execution(* com.holaho.intern.service.SupportTicketService.createTicket(..)) || " +
            "execution(* com.holaho.intern.service.SupportTicketService.updateTicketStatus(..)) || " +
            "execution(* com.holaho.intern.service.SupportTicketService.addComment(..))")
    public void ticketServiceMethods() {}

    @Pointcut("@annotation(com.holaho.intern.shared.annotation.Auditable)")
    public void auditableAnnotation() {}

    // --- Advice ---

    @AfterReturning(pointcut = "userServiceMethods() || taskServiceMethods() || reportServiceMethods() || profileServiceMethods() || "
            + "programServiceMethods() || applicationServiceMethods() || evaluationServiceMethods() || leaveRequestServiceMethods() || "
            + "contractServiceMethods() || documentServiceMethods() || ticketServiceMethods() || auditableAnnotation()", returning = "result")
    public void logAfterMethodSuccess(JoinPoint joinPoint, Object result) {
        publishAuditLog(joinPoint, result, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "userServiceMethods() || taskServiceMethods() || reportServiceMethods() || profileServiceMethods() || "
            + "programServiceMethods() || applicationServiceMethods() || evaluationServiceMethods() || leaveRequestServiceMethods() || "
            + "contractServiceMethods() || documentServiceMethods() || ticketServiceMethods() || auditableAnnotation()", throwing = "ex")
    public void logAfterMethodException(JoinPoint joinPoint, Exception ex) {
        publishAuditLog(joinPoint, null, "FAILED", ex.getMessage());
    }

    private void publishAuditLog(JoinPoint joinPoint, Object result, String resultStatus, String exceptionMessage) {
        try {
            Long actorId = SecurityAuditContext.getActorId();
            String actorUsername = SecurityAuditContext.getActorUsername();
            String actorEmail = SecurityAuditContext.getActorEmail();
            String actorRole = SecurityAuditContext.getActorRole();
            Long tenantId = SecurityAuditContext.getTenantId();
            String requestId = SecurityAuditContext.getRequestId();
            String ipAddress = SecurityAuditContext.getIpAddress();
            String userAgent = SecurityAuditContext.getUserAgent();

            String className = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();

            String action = determineAction(joinPoint);
            String resourceType = determineEntityType(joinPoint, className);
            Long entityId = extractEntityId(args, result);
            String resourceId = entityId != null ? String.valueOf(entityId) : null;

            String message = String.format("Actor %s (%s) executed %s on %s [%s] - %s",
                    actorUsername, actorRole, action, resourceType, resourceId != null ? resourceId : "N/A", resultStatus);
            if (exceptionMessage != null) {
                message += " | Error: " + exceptionMessage;
            }

            String beforeJson = trySerializeArgs(args);
            String afterJson = trySerializeResult(result);

            eventPublisher.publishEvent(AuditEvent.builder()
                    .tenantId(tenantId)
                    .actorId(actorId)
                    .actorUsername(actorUsername)
                    .actorEmail(actorEmail)
                    .actorRole(actorRole)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .entityType(resourceType)
                    .entityId(entityId)
                    .result(resultStatus)
                    .status(resultStatus)
                    .message(message)
                    .beforeJson(beforeJson)
                    .afterJson(afterJson)
                    .oldValue(beforeJson)
                    .newValue(afterJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestId(requestId)
                    .build());

        } catch (Exception e) {
            log.error("Failed to intercept and publish audit event", e);
        }
    }

    private String determineAction(JoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature signature) {
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);
            if (auditable != null && !auditable.action().isEmpty()) {
                return auditable.action();
            }
        }

        String methodName = joinPoint.getSignature().getName();
        if (methodName.startsWith("create")) return "CREATE";
        if (methodName.startsWith("update")) return "UPDATE";
        if (methodName.startsWith("delete")) return "DELETE";
        if (methodName.startsWith("submit")) return "SUBMIT";
        return methodName.toUpperCase();
    }

    private String determineEntityType(JoinPoint joinPoint, String className) {
        if (joinPoint.getSignature() instanceof MethodSignature signature) {
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);
            if (auditable != null && !auditable.resource().isEmpty()) {
                return auditable.resource();
            }
        }

        if (className.contains("UserService")) return "USER";
        if (className.contains("TaskService")) return "TASK";
        if (className.contains("WeeklyReportService")) return "REPORT";
        if (className.contains("InternService")) return "INTERN_PROFILE";
        if (className.contains("MentorService")) return "MENTOR_PROFILE";
        if (className.contains("ProgramService")) return "PROGRAM";
        if (className.contains("ApplicationService")) return "APPLICATION";
        if (className.contains("EvaluationService")) return "EVALUATION";
        if (className.contains("LeaveRequestService")) return "LEAVE_REQUEST";
        if (className.contains("ContractService")) return "CONTRACT";
        if (className.contains("InternDocumentService")) return "DOCUMENT";
        if (className.contains("SupportTicketService")) return "TICKET";
        return "UNKNOWN";
    }

    private Long extractEntityId(Object[] args, Object result) {
        if (result != null) {
            try {
                Method getId = result.getClass().getMethod("getId");
                return (Long) getId.invoke(result);
            } catch (Exception ignored) {}
        }
        if (args != null && args.length > 0 && args[0] instanceof Long longId) {
            return longId;
        }
        return null;
    }

    private String trySerializeArgs(Object[] args) {
        try {
            if (args == null || args.length == 0) return null;
            List<Object> filteredArgs = Arrays.stream(args)
                    .map(this::redactSensitiveData)
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(filteredArgs);
        } catch (Exception e) {
            return "[Serialization Error]";
        }
    }

    private String trySerializeResult(Object result) {
        try {
            if (result == null) return null;
            Object redactedResult = redactSensitiveData(result);
            return objectMapper.writeValueAsString(redactedResult);
        } catch (Exception e) {
            return "[Serialization Error]";
        }
    }

    private Object redactSensitiveData(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Map) {
                Map<Object, Object> map = new HashMap<>((Map<?, ?>) obj);
                for (String key : SENSITIVE_FIELDS) {
                    if (map.containsKey(key)) {
                        map.put(key, "[REDACTED]");
                    }
                }
                return map;
            }

            Map<String, Object> map = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
            boolean changed = false;
            for (String key : SENSITIVE_FIELDS) {
                if (map.containsKey(key)) {
                    map.put(key, "[REDACTED]");
                    changed = true;
                }
            }
            return changed ? map : obj;
        } catch (Exception e) {
            return obj;
        }
    }
}
