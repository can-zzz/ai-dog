package com.can.travelplanner.ai;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;

public abstract class BaseAiClient {
    protected final ChatClient chatClient;
    
    protected BaseAiClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    public String generateResponse(String prompt) {
        return chatClient.call(new Prompt(prompt)).getResult().getOutput().getContent();
    }
    
    public String generateResponse(Message message) {
        return chatClient.call(new Prompt(message)).getResult().getOutput().getContent();
    }
} 