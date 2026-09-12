package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.dto.AccountCreateDTO;
import com.nippy.devhub.service.AccountService;
import com.nippy.devhub.vo.AccountVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Long> createAccount(@Valid @RequestBody AccountCreateDTO request) {
        return Result.success(accountService.createAccount(request));
    }
}
