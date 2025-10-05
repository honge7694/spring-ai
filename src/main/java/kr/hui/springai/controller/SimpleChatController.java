package kr.hui.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class SimpleChatController {

    private final ChatClient chatClient;

    public SimpleChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("ai")
    public String generation(String userPrompt) {
        return this.chatClient.prompt().user(userPrompt).call().content();
    }

    @GetMapping(value = "call", produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse call(String userPrompt) {
        return this.chatClient.prompt().user(userPrompt).call().chatResponse();
    }

    @GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(String userPrompt) {
        return this.chatClient.prompt().user(userPrompt).stream().content();
    }
}
