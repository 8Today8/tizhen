package com.tizhen.worker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * worker 不启用 Web 安全过滤链，但核心模块的账号服务依赖密码编码器。
 * 这里提供与 app 一致的实现，保证非 Web 进程可以完成组件装配。
 */
@Configuration
public class WorkerConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
