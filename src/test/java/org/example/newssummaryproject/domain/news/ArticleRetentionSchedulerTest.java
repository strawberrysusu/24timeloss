package org.example.newssummaryproject.domain.news;

import org.example.newssummaryproject.domain.member.Member;
import org.example.newssummaryproject.domain.member.MemberRepository;
import org.example.newssummaryproject.domain.member.SavedArticle;
import org.example.newssummaryproject.domain.member.SavedArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Retention 정리 스케줄러 단위 테스트.
 *
 * @Transactional을 일부러 안 붙였다 — 스케줄러가 자체 @Transactional로 동작하므로
 * 테스트가 격리된 트랜잭션을 갖고 있으면 스케줄러의 변경 사항이 보이지 않는다.
 * 대신 각 테스트 끝에서 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ArticleRetentionSchedulerTest {

    @Autowired
    ArticleRetentionScheduler scheduler;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    ArticleSummaryRepository articleSummaryRepository;

    @Autowired
    SavedArticleRepository savedArticleRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        // 다른 테스트 흔적 제거
        savedArticleRepository.deleteAll();
        articleSummaryRepository.deleteAll();
        articleRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void retention_정리_대상_기사만_삭제된다() {
        // given
        Member user = memberRepository.save(Member.builder()
                .email("u@test.com").password("x").nickname("user").build());

        Article oldUnsavedSystem = saveArticle(null);     // 삭제 대상
        Article oldUserWritten = saveArticle(user);        // 사용자 작성 → 보존
        Article oldSavedSystem = saveArticle(null);        // 누가 저장 → 보존
        Article recentSystem = saveArticle(null);          // 최근 기사 → 보존

        // 100일 전으로 backdate (recentSystem 제외)
        backdate(oldUnsavedSystem.getId(), 100);
        backdate(oldUserWritten.getId(), 100);
        backdate(oldSavedSystem.getId(), 100);

        // oldSavedSystem은 누가 저장해놓았다고 가정
        savedArticleRepository.save(SavedArticle.builder()
                .member(user).article(oldSavedSystem).build());

        // when: 90일 컷오프로 정리
        int deleted = scheduler.purgeBatch(LocalDateTime.now().minusDays(90));

        // then
        assertThat(deleted).isEqualTo(1);
        assertThat(articleRepository.findById(oldUnsavedSystem.getId())).isEmpty();
        assertThat(articleRepository.findById(oldUserWritten.getId())).isPresent();
        assertThat(articleRepository.findById(oldSavedSystem.getId())).isPresent();
        assertThat(articleRepository.findById(recentSystem.getId())).isPresent();
    }

    @Test
    void retention_정리_시_요약도_함께_삭제된다() {
        // given
        Article systemArticle = saveArticle(null);
        backdate(systemArticle.getId(), 100);

        ArticleSummary summary = articleSummaryRepository.save(ArticleSummary.builder()
                .article(systemArticle)
                .summaryLine1("요약1").summaryLine2("요약2").summaryLine3("요약3")
                .keyPoint1("p1").keyPoint2("p2").keyPoint3("p3")
                .summarySource(SummarySource.AI_GENERATED)
                .modelName("test-model")
                .generatedAt(LocalDateTime.now())
                .build());

        // when
        scheduler.purgeBatch(LocalDateTime.now().minusDays(90));

        // then
        assertThat(articleRepository.findById(systemArticle.getId())).isEmpty();
        assertThat(articleSummaryRepository.findById(summary.getId())).isEmpty();
    }

    @Test
    void retention_정리_대상이_없으면_0_반환한다() {
        // given: 모두 최근 기사
        saveArticle(null);
        saveArticle(null);

        // when
        int deleted = scheduler.purgeBatch(LocalDateTime.now().minusDays(90));

        // then
        assertThat(deleted).isZero();
        assertThat(articleRepository.count()).isEqualTo(2);
    }

    private Article saveArticle(Member writer) {
        return articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("테스트 기사 " + System.nanoTime())
                .content("본문 내용")
                .publishedAt(LocalDateTime.now())
                .writer(writer)
                .build());
    }

    /**
     * 기사의 created_at을 N일 전으로 강제 변경한다.
     * BaseTimeEntity는 @CreatedDate라 자동으로 now()가 들어가므로
     * retention 테스트를 하려면 native SQL로 backdate가 필요하다.
     */
    private void backdate(Long articleId, int daysAgo) {
        jdbcTemplate.update("UPDATE articles SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(daysAgo), articleId);
    }
}
