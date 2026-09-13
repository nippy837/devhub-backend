package com.nippy.devhub.config;

import com.nippy.devhub.interceptor.AccountOperationLogInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private AccountOperationLogInterceptor accountOperationLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 参数校验前开始记录，请求处理结束后统一保存，覆盖成功和失败结果。
        registry.addInterceptor(accountOperationLogInterceptor).addPathPatterns("/api/accounts");
    }
}
