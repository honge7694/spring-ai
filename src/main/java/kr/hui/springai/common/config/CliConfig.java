package kr.hui.springai.common.config;

import ch.qos.logback.classic.LoggerContext;
import kr.hui.springai.rag.service.RagChatService;
import kr.hui.springai.tool.service.ToolChatService;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

@Configuration
public class CliConfig {

    @ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true")
    @Bean
    public CommandLineRunner cli(@Value("${spring.application.name}") String applicationName,
                                 RagChatService ragChatService,
                                 @Value("${app.cli.filter-expression:}") String filterExpression) {
        // RAG 서비스의 stream 메서드 호출을 람다로 캡슐화하여 헬퍼 메서드에 전달합니다.
        return createCliRunner(applicationName, "RAG", (userMessage) ->
                ragChatService.stream(Prompt.builder().content(userMessage).build(),
                        "cli",
                        Optional.ofNullable(filterExpression).filter(String::isBlank))
        );
    }

    @ConditionalOnProperty(prefix = "app.tool.cli", name = "enabled", havingValue = "true")
    @Bean
    public CommandLineRunner toolCliRunner(
            @Value("${spring.application.name}") String applicationName,
            ToolChatService toolChatService) {
        // Tool 서비스의 stream 메서드 호출(인자가 다름)을 람다로 캡슐화하여 동일한 헬퍼 메서드에 전달합니다.
        return createCliRunner(applicationName, "Tool", (userMessage) ->
                toolChatService.stream(Prompt.builder().content(userMessage).build(), "cli")
        );
    }

    /**
     * 중복되는 CLI 로직을 담고 있는 비공개 헬퍼 메서드입니다.
     *
     * @param applicationName 애플리케이션 이름
     * @param chatMode        채팅 모드 (e.g., "RAG", "Tool")
     * @param chatFunction    사용자 입력을 받아 응답 스트림을 반환하는 실제 동작
     * @return                설정이 완료된 CommandLineRunner 인스턴스
     */
    private CommandLineRunner createCliRunner(String applicationName, String chatMode, Function<String, Flux<String>> chatFunction) {
        return args -> {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " " + chatMode + " CLI CHAT BOT");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUser: ");
                    String userMessage = scanner.nextLine();

                    // 파라미터로 받은 'chatFunction'을 실행합니다.
                    // 이 함수가 RAG 서비스 호출이든 Tool 서비스 호출이든 상관없이 동일하게 동작합니다.
                    chatFunction.apply(userMessage)
                            .doFirst(() -> System.out.print("\nAssistant: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .blockLast();
                }
            }
        };
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
     *//*
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

    /**
     * Tool 사용 CLI를 위한 CommandLineRunner Bean 입니다.
     * 'app.tool.cli.enabled=true'일 때 활성화됩니다.
     *//*
    @Bean
    @ConditionalOnProperty(prefix = "app.tool.cli", name = "enabled", havingValue = "true")
    public CommandLineRunner toolCliRunner(
            @Value("${spring.application.name}") String applicationName,
            ToolChatService toolChatService) {

        // ToolChatService의 스트림 호출 로직을 '함수'로 전달합니다.
        return args -> {
            // 아래 코드를 주석하면 Elasticsearch 등 외부 시스템 연동 시 발생하는 상세 에러를 확인할 수 있습니다.
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " CLI CHAT BOT");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUser: ");
                    String userMessage = scanner.nextLine();
                    toolChatService.stream(Prompt.builder().content(userMessage).build(), "cli")
                            .doFirst(() -> System.out.print("\nAssistant: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .blockLast();
                }
            }
        };
    }*/
}
