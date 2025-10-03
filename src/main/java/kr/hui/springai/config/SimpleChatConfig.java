package kr.hui.springai.config;

import ch.qos.logback.classic.LoggerContext;
import kr.hui.springai.service.SimpleChatService;
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

import java.util.Scanner;

@Slf4j
@Configuration
public class SimpleChatConfig {

    /* 디버깅 및 모니터링에 유용하며, 기본 로깅 포맷과 커스터마이징 기능을 지원 */
    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder().order(0).build(); // order()를 안줘도 기본은 0
    }

    /*
    * MessageWindowChatMemory를 사용해 최근 메시지를 유지하는 채팅 메모리
    * 초과 시 오래된 메시지를 순나적으로 제거하며 SystemMessage는 보관하지 않음
    * */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(10).build();
    }

    /* ChatClient 호출 전후에 chatMemory를 이용해 대화 내역을 프롬프트에 자동으로 주입하거나 응답 저장을 수행 */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @ConditionalOnProperty(prefix = "spring.application", name = "cli", havingValue = "true")
    @Bean
    public CommandLineRunner cli(@Value("${spring.application.name}") String applicationName, SimpleChatService chatService) {
        return args -> {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " CLI CHAT BOT");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUser: ");
                    String userMessage = scanner.nextLine();
                    chatService.stream(Prompt.builder().content(userMessage).build(), "cli")
                            .doFirst(() -> System.out.print("\nAssistant: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .blockLast();
                }
            }
        };
    }
}
