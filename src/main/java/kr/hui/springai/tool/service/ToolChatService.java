package kr.hui.springai.tool.service;

import kr.hui.springai.tool.config.Tools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(Prompt prompt, String conversationId) {
        return chatClient.prompt(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId));
    }

    public ChatResponse call(Prompt prompt, String conversationId) {
        return buildChatClientRequestSpec(prompt, conversationId).call().chatResponse();
    }

    public Flux<String> stream(Prompt prompt, String conversationId) {
        return buildChatClientRequestSpec(prompt, conversationId).stream().content();
    }
}
