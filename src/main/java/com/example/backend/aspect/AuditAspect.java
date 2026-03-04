package com.example.backend.aspect;

import com.example.backend.annotation.Auditable;
import com.example.backend.service.AuditLogService;
import com.example.backend.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "passwordHash", "newPassword", "oldPassword", "secret", "token", "apiKey"));

    // --- Pointcuts ---

    // 1. User Management (Admin/HR)
    @Pointcut("execution(* com.example.backend.service.UserService.createUser(..)) || " +
            "execution(* com.example.backend.service.UserService.updateUser(..)) || " +
            "execution(* com.example.backend.service.UserService.updateUserStatus(..)) || " +
            "execution(* com.example.backend.service.UserService.deleteUser(..))")
    public void userServiceMethods() {
    }

    // 2a. Program & Group Management (HR)
    @Pointcut("execution(* com.example.backend.service.ProgramService.createProgram(..)) || " +
            "execution(* com.example.backend.service.ProgramService.updateProgram(..)) || " +
            "execution(* com.example.backend.service.ProgramService.updateProgramStatus(..)) || " +
            "execution(* com.example.backend.service.ProgramService.deleteProgram(..))")
    public void programServiceMethods() {
    }

    // 2b. Application Management (Intern/HR)
    @Pointcut("execution(* com.example.backend.service.ApplicationService.submit(..)) || " +
            "execution(* com.example.backend.service.ApplicationService.reviewApplication(..))")
    public void applicationServiceMethods() {
    }

    // 2c. Evaluation Management (Mentor)
    @Pointcut("execution(* com.example.backend.service.EvaluationService.create(..))")
    public void evaluationServiceMethods() {
    }

    // 2d. Leave Request Management (Intern/HR)
    @Pointcut("execution(* com.example.backend.service.LeaveRequestService.createLeaveRequest(..)) || " +
            "execution(* com.example.backend.service.LeaveRequestService.approveLeaveRequest(..)) || " +
            "execution(* com.example.backend.service.LeaveRequestService.rejectLeaveRequest(..))")
    public void leaveRequestServiceMethods() {
    }

    // 2. Task Management (Mentor/Intern)
    @Pointcut("execution(* com.example.backend.service.TaskService.createTask(..)) || " +
            "execution(* com.example.backend.service.TaskService.updateTaskProgress(..)) || " +
            "execution(* com.example.backend.service.TaskService.update(..)) || " +
            "execution(* com.example.backend.service.TaskService.updateStatus(..)) || " +
            "execution(* com.example.backend.service.TaskService.deleteTask(..))")
    public void taskServiceMethods() {
    }

    // 3. Weekly Report (Intern/Mentor)
    @Pointcut("execution(* com.example.backend.service.WeeklyReportService.internSubmit(..)) || " +
            "execution(* com.example.backend.service.WeeklyReportService.mentorReview(..)) || " +
            "execution(* com.example.backend.service.WeeklyReportService.updateStatus(..))")
    public void reportServiceMethods() {
    }

    // 4. Profile Management (Intern/Mentor)
    // 4. Profile Management (Intern/Mentor)
    @Pointcut("execution(* com.example.backend.service.InternProfileService.updateIntern(..)) || " +
            "execution(* com.example.backend.service.InternProfileService.updateMyProfile(..)) || " +
            "execution(* com.example.backend.service.MentorService.updateMentor(..))")
    public void profileServiceMethods() {
    }

    // 2e. Contract Management (HR/Intern)
    @Pointcut("execution(* com.example.backend.service.ContractService.uploadContract(..)) || " +
            "execution(* com.example.backend.service.ContractService.signContract(..))")
    public void contractServiceMethods() {
    }

    // 2f. Document Management (Intern/HR)
    @Pointcut("execution(* com.example.backend.service.InternDocumentService.uploadForIntern(..)) || " +
            "execution(* com.example.backend.service.InternDocumentService.approve(..)) || " +
            "execution(* com.example.backend.service.InternDocumentService.reject(..)) || " +
            "execution(* com.example.backend.service.InternDocumentService.confirmContract(..))")
    public void documentServiceMethods() {
    }

    // 2g. Support Ticket (Any)
    @Pointcut("execution(* com.example.backend.service.SupportTicketService.createTicket(..)) || " +
            "execution(* com.example.backend.service.SupportTicketService.updateTicketStatus(..)) || " +
            "execution(* com.example.backend.service.SupportTicketService.addComment(..))")
    public void ticketServiceMethods() {
    }

    @Pointcut("@annotation(com.example.backend.annotation.Auditable)")
    public void auditableAnnotation() {
    }

    // --- Advice ---

    @AfterReturning(pointcut = "userServiceMethods() || taskServiceMethods() || reportServiceMethods() || profileServiceMethods() || "
            +
            "programServiceMethods() || applicationServiceMethods() || evaluationServiceMethods() || leaveRequestServiceMethods() || "
            +
            "contractServiceMethods() || documentServiceMethods() || ticketServiceMethods() || auditableAnnotation()", returning = "result")
    public void logAfterMethod(JoinPoint joinPoint, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return; // System action or unauthenticated
            }

            // Get Actor Info
            Long actorId = null;
            String actorEmail = auth.getName();

            if (auth.getPrincipal() instanceof CustomUserDetails) {
                actorId = ((CustomUserDetails) auth.getPrincipal()).getId();
            }

            String className = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();

            // Determine Action & Message
            String action = determineAction(joinPoint);
            String entityType = determineEntityType(className);
            Long entityId = extractEntityId(args, result);
            String message = String.format("User %s performed %s on %s (ID: %s)", actorEmail, action, entityType,
                    entityId);

            String beforeJson = trySerializeArgs(args);
            String afterJson = trySerializeResult(result);
            String diff = determineDiff(args, result);

            // Get Request Context Metadata
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            String ipAddress = null;
            String userAgent = null;
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
            }

            // Sync log (conceptually, it calls @Async service method)
            log.debug("Recording audit log for {} on {}", action, entityType);
            String finalMessage = message + (diff != null ? " | Changes: " + diff : "");
            auditLogService.createAuditLog(actorId, actorEmail, action, entityType, entityId, "SUCCESS", finalMessage,
                    beforeJson, afterJson, ipAddress, userAgent);

        } catch (Exception e) {
            log.error("Failed to record audit log", e);
        }
    }

    private String determineAction(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();

        // Check for @Auditable annotation
        if (joinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);
            if (auditable != null && !auditable.action().isEmpty()) {
                return auditable.action();
            }
        }

        if (methodName.startsWith("create"))
            return "CREATE";
        if (methodName.startsWith("update"))
            return "UPDATE";
        if (methodName.startsWith("delete"))
            return "DELETE";
        if (methodName.startsWith("submit"))
            return "SUBMIT";
        return methodName.toUpperCase();
    }

    private String determineEntityType(String className) {
        if (className.contains("UserService"))
            return "USER";
        if (className.contains("TaskService"))
            return "TASK";
        if (className.contains("WeeklyReportService"))
            return "REPORT";
        if (className.contains("InternService"))
            return "INTERN_PROFILE";
        if (className.contains("MentorService"))
            return "MENTOR_PROFILE";
        if (className.contains("ProgramService"))
            return "PROGRAM";
        if (className.contains("ApplicationService"))
            return "APPLICATION";
        if (className.contains("EvaluationService"))
            return "EVALUATION";
        if (className.contains("LeaveRequestService"))
            return "LEAVE_REQUEST";
        if (className.contains("ContractService"))
            return "CONTRACT";
        if (className.contains("InternDocumentService"))
            return "DOCUMENT";
        if (className.contains("SupportTicketService"))
            return "TICKET";
        return "UNKNOWN";
    }

    private Long extractEntityId(Object[] args, Object result) {
        // Strategy 1: If result is DTO having ID (e.g. UserResponse, TaskResponse)
        if (result != null) {
            try {
                java.lang.reflect.Method getId = result.getClass().getMethod("getId");
                return (Long) getId.invoke(result);
            } catch (Exception ignored) {
            }
        }

        // Strategy 2: First argument is Long (update/delete usually starts with ID)
        if (args != null && args.length > 0 && args[0] instanceof Long) {
            return (Long) args[0];
        }

        return null;
    }

    private String trySerializeArgs(Object[] args) {
        try {
            if (args == null || args.length == 0)
                return null;

            List<Object> filteredArgs = Arrays.stream(args)
                    .map(this::redactSensitiveData)
                    .collect(Collectors.toList());

            return objectMapper.writeValueAsString(filteredArgs);
        } catch (Exception e) {
            log.warn("Failed to serialize audit args", e);
            return "[Serialization Error]";
        }
    }

    private String trySerializeResult(Object result) {
        try {
            if (result == null)
                return null;
            Object redactedResult = redactSensitiveData(result);
            return objectMapper.writeValueAsString(redactedResult);
        } catch (Exception e) {
            log.warn("Failed to serialize audit result", e);
            return "[Serialization Error]";
        }
    }

    private Object redactSensitiveData(Object obj) {
        if (obj == null)
            return null;
        try {
            // If it's a Map, filter keys
            if (obj instanceof Map) {
                Map<Object, Object> map = new HashMap<>((Map<?, ?>) obj);
                for (String key : SENSITIVE_FIELDS) {
                    if (map.containsKey(key)) {
                        map.put(key, "[REDACTED]");
                    }
                }
                return map;
            }

            // If it's a DTO/Entity, use reflection or Convert to Map and filter
            // Simplest approach: convert to Map, redact, then return map
            Map<String, Object> map = objectMapper.convertValue(obj,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
            boolean changed = false;
            for (String key : SENSITIVE_FIELDS) {
                if (map.containsKey(key)) {
                    map.put(key, "[REDACTED]");
                    changed = true;
                }
            }
            return changed ? map : obj;
        } catch (Exception e) {
            return obj; // Return original if conversion fails
        }
    }

    private String determineDiff(Object[] args, Object result) {
        if (args == null || args.length == 0 || result == null)
            return null;
        try {
            // Very simple diffing: compare first argument (often the request DTO) with
            // result
            Object requestDto = args[0];
            Map<String, Object> beforeMap = objectMapper.convertValue(requestDto,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> afterMap = objectMapper.convertValue(result,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });

            StringBuilder diffBuilder = new StringBuilder();
            beforeMap.forEach((k, v) -> {
                if (afterMap.containsKey(k) && !Objects.equals(v, afterMap.get(k)) && !SENSITIVE_FIELDS.contains(k)) {
                    diffBuilder.append(String.format("[%s: %s -> %s] ", k, v, afterMap.get(k)));
                }
            });
            return diffBuilder.length() > 0 ? diffBuilder.toString().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
