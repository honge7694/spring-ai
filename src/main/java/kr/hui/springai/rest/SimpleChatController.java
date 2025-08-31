package kr.hui.springai.rest;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleChatController {

    private final ChatClient chatClient;

    public SimpleChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("ai")
    String generation(String userPrompt) {
        return this.chatClient.prompt().user(userPrompt).call().content();
    }
}
