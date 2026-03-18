package org.example.newssummaryproject.domain.news;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.MemberInterest;
import org.example.newssummaryproject.domain.member.MemberInterestRepository;
import org.example.newssummaryproject.domain.news.dto.ArticleDetailResponse;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository articleSummaryRepository;
    private final MemberInterestRepository memberInterestRepository;

    /**
     * 카테고리별 기사 목록을 최신순으로 조회한다.
     * category가 null이면 전체 기사를 반환한다.
     */
    public List<ArticleListResponse> getArticles(Category category) {
        List<Article> articles = (category != null)
                ? articleRepository.findByCategoryOrderByPublishedAtDesc(category)
                : articleRepository.findAllByOrderByPublishedAtDesc();

        return articles.stream()
                .map(ArticleListResponse::from)
                .toList();
    }

    /**
     * 기사 상세 정보를 조회한다.
     * AI 요약이 있으면 함께 내려준다.
     */
    public ArticleDetailResponse getArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다. id=" + articleId));

        ArticleSummary summary = articleSummaryRepository.findByArticleId(articleId)
                .orElse(null);

        return ArticleDetailResponse.from(article, summary);
    }

    /**
     * 키워드로 기사를 검색한다. (제목 + 본문)
     */
    public List<ArticleListResponse> search(String keyword) {
        return articleRepository.searchByKeyword(keyword)
                .stream()
                .map(ArticleListResponse::from)
                .toList();
    }

    /**
     * 회원의 관심 카테고리 기반으로 기사를 추천한다.
     * 비로그인이거나 관심 분야가 없으면 전체 최신 기사를 반환한다.
     */
    public List<ArticleListResponse> recommend(Long memberId) {
        if (memberId != null) {
            List<Category> interests = memberInterestRepository.findByMemberId(memberId)
                    .stream()
                    .map(MemberInterest::getCategory)
                    .toList();

            if (!interests.isEmpty()) {
                return articleRepository.findByCategoryInOrderByPublishedAtDesc(interests)
                        .stream()
                        .map(ArticleListResponse::from)
                        .toList();
            }
        }
        return articleRepository.findAllByOrderByPublishedAtDesc()
                .stream()
                .map(ArticleListResponse::from)
                .toList();
    }
}
