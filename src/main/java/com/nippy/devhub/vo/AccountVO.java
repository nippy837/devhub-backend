package com.nippy.devhub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountVO {
    // 主键
    private Long id;
    // 系统名称
    private String systemName;
    // 环境名称
    private String environment;
    // 用户名
    private String username;
    // 账号密码
    private String password;
    // 登录URL
    private String loginUrl;
    // 备注
    private String remark;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;
}
