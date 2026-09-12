package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.service.AccountService;
import com.nippy.devhub.vo.AccountVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api")
@RestController
public class AccountController {

    @Resource
    private AccountService accountService;

    @GetMapping("/accounts")
    public Result<List<AccountVO>> listAccounts() {
        return Result.success(accountService.listAccounts());
    }
}
