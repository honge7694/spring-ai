package kr.hui.springai.tool.config;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 도구(Tool) 관련 설정을 담당하는 클래스입니다.
 * 이 클래스는 AI 모델이 특정 작업을 수행하기 위해 호출할 수 있는 '도구'들의 동작 방식을 정의합니다.
 * 예를 들어, AI가 날씨를 알려주거나, 데이터베이스에서 정보를 가져오는 등의 기능을 '도구'로 만들 수 있습니다.
 * {@link Configuration} 어노테이션은 이 클래스가 스프링의 설정 정보를 담고 있음을 나타냅니다.
 */
@Configuration
public class ToolConfig {

    /**
     * AI 모델이 도구를 호출하는 과정을 관리하는 {@link ToolCallingManager}를 스프링 빈(Bean)으로 등록합니다.
     * AI가 사용자의 요청을 분석하고, 그에 맞는 도구가 필요하다고 판단했을 때,
     * 이 매니저가 적절한 도구를 찾아 실행하는 역할을 합니다.
     *
     * @return {@link ToolCallingManager}의 기본 구현체
     * {@link Bean} 어노테이션은 이 메소드가 반환하는 객체를 스프링이 관리하는 '빈(Bean)'으로 만들어줍니다.
     * 빈은 스프링 애플리케이션 전체에서 공유하고 재사용할 수 있는 객체입니다.
     */
    @Bean
    public ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder().build();
    }

    /**
     * 도구 실행 중 발생하는 예외(에러)를 처리하는 {@link ToolExecutionExceptionProcessor}를 스프링 빈으로 등록합니다.
     * AI가 도구를 사용하다가 에러가 발생했을 때, 이 프로세서가 어떻게 대응할지를 결정합니다.
     *
     * <p>
     * {@code .alwaysThrow(false)} 설정은 에러가 발생하더라도 즉시 전체 프로세스를 중단시키지 않고,
     * 에러 정보를 AI 모델에게 전달하여 다음 행동을 결정할 수 있도록 합니다.
     * 예를 들어, "죄송합니다. 도구 실행에 실패했습니다. 다른 방법으로 시도해볼까요?" 와 같이
     * AI가 유연하게 대처할 수 있게 됩니다.
     * </p>
     *
     * @return {@link DefaultToolExecutionExceptionProcessor}의 맞춤 설정된 인스턴스
     */
//    @Bean
//    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
//        return DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build();
//    }
}
