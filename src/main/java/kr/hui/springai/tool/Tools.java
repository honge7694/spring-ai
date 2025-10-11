package kr.hui.springai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class Tools {

    private final WebClient webClient;

    public Tools(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Tool(name = "getWeather", description = "지역 이름을 받아서 날씨를 조회합니다.")
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
}

