package com.can.travelplanner.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiClientFactory {
    
    private final QwenClient qwenClient;
    private final OpenAiClient openAiClient;
    
    @Autowired
    public AiClientFactory(QwenClient qwenClient, OpenAiClient openAiClient) {
        this.qwenClient = qwenClient;
        this.openAiClient = openAiClient;
    }
    
    public BaseAiClient getClient(String modelType) {
        return switch (modelType.toLowerCase()) {
            case "qwen" -> qwenClient;
            case "openai" -> openAiClient;
            default -> throw new IllegalArgumentException("Unsupported model type: " + modelType);
        };
    }
} 