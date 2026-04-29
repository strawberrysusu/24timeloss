package org.example.newssummaryproject.domain.news;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.ArticleReadHistoryRepository;
import org.example.newssummaryproject.domain.member.SavedArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오래된 자동수집 기사를 정리하는 스케줄러.
 *
 * 정책:
 *   - writer가 null인 기사 (자동수집)
 *   - DB 등록(createdAt) 후 retention-days 일이 지남
 *   - 누구도 저장(saved_articles)하지 않음
 *   → 위 3가지 모두 만족하면 자동삭제
 *
 * 사용자가 직접 작성한 기사는 절대 건드리지 않는다.
 * 누군가 저장해둔 기사는 영구 보존된다.
 *
 * 비활성화: ARTICLE_RETENTION_DAYS=0 (기본값) 으로 두면 스케줄러가 스킵한다.
 *
 * 한 번에 너무 많이 지우면 트랜잭션이 길어지므로 BATCH_SIZE 단위로 잘라서 반복한다.
 */
@Component
@RequiredArgsConstructor
public class ArticleRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ArticleRetentionScheduler.class);

    private static final int BATCH_SIZE = 500;

    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository articleSummaryRepository;
    private final SavedArticleRepository savedArticleRepository;
    private final ArticleReadHistoryRepository articleReadHistoryRepository;

    @Value("${article.retention-days:0}")
    private int retentionDays;

    /**
     * 매일 새벽 3시(KST)에 실행 (트래픽이 가장 적은 시간대).
     *
     * cron 6필드: 초 분 시 일 월 요일
     * "0 0 3 * * *" = 매일 03:00:00
     * zone = "Asia/Seoul" — JVM TZ가 UTC라도 한국시간 03시에 실행됨.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeOldUnsavedArticles() {
        if (retentionDays <= 0) {
            log.debug("ARTICLE_RETENTION_DAYS={} - retention 정리 비활성화", retentionDays);
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("retention 정리 시작: 임계 시각={}, 보관기간={}일", cutoff, retentionDays);

        int totalDeleted = 0;
        while (true) {
            int deleted = purgeBatch(cutoff);
            totalDeleted += deleted;
            if (deleted < BATCH_SIZE) {
                break;
            }
        }

        log.info("retention 정리 완료: 삭제 {}건", totalDeleted);
    }

    /**
     * 한 트랜잭션에서 최대 BATCH_SIZE개 기사를 정리한다.
     * 자식 행(요약/저장/읽기기록)을 먼저 지우고 마지막에 기사를 삭제한다.
     */
    @Transactional
    public int purgeBatch(LocalDateTime cutoff) {
        List<Long> articleIds = articleRepository.findOldUnsavedSystemArticleIds(
                cutoff, PageRequest.of(0, BATCH_SIZE));

        if (articleIds.isEmpty()) {
            return 0;
        }

        for (Long articleId : articleIds) {
            // 자식 행 먼저 정리 (FK 제약 위반 방지)
            articleSummaryRepository.deleteByArticleId(articleId);
            articleReadHistoryRepository.deleteByArticleId(articleId);
            // saved_articles는 쿼리 조건상 0건이지만 방어적으로 호출
            savedArticleRepository.deleteByArticleId(articleId);
            articleRepository.deleteById(articleId);
        }

        return articleIds.size();
    }
}
