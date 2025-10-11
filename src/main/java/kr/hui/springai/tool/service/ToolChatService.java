package kr.hui.springai.tool.service;

import kr.hui.springai.tool.Tools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ToolChatService {

    private final ChatClient chatClient;

    public ToolChatService(
            ChatClient.Builder chatClientBuilder,
            Advisor[] advisors,
            @Value("${app.chat.default-system-prompt:}") String defaultSystemPrompt,
            Tools tools) {
        this.chatClient = chatClientBuilder.defaultSystem(defaultSystemPrompt)
                .defaultAdvisors(advisors)
                .defaultTools(tools)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(true)
                        .temperature(0.2)
                        .build())
                .build();
    }
}
