package com.nippy.devhub.service.impl;

import com.nippy.devhub.common.AccountNotFoundException;
import com.nippy.devhub.dto.*;
import com.nippy.devhub.entity.Account;
import com.nippy.devhub.mapper.AccountMapper;
import com.nippy.devhub.service.AccountService;
import com.nippy.devhub.vo.AccountPageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {
    @Resource
    private AccountMapper accountMapper;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AccountPageVO listAccounts(AccountQueryDTO query) {
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        String environment = query.getEnvironment() == null ? "" : query.getEnvironment().trim();

        int pageSize = query.getPageSize();
        long total = accountMapper.countFiltered(keyword, environment);

        // 删除末页最后一条、或者请求超大页码时，返回最后一个有效页；空列表仍为第1页。
        long lastPage = Math.max(1, (total + pageSize - 1) / pageSize);
        int page = (int) Math.min(query.getPage(), lastPage);
        long offset = (long) (page - 1) * pageSize;
        return new AccountPageVO(accountMapper.selectPage(keyword, environment, pageSize, offset),
                total, page, pageSize, accountMapper.selectStats(), accountMapper.selectEnvironments());
    }

    @Override
    public Long createAccount(AccountCreateDTO request) {
        Account account = toEntity(request);
        accountMapper.insert(account);
        return account.getId();
    }

    @Override
    @Transactional
    public void updateAccount(Long id, AccountUpdateDTO request) {
        // 在事务中锁定目标行：避免检查后被删除，也避免内容未改变时用更新行数误判不存在。
        if (accountMapper.lockById(id) == null)
            throw new AccountNotFoundException();
        Account account = toEntity(request);
        account.setId(id);
        accountMapper.update(account);
    }

    @Override
    public void deleteAccount(Long id) {
        // Mapper 只返回影响行数，Service 决定“删不到账号”这一业务含义。
        if (accountMapper.deleteById(id) == 0)
            throw new AccountNotFoundException();
    }

    private Account toEntity(AccountWriteDTO request) {
        Account account = new Account();
        account.setSystemName(request.getSystemName().trim());
        account.setEnvironment(request.getEnvironment());
        account.setUsername(request.getUsername().trim());
        // 密码中的空格可能有意义，不能 trim。
        account.setPassword(request.getPassword());
        account.setLoginUrl(request.getLoginUrl() == null ? null : request.getLoginUrl().trim());
        account.setRemark(request.getRemark());
        return account;
    }
}
