package com.nippy.devhub.service.impl;

import com.nippy.devhub.mapper.AccountMapper;
import com.nippy.devhub.dto.AccountCreateDTO;
import com.nippy.devhub.entity.Account;
import com.nippy.devhub.service.AccountService;
import com.nippy.devhub.vo.AccountVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;


    @Override
    public List<AccountVO> listAccounts() {
        return accountMapper.selectAll();
    }

    @Override
    public Long createAccount(AccountCreateDTO request) {
        Account account = new Account();
        account.setSystemName(request.getSystemName().trim());
        account.setEnvironment(request.getEnvironment());
        account.setUsername(request.getUsername().trim());
        // 密码中的空格可能有意义，不能 trim。
        account.setPassword(request.getPassword());
        account.setLoginUrl(request.getLoginUrl() == null ? null : request.getLoginUrl().trim());
        account.setRemark(request.getRemark());
        accountMapper.insert(account);
        return account.getId();
    }

}
