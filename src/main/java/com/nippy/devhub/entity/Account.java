package com.nippy.devhub.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account {
    private Long id;
    private String systemName;
    private String environment;
    private String username;
    private String password;
    private String loginUrl;
    private String remark;
}
