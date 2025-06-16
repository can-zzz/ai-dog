package com.can.travelplanner.ai;

import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//@Component
//public class OpenAiClient extends BaseAiClient {
//
//    public OpenAiClient(
//            @Value("${spring.ai.openai.api-key}") String apiKey,
//            @Value("${spring.ai.openai.model}") String model,
//            @Value("${spring.ai.openai.base-url}") String baseUrl) {
//        super(createChatClient(apiKey, model, baseUrl));
//    }
//
//    private static OpenAiChatClient createChatClient(String apiKey, String model, String baseUrl) {
//        OpenAiChatOptions options = OpenAiChatOptions.builder()
//            .withModel(model)
//            .withTemperature(0.7f)
//            .withMaxTokens(2000)
//            .build();
//
//        return new OpenAiChatClient(apiKey, options);
//    }
//}