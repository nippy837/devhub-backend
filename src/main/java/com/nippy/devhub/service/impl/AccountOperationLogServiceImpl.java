package com.nippy.devhub.service.impl;

import com.nippy.devhub.entity.AccountOperationLog;
import com.nippy.devhub.mapper.AccountOperationLogMapper;
import com.nippy.devhub.service.AccountOperationLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountOperationLogServiceImpl implements AccountOperationLogService {
    @Resource
    private AccountOperationLogMapper operationLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 3)
    public void save(AccountOperationLog operationLog) {
        // 独立事务保存日志：业务失败回滚以后，失败日志仍然可以提交。
        // 此方法由拦截器通过 Spring Bean 调用，保证事务注解生效。
        if (operationLogMapper.insert(operationLog) != 1) {
            throw new IllegalStateException("操作日志写入行数异常");
        }
    }
}
