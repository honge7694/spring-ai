package kr.hui.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * RAG(Retrieval-Augmented Generation) 기반의 채팅 기능을 제공하는 서비스 클래스입니다.
 * Spring AI의 ChatClient를 사용하여 LLM과의 상호작용을 처리하며,
 * 스트리밍 및 일반 호출 방식을 모두 지원합니다.
 */
@Service
public class RagChatService {

    private final ChatClient chatClient;

    /**
     * RagChatService의 생성자입니다.
     * 주입된 ChatClient.Builder와 Advisor들을 사용하여 ChatClient 인스턴스를 생성하고 설정합니다.
     * - defaultOptions: 모든 채팅 요청에 기본적으로 적용될 옵션(예: temperature=0.0)을 설정합니다.
     * - defaultAdvisors: RAG, 채팅 메모리 등 등록된 모든 어드바이저를 ChatClient에 적용합니다.
     *
     * @param chatClientBuilder ChatClient를 생성하기 위한 빌더
     * @param advisors ChatClient의 동작을 확장하는 어드바이저 배열 (Spring이 자동으로 주입)
     */
    public RagChatService(ChatClient.Builder chatClientBuilder, Advisor[] advisors) {
        this.chatClient = chatClientBuilder.defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .defaultAdvisors(advisors).build();
    }

    /**
     * 스트리밍 방식으로 LLM의 응답을 받습니다.
     * ChatClient를 통해 받은 응답을 Flux<String> 형태로 반환하여, 토큰이 생성될 때마다 실시간으로 처리할 수 있게 합니다.
     *
     * @param prompt 사용자 입력이 포함된 Prompt 객체
     * @param conversationId 대화 기록을 관리하기 위한 대화 ID
     * @param filterExpressionAsOpt 벡터 검색 시 적용할 메타데이터 필터 (선택 사항)
     * @return 응답 토큰의 스트림을 나타내는 Flux<String>
     */
    public Flux<String> stream(Prompt prompt, String conversationId, Optional<String> filterExpressionAsOpt) {
        return buildChatClientRequestSpec(prompt, conversationId, filterExpressionAsOpt).stream().content();
    }

    /**
     * ChatClient 요청 명세(Request Specification)를 생성하는 내부 헬퍼 메서드입니다.
     * 프롬프트와 함께 대화 ID, 벡터 검색 필터와 같은 동적 파라미터를 어드바이저에 전달하도록 요청을 구성합니다.
     *
     * @param prompt 사용자 입력이 포함된 Prompt 객체
     * @param conversationId 대화 ID
     * @param filterExpressionAsOpt 벡터 검색 필터
     * @return 구성이 완료된 ChatClient.ChatClientRequestSpec
     */
    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(
            Prompt prompt,
            String conversationId,
            Optional<String> filterExpressionAsOpt) {
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId));
        filterExpressionAsOpt.ifPresent(filterExpression -> chatClientRequestSpec.advisors(advisorSpec ->
                advisorSpec.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression)));
        return chatClientRequestSpec;
    }

    /**
     * 일반적인 요청-응답 방식으로 LLM의 전체 응답을 한 번에 받습니다.
     *
     * @param prompt 사용자 입력이 포함된 Prompt 객체
     * @param conversationId 대화 기록을 관리하기 위한 대화 ID
     * @param filterExpressionAsOpt 벡터 검색 시 적용할 메타데이터 필터 (선택 사항)
     * @return LLM의 전체 응답 정보가 담긴 ChatResponse 객체
     */
    public ChatResponse call(Prompt prompt, String conversationId, Optional<String> filterExpressionAsOpt) {
        return buildChatClientRequestSpec(prompt, conversationId, filterExpressionAsOpt).call().chatResponse();
    }
}
