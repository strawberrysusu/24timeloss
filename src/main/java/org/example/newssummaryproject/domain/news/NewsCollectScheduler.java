package org.example.newssummaryproject.domain.news;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 주기적으로 네이버 뉴스를 수집해서 DB에 저장하는 스케줄러.
 *
 * 동작 흐름:
 *   앱 시작 1분 후 첫 수집 → 이후 30분마다 반복
 *   카테고리별로 최신 기사를 검색 → 기존에 없는 URL만 크롤링 → DB 저장
 *
 * API 호출 횟수: 7 카테고리 × 기사 수(기본 5개) × 48회/일 = 1,680회/일
 * 네이버 무료 한도(25,000회/일)에 훨씬 못 미친다.
 *
 * NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 환경변수가 없으면 자동으로 스킵한다.
 */
@Component
@RequiredArgsConstructor
public class NewsCollectScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectScheduler.class);

    private final NaverNewsCollector naverNewsCollector;
    private final ArticleExtractService articleExtractService;
    private final ArticleRepository articleRepository;

    @Value("${naver.news.collect-size:5}")
    private int collectSize;

    // 카테고리 → 검색 키워드 (네이버 뉴스 검색에 효과적인 쿼리로 설정)
    private static final Map<Category, String> CATEGORY_QUERIES = new LinkedHashMap<>();

    static {
        CATEGORY_QUERIES.put(Category.POLITICS, "정치 국회");
        CATEGORY_QUERIES.put(Category.ECONOMY, "경제 금융");
        CATEGORY_QUERIES.put(Category.SOCIETY, "사회 사건사고");
        CATEGORY_QUERIES.put(Category.IT_SCIENCE, "IT 인공지능");
        CATEGORY_QUERIES.put(Category.WORLD, "국제뉴스");
        CATEGORY_QUERIES.put(Category.SPORTS, "스포츠");
        CATEGORY_QUERIES.put(Category.ENTERTAINMENT, "연예");
    }

    /**
     * 앱 시작 1분 후 첫 수집, 이후 30분마다 실행.
     *
     * fixedDelay: 이전 실행이 끝난 시점부터 30분 대기 (동시 실행 방지).
     * initialDelay: 앱 시작 직후 실행하지 않고 1분 대기 (DB 초기화 여유).
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void collect() {
        if (!naverNewsCollector.isEnabled()) {
            log.debug("NAVER_CLIENT_ID 미설정 — 뉴스 자동 수집 스킵");
            return;
        }

        log.info("뉴스 자동 수집 시작 (카테고리 수: {}, 카테고리당 {}개)", CATEGORY_QUERIES.size(), collectSize);
        int totalSaved = 0;

        for (Map.Entry<Category, String> entry : CATEGORY_QUERIES.entrySet()) {
            Category category = entry.getKey();
            String query = entry.getValue();

            List<NaverNewsCollector.NewsItem> items = naverNewsCollector.search(query, collectSize);
            totalSaved += saveItems(category, items);
        }

        log.info("뉴스 자동 수집 완료: {}개 신규 저장", totalSaved);
    }

    private int saveItems(Category category, List<NaverNewsCollector.NewsItem> items) {
        int count = 0;

        for (NaverNewsCollector.NewsItem item : items) {
            // 이미 등록된 URL이면 스킵
            if (articleRepository.findByOriginalUrl(item.url()).isPresent()) {
                continue;
            }

            try {
                ArticleExtractService.ExtractResult extracted = tryExtract(item);
                if (extracted == null) continue;

                articleRepository.save(Article.builder()
                        .category(category)
                        .title(extracted.title().isBlank() ? item.title() : extracted.title())
                        .content(extracted.content())
                        .source(extracted.source())
                        .originalUrl(item.url())
                        .thumbnailUrl(extracted.thumbnailUrl())
                        .videoEmbedUrl(extracted.videoEmbedUrl())
                        .publishedAt(item.publishedAt())
                        .writer(null)  // 시스템 수집 기사는 작성자 없음
                        .build());

                count++;

            } catch (Exception e) {
                log.warn("기사 저장 실패 (스킵): url={}, error={}", item.url(), e.getMessage());
            }
        }

        return count;
    }

    /**
     * 원본 URL로 먼저 추출을 시도하고, 실패하면 네이버 URL로 재시도한다.
     * 둘 다 실패하면 null을 반환한다.
     */
    private ArticleExtractService.ExtractResult tryExtract(NaverNewsCollector.NewsItem item) {
        try {
            return articleExtractService.extract(item.url());
        } catch (Exception e) {
            log.debug("원본 URL 추출 실패, 네이버 URL로 재시도: url={}", item.url());
        }

        // 원본 URL 실패 시 네이버 URL로 폴백 (네이버 링크와 원본 링크가 다른 경우)
        if (!item.naverUrl().equals(item.url()) && !item.naverUrl().isEmpty()) {
            try {
                return articleExtractService.extract(item.naverUrl());
            } catch (Exception e) {
                log.warn("네이버 URL도 추출 실패 (스킵): naverUrl={}", item.naverUrl());
            }
        }

        return null;
    }
}
