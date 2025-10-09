package kr.hui.springai.config;

import ch.qos.logback.classic.LoggerContext;
import kr.hui.springai.service.RagChatService;
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
public class RagChatConfig {

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

    /**
     * 애플리케이션 시작 시 대화형 CLI 챗봇을 실행하는 CommandLineRunner Bean을 생성합니다.
     * 'app.cli.enabled=true'일 때만 활성화됩니다.
     * 이 Runner는 사용자로부터 입력을 받고, RagChatService를 통해 응답을 스트리밍하여 콘솔에 출력하는
     * 무한 루프를 실행합니다.
     *
     * @param applicationName 애플리케이션 이름
     * @param ragChatService RAG 챗 서비스를 제공하는 서비스
     * @param filterExpression RAG 검색 시 사용할 메타데이터 필터 표현식
     * @return CommandLineRunner 인스턴스
     */
    @ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true")
    @Bean
    public CommandLineRunner cli(@Value("${spring.application.name}") String applicationName,
                                 RagChatService ragChatService,
                                 @Value("${app.cli.filter-expression:}") String filterExpression) {
        return args -> {
            // 아래 코드를 주석하면 Elasticsearch 등 외부 시스템 연동 시 발생하는 상세 에러를 확인할 수 있습니다.
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " CLI CHAT BOT");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUser: ");
                    String userMessage = scanner.nextLine();
                    ragChatService.stream(Prompt.builder().content(userMessage).build(),
                                    "cli",
                                    // filterExpression이 비어 있을 경우에만 Optional 값을 채워 전달하려는 의도의 코드
                                    Optional.ofNullable(filterExpression).filter(String::isBlank))
                            .doFirst(() -> System.out.print("\nAssistant: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            // 스트림이 완료(complete) 신호를 보낼 때까지 현재 스레드를 차단(block)합니다.
                            .blockLast();
                }
            }
        };
    }
}
