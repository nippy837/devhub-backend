package com.nippy.devhub.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
// 新增与编辑共用字段规则，避免两套校验逐渐不一致。
public abstract class AccountWriteDTO {

    @NotBlank(message = "请输入系统名称")
    @Size(max = 100, message = "系统名称不能超过100个字符")
    private String systemName;

    @NotBlank(message = "请选择环境")
    @Pattern(regexp = "dev|test|prod", message = "环境只能是开发、测试或生产环境")
    private String environment;

    @NotBlank(message = "请输入用户名")
    @Size(max = 100, message = "用户名不能超过100个字符")
    private String username;

    @Size(max = 4096, message = "密码不能超过4096个字符")
    private String password;

    @Size(max = 500, message = "登录地址不能超过500个字符")
    private String loginUrl;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @AssertTrue(message = "登录地址必须是有效的 http:// 或 https:// 地址")
    public boolean isLoginUrlValid() {
        if (loginUrl == null || loginUrl.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(loginUrl.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
