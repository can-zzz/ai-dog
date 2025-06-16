package com.can.travelplanner.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.dashscope.DashScopeChatClient;
import org.springframework.ai.dashscope.DashScopeChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiConfig {
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.model}")
    private String model;
    
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    
    @Bean
    @Primary
    public ChatClient chatClient() {
        DashScopeChatOptions options = DashScopeChatOptions.builder()
            .withModel(model)
            .withTemperature(0.7f)
            .withMaxTokens(2000)
            .build();
            
        return new DashScopeChatClient(apiKey, options);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 