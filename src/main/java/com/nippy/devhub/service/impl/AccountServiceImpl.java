package com.nippy.devhub.service.impl;

import com.nippy.devhub.mapper.AccountMapper;
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


}
