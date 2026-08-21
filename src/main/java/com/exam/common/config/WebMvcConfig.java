package com.exam.common.config;

import com.exam.common.interceptor.AdminAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 2026-08-18: AdminAccessInterceptor를 실제로 요청 처리 흐름에 끼워넣는(등록하는) 설정 파일.
// 인터셉터 클래스를 만들어서 @Component로 등록만 해두면 스프링 빈으로는 존재하지만,
// "어떤 경로의 요청이 올 때 이 인터셉터를 거치게 할지"는 여기서 명시적으로 지정해야 실제로 동작함.
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
