package com.tizhen.core.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 选手 LLM 凭证配置装配。app 与 worker 共用同一份配置。
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {
}
