package kr.hui.springai.common.config;

import ch.qos.logback.classic.LoggerContext;
import kr.hui.springai.rag.service.RagChatService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.Scanner;

/**
 * 채팅 관련 기능과 CLI(Command Line Interface)를 위한 Spring Bean 설정을 담당합니다.
 * 이 설정 클래스는 채팅 메모리, 요청/응답 로깅, 대화형 CLI 챗봇 기능을 구성합니다.
 */
@Slf4j
@Configuration
public class CommonChatConfig {

    /**
     * ChatClient 요청과 응답을 로깅하는 간단한 어드바이저(Advisor) Bean을 생성합니다.
     * 디버깅 및 모니터링에 유용하며, 기본 로깅 포맷을 사용합니다.
     *
     * @return SimpleLoggerAdvisor 인스턴스
     */
    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder().order(0).build(); // order()를 안줘도 기본은 0
    }

    /**
     * 대화 기록을 인메모리에 저장하는 ChatMemory Bean을 생성합니다.
     * MessageWindowChatMemory는 지정된 개수(maxMessages)만큼의 최근 대화만 유지하여
     * 메모리 사용량을 관리합니다.
     *
     * @return ChatMemory 인스턴스
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(10).build();
    }

    /**
     * ChatMemory를 ChatClient와 통합하는 어드바이저 Bean을 생성합니다.
     * 이 어드바이저는 ChatClient 호출 전에 대화 기록을 프롬프트에 자동으로 주입하고,
     * 호출 후에 새로운 응답을 대화 기록에 저장하는 역할을 합니다.
     *
     * @param chatMemory 사용할 채팅 메모리
     * @return MessageChatMemoryAdvisor 인스턴스
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
