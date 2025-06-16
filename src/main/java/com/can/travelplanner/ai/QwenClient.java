package com.can.travelplanner.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QwenClient extends BaseAiClient {
    
    private final String apiKey;
    private final String model;
    
    public QwenClient(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.model}") String model) {
        super(null); // We'll override the methods
        this.apiKey = apiKey;
        this.model = model;
    }
    
    @Override
    public String generateResponse(String prompt) {
        try {
            Generation gen = new Generation();
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Role.USER.getValue()).content(prompt).build());
            
            QwenParam param = QwenParam.builder()
                .model(model)
                .messages(messages)
                .apiKey(apiKey)
                .temperature(0.7f)
                .maxTokens(2000)
                .build();
                
            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response from Qwen", e);
        }
    }
} 