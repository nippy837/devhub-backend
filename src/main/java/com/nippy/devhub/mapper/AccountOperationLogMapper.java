package com.nippy.devhub.mapper;

import com.nippy.devhub.entity.AccountOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountOperationLogMapper {
    // 参数绑定交给 MyBatis；操作日志只追加，不跟随账号更新或删除。
    @Insert("""
            INSERT INTO account_operation_logs
                (request_id, operation_type, account_id, request_method, request_path,
                 success, http_status, duration_ms, client_ip, message, created_time)
            VALUES
                (#{requestId}, #{operationType}, #{accountId}, #{requestMethod}, #{requestPath},
                 #{success}, #{httpStatus}, #{durationMs}, #{clientIp}, #{message}, #{createdTime})
            """)
    int insert(AccountOperationLog operationLog);
}
