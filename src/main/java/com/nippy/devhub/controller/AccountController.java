package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.dto.*;
import com.nippy.devhub.service.AccountService;
import com.nippy.devhub.vo.AccountPageVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RestController
public class AccountController {
    @Resource
    private AccountService accountService;

    @GetMapping("/accounts")
    public Result<AccountPageVO> listAccounts(@Valid @ModelAttribute AccountQueryDTO query) {
        return Result.success(accountService.listAccounts(query));
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<Long> createAccount(@Valid @RequestBody AccountCreateDTO request) {
        return Result.success(accountService.createAccount(request));
    }

    // ID 使用查询参数，例如 PUT /api/accounts?id=10；账号内容仍放在 JSON 请求体中。
    @PutMapping("/accounts")
    public Result<Void> updateAccount(@RequestParam("id") @Positive Long id,
                                      @Valid @RequestBody AccountUpdateDTO request) {
        accountService.updateAccount(id, request);
        return Result.success(null);
    }

    // 删除同样使用查询参数，例如 DELETE /api/accounts?id=10。
    @DeleteMapping("/accounts")
    public Result<Void> deleteAccount(@RequestParam("id") @Positive Long id) {
        accountService.deleteAccount(id);
        return Result.success(null);
    }
}
