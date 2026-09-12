package com.nippy.devhub.service;

import com.nippy.devhub.vo.AccountVO;
import com.nippy.devhub.dto.AccountCreateDTO;

import java.util.List;

public interface AccountService {
    List<AccountVO> listAccounts();

    Long createAccount(AccountCreateDTO request);
}
