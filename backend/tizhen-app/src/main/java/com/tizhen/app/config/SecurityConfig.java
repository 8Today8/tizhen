package com.tizhen.app.config;

import com.tizhen.app.config.security.AdminSessionAuthenticationFilter;
import com.tizhen.app.config.security.RestAccessDeniedHandler;
import com.tizhen.app.config.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * 安全基线：后台使用独立 Cookie + Redis 会话，选手端保留服务端会话配置；
 * 两条过滤链共享统一 JSON 异常响应，但 CSRF Cookie 与会话互不覆盖。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 后台状态完全由独立 Cookie + Redis 会话恢复，不创建 Servlet 会话。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            AdminSessionAuthenticationFilter adminSessionAuthenticationFilter,
            @Qualifier("adminCsrfTokenRepository") CookieCsrfTokenRepository adminCsrfTokenRepository,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .securityMatcher("/api/v1/admin/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/login").permitAll()
                        .anyRequest().authenticated())
                // SPA 从 Cookie 读取原始 Token 后放入请求头，需要非 XOR 的解析方式。
                .csrf(csrf -> csrf
                        .csrfTokenRepository(adminCsrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                // 后台身份不写入 Servlet 会话，也完全不运行会话认证策略，
                // 避免把每次请求判定为新登录并轮换 CSRF / 选手会话。
                .securityContext(context -> context
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .sessionManagement(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // 必须早于匿名认证过滤器，否则 SecurityContext 已存在匿名身份，后台 Cookie 不会被解析。
                .addFilterBefore(adminSessionAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/ws/**").authenticated()
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean("adminCsrfTokenRepository")
    public CookieCsrfTokenRepository adminCsrfTokenRepository(
            @Value("${tizhen.security.admin.csrf.cookie-name:ADMIN-XSRF-TOKEN}") String cookieName,
            @Value("${tizhen.security.admin.csrf.header-name:X-XSRF-TOKEN}") String headerName,
            @Value("${tizhen.security.admin.session.cookie-secure:false}") boolean secure) {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName(cookieName);
        repository.setHeaderName(headerName);
        repository.setCookiePath("/");
        repository.setCookieHttpOnly(false);
        repository.setCookieCustomizer(builder -> {
            builder.sameSite("Lax");
            if (secure) {
                builder.secure(true);
            }
        });
        return repository;
    }

    /**
     * 过滤器只挂在后台 SecurityFilterChain 上，禁止 Spring Boot 再次注册到 Servlet 容器。
     */
    @Bean
    public FilterRegistrationBean<AdminSessionAuthenticationFilter> adminSessionFilterRegistration(
            AdminSessionAuthenticationFilter filter) {
        FilterRegistrationBean<AdminSessionAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
