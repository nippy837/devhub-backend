package com.nippy.devhub.common;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("账号不存在，请刷新列表后重试");
    }
}
