package org.example.newssummaryproject.domain.member;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.MyPageResponse;
import org.example.newssummaryproject.domain.member.dto.ReadHistoryResponse;
import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.ArticleRepository;
import org.example.newssummaryproject.domain.news.Category;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;
import org.example.newssummaryproject.global.exception.DuplicateException;
import org.example.newssummaryproject.global.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/*
 * ── 마이페이지 서비스 ──
 *
 * 흐름: MyPageController → ★ MyPageService → 여러 Repository → DB
 *
 * 마이페이지에 필요한 기능들을 모아둔 서비스다:
 *   - 종합 정보 조회 (닉네임, 읽은 수, 스트릭, 관심분야, 저장기사)
 *   - 기사 저장/해제 (북마크)
 *   - 관심 카테고리 추가/삭제
 *   - 읽기 기록 저장/조회
 *
 * "연속 방문일(streak)" 계산이 이 서비스의 독특한 로직이다.
 * 오늘부터 과거로 거슬러 올라가며 연속으로 기사를 읽은 날수를 센다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final ArticleRepository articleRepository;
    private final ArticleReadHistoryRepository articleReadHistoryRepository;
    private final MemberInterestRepository memberInterestRepository;
    private final SavedArticleRepository savedArticleRepository;

    /**
     * 마이페이지 종합 정보를 조회한다.
     */
    public MyPageResponse getMyPage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        // 읽은 고유 기사 수 (같은 기사를 다른 날에 다시 읽어도 1로 센다)
        long readCount = articleReadHistoryRepository.countDistinctArticlesByMemberId(memberId);

        int streak = calculateStreak(memberId);

        List<Category> interests = memberInterestRepository.findByMemberId(memberId)
                .stream()
                .map(MemberInterest::getCategory)
                .toList();

        List<ArticleListResponse> savedArticles = savedArticleRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(sa -> ArticleListResponse.from(sa.getArticle()))
                .toList();

        return new MyPageResponse(
                member.getNickname(),
                member.getEmail(),
                readCount,
                streak,
                interests,
                savedArticles
        );
    }

    /**
     * 연속 방문일(streak)을 계산한다.
     *
     * 알고리즘 설명 (예시로 이해하기):
     *   읽기 기록 날짜: 3/19, 3/18, 3/17, 3/15 (3/16이 빠짐)
     *   오늘이 3/19라면:
     *     3/19 있음 → streak = 1
     *     3/18 있음 → streak = 2
     *     3/17 있음 → streak = 3
     *     3/16 없음 → 멈춤! → 결과: 3일 연속
     *
     *   만약 오늘(3/19) 아직 기사를 안 읽었다면:
     *     어제(3/18)부터 확인 시작 → 3/18 있음 → streak = 1 → ...
     *     어제도 없으면 → streak = 0
     */
    private int calculateStreak(Long memberId) {
        List<ArticleReadHistory> histories = articleReadHistoryRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId);

        if (histories.isEmpty()) return 0;

        // Set에 날짜를 모으면 중복이 제거되고, contains() 조회가 O(1)로 빠르다
        java.util.Set<LocalDate> readDates = histories.stream()
                .map(h -> h.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;

        // 오늘 기록이 없으면 어제부터 확인 (아직 오늘 기사를 안 읽은 경우)
        if (!readDates.contains(today)) {
            checkDate = today.minusDays(1);
            if (!readDates.contains(checkDate)) return 0;
        }

        // 과거로 한 날씩 거슬러 올라가며 연속 기록을 센다
        int streak = 0;
        while (readDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    /**
     * 회원이 저장한 기사 ID 목록을 반환한다. (프론트에서 저장 버튼 상태 표시용)
     */
    public List<Long> getSavedArticleIds(Long memberId) {
        return savedArticleRepository.findArticleIdsByMemberId(memberId);
    }

    /**
     * 기사를 저장한다.
     */
    @Transactional
    public void saveArticle(Long memberId, Long articleId) {
        if (savedArticleRepository.existsByMemberIdAndArticleId(memberId, articleId)) {
            throw new DuplicateException("이미 저장한 기사입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다."));

        savedArticleRepository.save(SavedArticle.builder()
                .member(member)
                .article(article)
                .build());
    }

    /**
     * 기사 저장을 취소한다.
     */
    @Transactional
    public void unsaveArticle(Long memberId, Long articleId) {
        SavedArticle savedArticle = savedArticleRepository
                .findByMemberIdAndArticleId(memberId, articleId)
                .orElseThrow(() -> new NotFoundException("저장한 기사가 아닙니다."));

        savedArticleRepository.delete(savedArticle);
    }

    /**
     * 관심 카테고리를 추가한다.
     */
    @Transactional
    public void addInterest(Long memberId, Category category) {
        if (memberInterestRepository.existsByMemberIdAndCategory(memberId, category)) {
            throw new DuplicateException("이미 등록된 관심 분야입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        memberInterestRepository.save(MemberInterest.builder()
                .member(member)
                .category(category)
                .build());
    }

    /**
     * 관심 카테고리를 삭제한다.
     */
    @Transactional
    public void removeInterest(Long memberId, Category category) {
        MemberInterest interest = memberInterestRepository
                .findByMemberIdAndCategory(memberId, category)
                .orElseThrow(() -> new NotFoundException("등록되지 않은 관심 분야입니다."));

        memberInterestRepository.delete(interest);
    }

    /**
     * 기사 읽음 기록을 남긴다.
     * 같은 기사를 같은 날에 다시 읽으면 중복 기록하지 않지만,
     * 다른 날에 다시 읽으면 새 기록을 남겨서 streak 계산이 정확하게 동작한다.
     */
    @Transactional
    public void recordRead(Long memberId, Long articleId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        if (articleReadHistoryRepository.existsByMemberIdAndArticleIdAndDate(
                memberId, articleId, startOfDay, endOfDay)) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다."));

        articleReadHistoryRepository.save(ArticleReadHistory.builder()
                .member(member)
                .article(article)
                .build());
    }

    /**
     * 읽은 기사 목록을 최신순으로 조회한다.
     */
    public List<ReadHistoryResponse> getReadHistory(Long memberId) {
        return articleReadHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReadHistoryResponse::from)
                .toList();
    }
}
