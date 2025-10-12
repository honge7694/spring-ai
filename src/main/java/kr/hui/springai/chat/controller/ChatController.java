package kr.hui.springai.chat.controller;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import kr.hui.springai.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    public record PromptBody(@NotEmpty String conversationId,
                             @NotEmpty String userPrompt,
                             @Nullable String systemPrompt,
                             DefaultChatOptions chatOptions) {}


    @PostMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse call(@RequestBody @Valid PromptBody promptBody) {
        return chatService.call(buildPrompt(promptBody), promptBody.conversationId());
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestBody @Valid PromptBody promptBody) {
        return this.chatService.stream(buildPrompt(promptBody), promptBody.conversationId());
    }

    private static Prompt buildPrompt(PromptBody promptBody) {
        List<Message> messages = new ArrayList<>();
        Optional.ofNullable(promptBody.systemPrompt()).filter(Predicate.not(String::isBlank))
                        .map(SystemMessage.builder()::text).map(SystemMessage.Builder::build).ifPresent(messages::add);
        messages.add(UserMessage.builder().text(promptBody.userPrompt()).build());
        Prompt.Builder promptBuilder = Prompt.builder().messages(messages);
        Optional.ofNullable(promptBody.chatOptions()).ifPresent(promptBuilder::chatOptions);
        return promptBuilder.build();
    }

    @PostMapping(value = "/emotion", produces = MediaType.APPLICATION_JSON_VALUE)
    ChatService.EmotionEvaluation emotion(@RequestBody @Valid PromptBody promptBody) {
        return chatService.callEmotionEvaluation(buildPrompt(promptBody), promptBody.conversationId());
    }
}
