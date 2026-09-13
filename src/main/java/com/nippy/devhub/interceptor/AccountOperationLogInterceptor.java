package com.nippy.devhub.interceptor;

import com.nippy.devhub.controller.AccountController;
import com.nippy.devhub.entity.AccountOperationLog;
import com.nippy.devhub.service.AccountOperationLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AccountOperationLogInterceptor implements HandlerInterceptor {
    private static final String LOG_ATTRIBUTE = AccountOperationLogInterceptor.class.getName() + ".log";
    private static final String START_ATTRIBUTE = AccountOperationLogInterceptor.class.getName() + ".start";
    // 新增完成后，由 Controller 把生成的账号 ID 放进当前请求，避免再次查库。
    public static final String ACCOUNT_ID_ATTRIBUTE = AccountOperationLogInterceptor.class.getName() + ".accountId";

    @Resource
    private AccountOperationLogService operationLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只记录真正进入账号 Controller 的四类请求，不记录健康检查、静态资源或预检请求。
        if (!(handler instanceof HandlerMethod method)
                || !AccountController.class.isAssignableFrom(method.getBeanType())) {
            return true;
        }
        String operationType = operationType(request.getMethod());
        if (operationType == null || request.getAttribute(LOG_ATTRIBUTE) != null) return true;

        AccountOperationLog entry = new AccountOperationLog();
        entry.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entry.setOperationType(operationType);
        entry.setRequestMethod(request.getMethod());
        entry.setRequestPath("/api/accounts");
        entry.setCreatedTime(LocalDateTime.now(ZoneOffset.UTC));
        // 默认只使用连接方 IP，不直接信任客户端可伪造的 X-Forwarded-For。
        entry.setClientIp(request.getRemoteAddr());
        if ("UPDATE".equals(operationType) || "DELETE".equals(operationType)) {
            entry.setAccountId(parseAccountId(request.getParameter("id")));
        }
        request.setAttribute(LOG_ATTRIBUTE, entry);
        request.setAttribute(START_ATTRIBUTE, System.nanoTime());
        response.setHeader("X-Request-Id", entry.getRequestId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        Object value = request.getAttribute(LOG_ATTRIBUTE);
        if (!(value instanceof AccountOperationLog entry)) return;
        // 同一请求只写一次日志，不使用共享字段保存每次请求的数据。
        request.removeAttribute(LOG_ATTRIBUTE);
        long startedAt = (Long) request.getAttribute(START_ATTRIBUTE);
        entry.setDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        if (request.getAttribute(ACCOUNT_ID_ATTRIBUTE) instanceof Long id) entry.setAccountId(id);

        // 已被异常处理器处理的异常可能不会出现在 exception 参数里，所以还必须检查 HTTP 状态。
        int status = response.getStatus();
        if (exception != null && status < 400) status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        entry.setHttpStatus(status);
        entry.setSuccess(exception == null && status >= 200 && status < 300);
        entry.setMessage(summary(entry));

        // @Slf4j 生成 SLF4J logger；这里只输出白名单字段，不输出请求体或异常原文。
        if (entry.isSuccess()) {
            log.info("账号操作 requestId={} operation={} accountId={} status={} durationMs={}",
                    entry.getRequestId(), entry.getOperationType(), entry.getAccountId(), status, entry.getDurationMs());
        } else {
            log.warn("账号操作失败 requestId={} operation={} accountId={} status={} durationMs={}",
                    entry.getRequestId(), entry.getOperationType(), entry.getAccountId(), status, entry.getDurationMs());
        }

        try {
            // Controller 和业务事务已结束，再写日志，避免先记成功、后业务回滚。
            operationLogService.save(entry);
        } catch (RuntimeException error) {
            // 写日志失败不能改变已经完成的账号操作；不输出可能包含 SQL 参数的异常消息/堆栈。
            log.error("操作日志入库失败 requestId={} operation={} accountId={} status={} errorType={}",
                    entry.getRequestId(), entry.getOperationType(), entry.getAccountId(), status,
                    error.getClass().getSimpleName());
        }
    }

    private String operationType(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "GET" -> "QUERY";
            case "PUT" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> null;
        };
    }

    private Long parseAccountId(String value) {
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            // 非法输入仍交给 Controller 校验，日志只记有效数字，不记录原始输入。
            return null;
        }
    }

    private String summary(AccountOperationLog entry) {
        String action = switch (entry.getOperationType()) {
            case "CREATE" -> "新增账号";
            case "QUERY" -> "查询账号列表";
            case "UPDATE" -> "编辑账号";
            case "DELETE" -> "删除账号";
            default -> "账号操作";
        };
        if (entry.isSuccess()) return action + "成功";
        String reason = switch (entry.getHttpStatus()) {
            case 400 -> "请求参数不合法";
            case 404 -> "账号不存在";
            default -> "请求处理失败";
        };
        return action + "失败：" + reason;
    }
}
