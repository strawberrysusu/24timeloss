package org.example.newssummaryproject.global.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 서버가 정상 실행 중인지 가장 빠르게 확인하기 위한 컨트롤러다.
 *
 * 현재는 Service 계층 없이 Controller가 바로 JSON을 반환한다.
 * 브라우저에서 localhost:8080/ 또는 localhost:8080/health 로 접속하면 된다.
 */
@RestController
public class HealthCheckController {

    /**
     * 루트 주소("/") 확인용 응답.
     * 처음 서버가 떴는지 확인할 때 가장 먼저 보는 엔드포인트다.
     */
    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "NewsSummaryProject",
                "status", "ok",
                "message", "Server is running"
        );
    }

    /**
     * 헬스 체크 전용 주소.
     * 나중에 프론트엔드나 모니터링 도구가 서버 생존 여부를 확인할 때도 쓸 수 있다.
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
