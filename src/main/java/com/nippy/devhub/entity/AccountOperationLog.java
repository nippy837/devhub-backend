package com.nippy.devhub.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 普通 Java 类，只保存操作元数据，不复制 Account 中的账号密码等内容。
@Getter
@Setter
public class AccountOperationLog {
    private Long id;
    private String requestId;
    private String operationType;
    private Long accountId;
    private String requestMethod;
    private String requestPath;
    private boolean success;
    private int httpStatus;
    private long durationMs;
    private String clientIp;
    private String message;
    // 统一以 UTC 写入 DATETIME，避免不同服务器时区造成时间混乱。
    private LocalDateTime createdTime;
}
