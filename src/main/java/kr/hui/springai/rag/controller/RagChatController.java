package kr.hui.springai.rag.controller;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import kr.hui.springai.rag.service.RagChatService;
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
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    public record RagPromptBody(@NotEmpty String conversationId,
                                @NotEmpty String userPrompt,
                                @Nullable String systemPrompt,
                                @Nullable DefaultChatOptions chatOptions,
                                @Nullable String filterExpression) {}


    @PostMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse call(@RequestBody @Valid RagPromptBody ragPromptBody) {
        return ragChatService.call(
                buildPrompt(ragPromptBody),
                ragPromptBody.conversationId(),
                Optional.ofNullable(ragPromptBody.filterExpression())
        );
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestBody @Valid RagPromptBody ragPromptBody) {
        return this.ragChatService.stream(
                buildPrompt(ragPromptBody),
                ragPromptBody.conversationId(),
                Optional.ofNullable(ragPromptBody.filterExpression())
        );
    }

    private static Prompt buildPrompt(RagPromptBody ragPromptBody) {
        List<Message> messages = new ArrayList<>();
        Optional.ofNullable(ragPromptBody.systemPrompt()).filter(Predicate.not(String::isBlank))
                        .map(SystemMessage.builder()::text).map(SystemMessage.Builder::build).ifPresent(messages::add);
        messages.add(UserMessage.builder().text(ragPromptBody.userPrompt()).build());
        Prompt.Builder promptBuilder = Prompt.builder().messages(messages);
        Optional.ofNullable(ragPromptBody.chatOptions()).ifPresent(promptBuilder::chatOptions);
        return promptBuilder.build();
    }
}
