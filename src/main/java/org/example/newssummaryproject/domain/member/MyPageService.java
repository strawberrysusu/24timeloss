package org.example.newssummaryproject.domain.member;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.MyPageResponse;
import org.example.newssummaryproject.domain.member.dto.ReadHistoryResponse;
import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.ArticleRepository;
import org.example.newssummaryproject.domain.news.Category;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        long readCount = articleReadHistoryRepository.countByMemberId(memberId);

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
                interests,
                savedArticles
        );
    }

    /**
     * 기사를 저장한다.
     */
    @Transactional
    public void saveArticle(Long memberId, Long articleId) {
        if (savedArticleRepository.existsByMemberIdAndArticleId(memberId, articleId)) {
            throw new IllegalArgumentException("이미 저장한 기사입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다."));

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
                .orElseThrow(() -> new NoSuchElementException("저장한 기사가 아닙니다."));

        savedArticleRepository.delete(savedArticle);
    }

    /**
     * 관심 카테고리를 추가한다.
     */
    @Transactional
    public void addInterest(Long memberId, Category category) {
        if (memberInterestRepository.existsByMemberIdAndCategory(memberId, category)) {
            throw new IllegalArgumentException("이미 등록된 관심 분야입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

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
                .orElseThrow(() -> new NoSuchElementException("등록되지 않은 관심 분야입니다."));

        memberInterestRepository.delete(interest);
    }

    /**
     * 기사 읽음 기록을 남긴다. 이미 읽은 기사는 중복 기록하지 않는다.
     */
    @Transactional
    public void recordRead(Long memberId, Long articleId) {
        if (articleReadHistoryRepository.existsByMemberIdAndArticleId(memberId, articleId)) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다."));

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
