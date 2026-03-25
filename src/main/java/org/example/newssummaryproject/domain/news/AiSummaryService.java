package org.example.newssummaryproject.domain.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.newssummaryproject.global.exception.AiSummaryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 요약 서비스 — 기사 본문을 넣으면 3줄 요약 + 핵심 포인트 3개를 반환한다.
 *
 * 흐름: ArticleService → ★ AiSummaryService → NVIDIA NIM API (Kimi)
 *
 * NVIDIA NIM의 Kimi 모델은 스트리밍(stream=true)이 필수다.
 * 응답이 한꺼번에 오지 않고 조각(chunk)으로 쪼개져서 온다.
 * 이 서비스는 조각들을 모아서 최종 JSON을 완성한다.
 *
 * API 키가 없으면(환경변수 AI_API_KEY 미설정) 가짜 요약을 반환한다.
 */
@Service
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final boolean mockEnabled;

    public AiSummaryService(
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:moonshotai/kimi-k2.5}") String model,
            @Value("${ai.base-url:https://integrate.api.nvidia.com/v1/chat/completions}") String baseUrl,
            @Value("${ai.timeout:60}") int timeoutSeconds,
            @Value("${ai.mock-enabled:false}") boolean mockEnabled) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.mockEnabled = mockEnabled;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 기사 본문을 받아서 요약 결과를 반환한다.
     * API 키가 없으면 가짜 요약을 반환한다 (개발 편의).
     */
    public SummaryResult summarize(String content) {
        if (apiKey == null || apiKey.isBlank()) {
            if (mockEnabled) {
                log.info("AI mock 모드가 활성화되어 가짜 요약을 반환합니다.");
                return fakeSummarize();
            }
            throw new AiSummaryException("AI API 키가 설정되지 않아 요약을 생성할 수 없습니다.");
        }
        return callAi(content);
    }

    /**
     * NVIDIA NIM API를 스트리밍 모드로 호출한다.
     *
     * 스트리밍이란?
     *   일반 API: 응답 전체가 한 번에 온다  → {"choices":[{"message":{"content":"전체 텍스트"}}]}
     *   스트리밍: 응답이 조각으로 쪼개져 온다 → data: {"choices":[{"delta":{"content":"한"}}]}
     *                                         data: {"choices":[{"delta":{"content":"글"}}]}
     *                                         data: {"choices":[{"delta":{"content":"자"}}]}
     *                                         data: [DONE]
     *
     * 이 메서드는 조각(delta.content)들을 모아서 최종 JSON 문자열을 완성한다.
     */
    private SummaryResult callAi(String content) {
        HttpURLConnection conn = null;
        try {
            // 1. 본문 자르기 (토큰 초과 방지)
            String truncated = content.length() > 3000
                    ? content.substring(0, 3000) + "..."
                    : content;

            // 2. 요청 바디 구성
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", buildSystemPrompt()),
                    Map.of("role", "user", "content", truncated)
            ));
            body.put("temperature", 0.3);
            body.put("max_tokens", 1024);
            body.put("stream", true);  // NVIDIA NIM은 스트리밍 필수

            String jsonBody = objectMapper.writeValueAsString(body);

            // 3. HTTP 연결 설정
            conn = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);                       // 연결 타임아웃 10초
            conn.setReadTimeout(timeoutSeconds * 1000);           // 읽기 타임아웃

            // 4. 요청 전송
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // 5. 에러 응답 처리
            int status = conn.getResponseCode();
            if (status != 200) {
                String errorBody = readStream(conn.getErrorStream());
                log.error("AI API 에러 응답 [{}]: {}", status, errorBody);
                throw new AiSummaryException("AI API 호출 실패 (" + status + "): " + errorBody);
            }

            // 6. 스트리밍 응답 읽기 — 조각들을 모은다
            StringBuilder collected = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // SSE 형식: "data: {JSON}" 또는 "data: [DONE]"
                    if (!line.startsWith("data: ") || line.equals("data: [DONE]")) {
                        continue;
                    }
                    String json = line.substring(6);  // "data: " 제거
                    JsonNode chunk = objectMapper.readTree(json);

                    // delta.content에 실제 텍스트 조각이 들어있다
                    // (delta.reasoning_content는 AI의 사고 과정이므로 무시)
                    String piece = chunk.path("choices").path(0)
                            .path("delta").path("content").asText("");
                    collected.append(piece);
                }
            }

            log.info("AI 응답 수신 완료 ({}자)", collected.length());

            // 7. 모은 텍스트를 JSON으로 파싱
            return parseCollectedContent(collected.toString());

        } catch (AiSummaryException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI API 호출 실패: {}", e.getMessage(), e);
            throw new AiSummaryException("AI 연결 실패: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 스트리밍으로 모은 텍스트에서 요약 JSON을 추출한다.
     */
    private SummaryResult parseCollectedContent(String text) {
        try {
            // AI가 JSON 앞뒤에 ```json 같은 걸 붙일 수 있으므로 정리
            String cleaned = text.trim();
            if (cleaned.contains("{")) {
                cleaned = cleaned.substring(cleaned.indexOf("{"));
            }
            if (cleaned.contains("}")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("}") + 1);
            }

            JsonNode summary = objectMapper.readTree(cleaned);

            String line1 = summary.path("summaryLine1").asText("");
            String line2 = summary.path("summaryLine2").asText("");
            String line3 = summary.path("summaryLine3").asText("");
            String key1 = summary.path("keyPoint1").asText("");
            String key2 = summary.path("keyPoint2").asText("");
            String key3 = summary.path("keyPoint3").asText("");

            if (line1.isBlank() || key1.isBlank()) {
                throw new AiSummaryException("AI가 유효한 요약을 생성하지 못했습니다. 다시 시도해주세요.");
            }

            return new SummaryResult(line1, line2, line3, key1, key2, key3);

        } catch (AiSummaryException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", text, e);
            throw new AiSummaryException("AI 응답을 처리할 수 없습니다. 다시 시도해주세요.", e);
        }
    }

    /**
     * InputStream을 문자열로 읽는 헬퍼.
     */
    private String readStream(java.io.InputStream is) {
        if (is == null) return "(no body)";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "(read error)";
        }
    }

    /**
     * AI에게 보낼 시스템 프롬프트.
     */
    private String buildSystemPrompt() {
        return """
                당신은 한국어 뉴스 기사를 요약하는 전문 AI입니다.
                사용자가 보내는 기사 본문을 읽고, 아래 JSON 형식으로만 응답하세요.

                {
                  "summaryLine1": "첫 번째 요약 문장 (핵심 사실)",
                  "summaryLine2": "두 번째 요약 문장 (배경 또는 원인)",
                  "summaryLine3": "세 번째 요약 문장 (전망 또는 영향)",
                  "keyPoint1": "핵심 포인트 1 (짧은 문구)",
                  "keyPoint2": "핵심 포인트 2 (짧은 문구)",
                  "keyPoint3": "핵심 포인트 3 (짧은 문구)"
                }

                규칙:
                - 각 요약 문장은 한 줄, 50자 내외로 작성
                - 핵심 포인트는 20자 내외의 짧은 문구로 작성
                - 반드시 위 JSON 형식만 반환 (다른 텍스트 금지)
                - 한국어로 작성
                """;
    }

    /**
     * API 키 없을 때 사용하는 가짜 요약.
     */
    private SummaryResult fakeSummarize() {
        return new SummaryResult(
                "이 기사는 주요 이슈에 대해 심층적으로 다루고 있습니다.",
                "관련 전문가들의 분석과 향후 전망이 포함되어 있습니다.",
                "독자들이 알아야 할 핵심 내용을 정리하였습니다.",
                "핵심 사안에 대한 다양한 시각 제시",
                "전문가 의견을 통한 깊이 있는 분석",
                "향후 전망과 예상되는 변화 정리"
        );
    }

    /**
     * AI 요약 결과를 담는 레코드.
     */
    public record SummaryResult(
            String summaryLine1,
            String summaryLine2,
            String summaryLine3,
            String keyPoint1,
            String keyPoint2,
            String keyPoint3
    ) {}
}
