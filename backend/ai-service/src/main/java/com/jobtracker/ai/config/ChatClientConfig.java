package com.jobtracker.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a job search coach analyzing a user's job application data.
                        Be concise, specific, and actionable. Respond in English only.
                        When asked for JSON output, return ONLY valid JSON with no markdown fences or explanation.
                        """)
                .build();
    }
}
