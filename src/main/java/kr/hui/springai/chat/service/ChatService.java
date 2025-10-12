package kr.hui.springai.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder, Advisor[] advisors) {
        this.chatClient = chatClientBuilder.defaultAdvisors(advisors).build();
    }

    /**
     *
     * @param prompt USER PROMPT, SYS PROMPT를 받을 수 있도록 Prompt 사용 (String 사용 시 USER 프롬프트만 받음)
     * @param conversationId
     * @return
     */
    public Flux<String> stream(Prompt prompt, String conversationId) {
        return buildChatClientRequestSpec(prompt, conversationId).stream().content();
    }

    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(Prompt prompt, String conversationId) {
        return chatClient.prompt(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId));
    }

    public ChatResponse call(Prompt prompt, String conversationId) {
        return buildChatClientRequestSpec(prompt, conversationId).call().chatResponse();
    }

    public enum Emotion { VERY_NEGATIVE, NEGATIVE, NEUTRAL, POSITIVE, VERY_POSITIVE }

    public record EmotionEvaluation(Emotion emotion, List<String> reason) { }

    public EmotionEvaluation callEmotionEvaluation(Prompt prompt, String conversationId) {
        return buildChatClientRequestSpec(prompt, conversationId).call().entity(EmotionEvaluation.class);
    }

}
