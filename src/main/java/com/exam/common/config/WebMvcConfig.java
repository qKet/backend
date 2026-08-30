package com.exam.common.config;

import com.exam.common.interceptor.AdminAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// AdminAccessInterceptor를 실제 요청 처리 흐름에 등록하는 설정 — @Component로 빈만 등록해서는
// 안 걸리고, 어떤 경로에 적용할지 여기서 명시해야 동작함.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAccessInterceptor adminAccessInterceptor;

    public WebMvcConfig(AdminAccessInterceptor adminAccessInterceptor) {
        this.adminAccessInterceptor = adminAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAccessInterceptor)
                .addPathPatterns("/admin/**", "/manage/**");
    }
}
