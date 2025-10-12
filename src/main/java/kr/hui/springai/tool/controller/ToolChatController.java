package kr.hui.springai.tool.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import kr.hui.springai.tool.service.ToolChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/tool")
@RestController
public class ToolChatController {

    private final ToolChatService toolChatService;

    public record PromptBody(
        @NotEmpty @Schema(description = "대화 식별자", example = "conv-1234") String conversationId,
        @NotEmpty @Schema(description = "사용자 입력 프롬프트", example = "안녕하세요, 제주도 날씨 어때요?") String userPrompt,
        @NotEmpty @Schema(description = "시스템 프롬프트(선택)", example = "You are a helpful assistant.") String systemPrompt,
        @NotEmpty @Schema(description = "채팅 옵션(선택)", implementation = DefaultChatOptions.class) DefaultChatOptions chatOptions
    ) {}

    @PostMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse call(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return toolChatService.call(promptBuilder.build(), promptBody.conversationId);
    }

    private static Prompt.Builder getPromptBuilder(PromptBody promptBody) {
        List<Message> messages = new ArrayList<>();
        Optional.ofNullable(promptBody.systemPrompt).filter(Predicate.not(String::isBlank))
                .map(systemPrompt -> SystemMessage.builder().text(systemPrompt).build()).ifPresent(messages::add);
        messages.add(UserMessage.builder().text(promptBody.userPrompt).build());
        Prompt.Builder promptBuilder = Prompt.builder().messages(messages);
        Optional.ofNullable(promptBody.chatOptions).ifPresent(promptBuilder::chatOptions);
        return promptBuilder;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return toolChatService.stream(promptBuilder.build(), promptBody.conversationId);
    }
}
