package com.nippy.devhub.service;

import com.nippy.devhub.dto.AccountCreateDTO;
import com.nippy.devhub.dto.AccountQueryDTO;
import com.nippy.devhub.dto.AccountUpdateDTO;
import com.nippy.devhub.vo.AccountPageVO;

public interface AccountService {
    AccountPageVO listAccounts(AccountQueryDTO query);

    Long createAccount(AccountCreateDTO request);

    void updateAccount(Long id, AccountUpdateDTO request);

    void deleteAccount(Long id);
}
