package kr.hui.springai.tool.config;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class Tools {

    private final WebClient webClient;

    public Tools(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Tool(name = "getWeather", description = "지역 이름을 받아서 날씨를 조회합니다.", returnDirect = true)
    public String getWeather(@ToolParam(description = "지역 이름") String location) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https")
                        .host("wttr.in")
                        .path(location.replace(" ", "+"))
                        .queryParam("lang", "ko")
                        .queryParam("format", "현재+%l의+날씨는+%C+상태이며,+기온은+%t,+체감+기온은+%f,+풍속은+%W,+습도는+%h,+강수량은+%p입니다.")
                        .build())
                .retrieve()
                .bodyToMono(String.class).block();
    }

    @Tool(description = "지역 이름을 받아서 3일간의 날씨와 천문 정보 (달의 위상과 밝기 그리고 해와 달의 뜨고 지는 시각)를 조회합니다.")
    public WeatherResponse getWeatherDetails(@ToolParam(description = "지역 이름") String location) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https")
                        .host("wttr.in")
                        .path(location.replace(" ", "+"))
                        .queryParam("lang", "ko")
                        .queryParam("format", "j1") // json 타입
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class).block();
    }

    public record WeatherResponse(
        @Schema(description = "일별(3일) 예보 정보 리스트")
        List<WeatherForecast> weather
    ) {}

    // 일별 예보 (hourly 제외)
    public record WeatherForecast(
            @Schema(description = "천문 정보(일출, 일몰 등)") List<Astronomy> astronomy,
            @Schema(description = "해당 날짜(yyyy-MM-dd)") String date,
            @Schema(description = "평균 기온(섭씨)") int avgTempC,
            @Schema(description = "평균 기온(화씨)") int avgTempF,
            @Schema(description = "최고 기온(섭씨)") int maxTempC,
            @Schema(description = "최고 기온(화씨)") int maxTempF,
            @Schema(description = "최저 기온(섭씨)") int minTempC,
            @Schema(description = "최저 기온(화씨)") int minTempF,
            @Schema(description = "일조 시간(시간 단위)") double sunHour,
            @Schema(description = "적설량(센티미터)") double totalSnow_cm,
            @Schema(description = "자외선 지수") double uvIndex
    ) {}

    // 천문 정보
    public record Astronomy(
            @Schema(description = "달 밝기(%)") int moon_illumination,
            @Schema(description = "달의 위상(예: Full Moon 등)") String moon_phase,
            @Schema(description = "달 뜨는 시각") String moonrise,
            @Schema(description = "달 지는 시각") String moonset,
            @Schema(description = "해 뜨는 시각") String sunrise,
            @Schema(description = "해 지는 시각") String sunset
    ) {}
}

