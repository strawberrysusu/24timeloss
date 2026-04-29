package org.example.newssummaryproject.domain.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 네이버 뉴스 검색 API 클라이언트.
 *
 * API 키가 없으면(NAVER_CLIENT_ID 미설정) isEnabled()가 false를 반환하고,
 * 스케줄러가 실행을 건너뛴다.
 *
 * API 문서: https://developers.naver.com/docs/serviceapi/search/news/news.md
 */
@Component
public class NaverNewsCollector {

    private static final Logger log = LoggerFactory.getLogger(NaverNewsCollector.class);

    private static final String API_URL = "https://openapi.naver.com/v1/search/news.json";

    // 네이버 API pubDate 형식: "Mon, 29 Apr 2026 10:00:00 +0900"
    private static final DateTimeFormatter PUB_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final String clientId;
    private final String clientSecret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NaverNewsCollector(
            @Value("${naver.api.client-id:}") String clientId,
            @Value("${naver.api.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isEnabled() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    /**
     * 키워드로 뉴스를 검색해서 NewsItem 목록을 반환한다.
     *
     * @param query   검색 키워드 (예: "정치 국회")
     * @param display 가져올 기사 수 (최대 100)
     */
    public List<NewsItem> search(String query, int display) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_URL + "?query=" + encodedQuery
                    + "&display=" + display
                    + "&sort=date";

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Naver-Client-Id", clientId);
            conn.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("네이버 API 오류 [{}]: query={}", status, query);
                conn.disconnect();
                return List.of();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            conn.disconnect();

            return parseItems(sb.toString());

        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패: query={}, error={}", query, e.getMessage());
            return List.of();
        }
    }

    private List<NewsItem> parseItems(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode items = root.path("items");
        List<NewsItem> result = new ArrayList<>();

        for (JsonNode item : items) {
            String title = unescapeHtml(item.path("title").asText("").trim());
            String originalLink = item.path("originallink").asText("").trim();
            String naverLink = item.path("link").asText("").trim();

            // originallink 우선, 없으면 naver link 사용
            String url = !originalLink.isEmpty() ? originalLink : naverLink;
            if (url.isEmpty() || title.isEmpty()) continue;

            LocalDateTime publishedAt = parsePubDate(item.path("pubDate").asText(""));
            result.add(new NewsItem(title, url, naverLink, publishedAt));
        }

        return result;
    }

    private LocalDateTime parsePubDate(String pubDate) {
        try {
            return ZonedDateTime.parse(pubDate, PUB_DATE_FORMATTER).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String unescapeHtml(String text) {
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&apos;", "'");
    }

    public record NewsItem(
            String title,       // API 응답 제목 (본문 추출 실패 시 폴백용)
            String url,         // 원본 기사 URL (originallink 우선)
            String naverUrl,    // 네이버 뉴스 URL (원본 크롤링 실패 시 폴백용)
            LocalDateTime publishedAt
    ) {}
}
