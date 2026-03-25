package org.example.newssummaryproject.domain.news;

import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.ArticleReadHistoryRepository;
import org.example.newssummaryproject.domain.member.Member;
import org.example.newssummaryproject.domain.member.MemberInterest;
import org.example.newssummaryproject.domain.member.MemberInterestRepository;
import org.example.newssummaryproject.domain.member.MemberRepository;
import org.example.newssummaryproject.domain.member.SavedArticleRepository;
import org.example.newssummaryproject.domain.news.dto.ArticleDetailResponse;
import org.example.newssummaryproject.domain.news.dto.ArticleListResponse;
import org.example.newssummaryproject.domain.news.dto.CreateArticleRequest;
import org.example.newssummaryproject.domain.news.dto.UpdateArticleRequest;
import org.example.newssummaryproject.global.exception.ForbiddenException;
import org.example.newssummaryproject.global.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * ── 기사 서비스 (핵심 비즈니스 로직) ──
 *
 * 흐름: ArticleController → ★ ArticleService → 여러 Repository → DB
 *
 * 이 프로젝트에서 가장 많은 로직이 들어있는 서비스다.
 * 기사 CRUD, 검색, 추천, 트렌딩, 브리핑, 인기 검색어 등 핵심 기능이 여기에 있다.
 *
 * 의존하는 Repository가 7개로 많다 — 기사가 요약/관심분야/회원/검색로그/저장/읽기기록과
 * 모두 연관되어 있기 때문이다. 실무에서는 이 정도가 되면 서비스를 분리하기도 하지만,
 * MVP 단계에서는 한 곳에 모아두는 게 흐름 파악이 쉽다.
 *
 * 어노테이션은 MemberService.java 참고 (@Service, @RequiredArgsConstructor, @Transactional).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    // 기사 관련 CRUD
    private final ArticleRepository articleRepository;
    // AI 요약 조회/저장
    private final ArticleSummaryRepository articleSummaryRepository;
    // AI 요약 생성 (현재 가짜, 나중에 진짜 AI로 교체)
    private final AiSummaryService aiSummaryService;
    // 회원의 관심 카테고리 조회 (추천 기능용)
    private final MemberInterestRepository memberInterestRepository;
    // 회원 조회 (기사 작성자 설정용)
    private final MemberRepository memberRepository;
    // 검색 키워드 로그 저장 (인기 검색어용)
    private final SearchLogRepository searchLogRepository;
    // 저장한 기사 삭제 (기사 삭제 시 연쇄 삭제용)
    private final SavedArticleRepository savedArticleRepository;
    // 읽기 기록 삭제 (기사 삭제 시 연쇄 삭제용)
    private final ArticleReadHistoryRepository articleReadHistoryRepository;

    /**
     * 카테고리별 기사 목록을 최신순으로 페이지네이션 조회한다.
     * AI 요약 미리보기(첫째 줄)도 함께 내려준다.
     */
    public Page<ArticleListResponse> getArticles(Category category, Pageable pageable) {
        Page<Article> articles = (category != null)
                ? articleRepository.findByCategoryOrderByPublishedAtDesc(category, pageable)
                : articleRepository.findAllByOrderByPublishedAtDesc(pageable);

        return withSummaryPreview(articles);
    }

    /**
     * 기사 상세 정보를 조회하고, 조회수를 1 올린다.
     *
     * 조회수가 올라가야 하므로 readOnly가 아닌 일반 @Transactional을 쓴다.
     * JPA의 dirty checking이 자동으로 UPDATE view_count 쿼리를 날려준다.
     */
    @Transactional
    public ArticleDetailResponse getArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다. id=" + articleId));

        // 조회수 증가 → 트렌딩에 반영된다
        article.incrementViewCount();

        ArticleSummary summary = articleSummaryRepository.findByArticleId(articleId)
                .orElse(null);

        return ArticleDetailResponse.from(article, summary);
    }

    /**
     * 키워드로 기사를 검색한다. (제목 + 본문)
     *
     * 빈 키워드는 허용하지 않는다 — 인기 검색어 오염 방지.
     * 검색할 때마다 검색 로그를 남겨서 "인기 검색어"를 만든다.
     */
    @Transactional
    public Page<ArticleListResponse> search(String keyword, Pageable pageable) {
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        // 검색 로그 저장 (인기 검색어 산출용)
        searchLogRepository.save(new SearchLog(trimmed));

        return withSummaryPreview(articleRepository.searchByKeyword(trimmed, pageable));
    }

    /**
     * 회원의 관심 카테고리 기반으로 기사를 추천한다.
     * 비로그인이거나 관심 분야가 없으면 전체 최신 기사를 반환한다.
     */
    public Page<ArticleListResponse> recommend(Long memberId, Pageable pageable) {
        if (memberId != null) {
            List<Category> interests = memberInterestRepository.findByMemberId(memberId)
                    .stream()
                    .map(MemberInterest::getCategory)
                    .toList();

            if (!interests.isEmpty()) {
                return withSummaryPreview(
                        articleRepository.findByCategoryInOrderByPublishedAtDesc(interests, pageable));
            }
        }
        return withSummaryPreview(articleRepository.findAllByOrderByPublishedAtDesc(pageable));
    }

    /**
     * 관련 기사를 키워드 기반 + 카테고리 기반으로 최대 5개 반환한다.
     *
     * 동작 순서:
     *   1단계: 현재 기사 제목에서 의미 있는 키워드를 뽑는다.
     *   2단계: 각 키워드로 제목이 비슷한 기사를 찾는다.
     *   3단계: 아직 5개 안 되면 같은 카테고리 기사로 채운다.
     */
    public List<ArticleListResponse> getRelatedArticles(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다. id=" + articleId));

        // 1단계: 제목에서 키워드 추출 (2글자 이상, 불용어 제외)
        List<String> keywords = extractKeywords(article.getTitle());

        // 2단계: 키워드별로 관련 기사를 찾아서 중복 없이 모은다
        Set<Long> resultIds = new LinkedHashSet<>();
        for (String keyword : keywords) {
            List<Article> matched = articleRepository.findRelatedByTitleKeyword(
                    articleId, keyword, PageRequest.of(0, 5));
            matched.forEach(a -> resultIds.add(a.getId()));
            if (resultIds.size() >= 5) break;
        }

        // 3단계: 부족하면 같은 카테고리 기사로 보충
        if (resultIds.size() < 5) {
            Page<Article> sameCategory = articleRepository.findByCategoryAndIdNotOrderByPublishedAtDesc(
                    article.getCategory(), articleId, PageRequest.of(0, 5));
            sameCategory.forEach(a -> resultIds.add(a.getId()));
        }

        // 4단계: ID로 기사를 조회해서 응답 변환
        List<Article> results = resultIds.stream()
                .limit(5)
                .map(id -> articleRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        return toListWithSummary(results);
    }

    /**
     * 트렌딩 기사 5개를 반환한다.
     *
     * 정렬 기준: 조회수(viewCount) 내림차순.
     * 조회수가 같으면 최신 기사가 먼저 온다.
     * (앱 초기에 모든 조회수가 0이면 자연스럽게 최신순이 된다.)
     */
    public List<ArticleListResponse> getTrending() {
        List<Article> articles = articleRepository.findTrending(PageRequest.of(0, 5))
                .getContent();
        return toListWithSummary(articles);
    }

    /**
     * 오늘의 AI 브리핑을 생성한다.
     *
     * 최신 기사들의 AI 요약을 자연스러운 뉴스 앵커 스타일로 조합한다.
     * AI 요약이 없는 기사는 제목을 대신 사용한다.
     */
    public Map<String, Object> getBriefing() {
        List<Article> recent = articleRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, 5))
                .getContent();

        List<Long> ids = recent.stream().map(Article::getId).toList();
        List<ArticleSummary> summaries = articleSummaryRepository.findByArticleIdIn(ids);

        // 기사ID → 요약 맵을 만든다
        Map<Long, ArticleSummary> summaryMap = summaries.stream()
                .collect(Collectors.toMap(s -> s.getArticle().getId(), s -> s));

        // 자연스러운 브리핑 텍스트 생성
        StringBuilder sb = new StringBuilder();
        sb.append("오늘의 주요 뉴스를 전해드립니다. ");

        int count = 0;
        for (Article a : recent) {
            if (count >= 3) break;  // 최대 3개 기사만 브리핑에 포함
            ArticleSummary s = summaryMap.get(a.getId());
            if (s != null) {
                sb.append(s.getSummaryLine1());
            } else {
                sb.append(a.getTitle());
            }
            // 문장이 마침표로 끝나지 않으면 추가
            String current = sb.toString().trim();
            if (!current.endsWith(".") && !current.endsWith("다.")) {
                sb.append(".");
            }
            sb.append(" ");
            count++;
        }

        sb.append("이상 NewsPick AI 브리핑이었습니다.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", sb.toString().trim());
        result.put("generatedAt", LocalDateTime.now().withMinute(0).withSecond(0).withNano(0));
        return result;
    }

    /**
     * 최근 7일간의 인기 검색어를 최대 10개 반환한다.
     *
     * 동작 원리:
     *   search_logs 테이블에서 최근 7일치 로그를 꺼내서
     *   키워드별로 GROUP BY → COUNT → 내림차순 정렬.
     *
     * 검색 로그가 없으면(앱 처음 시작) 빈 리스트가 반환된다.
     */
    public List<String> getPopularKeywords() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> results = searchLogRepository.findPopularKeywords(since);

        return results.stream()
                .limit(10)
                .map(row -> (String) row[0])
                .toList();
    }

    // ──────────────────────────────────────────────────────
    // AI 요약 생성
    // ──────────────────────────────────────────────────────

    /**
     * 기사의 AI 요약을 생성(또는 재생성)한다.
     *
     * 동작 순서:
     *   1. 기사를 조회한다 (없으면 404)
     *   2. AiSummaryService로 요약을 생성한다
     *   3. 이미 요약이 있으면 덮어쓰고, 없으면 새로 만든다
     *   4. 결과를 ArticleDetailResponse로 반환한다
     */
    @Transactional
    public ArticleDetailResponse generateSummary(Long memberId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다. id=" + articleId));

        checkSummaryGenerationPermission(article, memberId);

        // 본문이 없으면 요약할 수 없다
        if (article.getContent() == null || article.getContent().isBlank()) {
            throw new IllegalArgumentException("기사 본문이 없어서 요약을 생성할 수 없습니다.");
        }

        // AI 요약 생성
        AiSummaryService.SummaryResult result = aiSummaryService.summarize(article.getContent());

        // 출처 정보: AI가 생성했으므로 AI_GENERATED + 모델명 + 생성 시각 기록
        String modelName = aiSummaryService.getModelName();
        LocalDateTime generatedAt = LocalDateTime.now();

        // 기존 요약이 있으면 덮어쓰기, 없으면 새로 생성
        ArticleSummary summary = articleSummaryRepository.findByArticleId(articleId)
                .orElse(null);

        if (summary != null) {
            summary.update(
                    result.summaryLine1(), result.summaryLine2(), result.summaryLine3(),
                    result.keyPoint1(), result.keyPoint2(), result.keyPoint3(),
                    SummarySource.AI_GENERATED, modelName, generatedAt);
        } else {
            summary = articleSummaryRepository.save(ArticleSummary.builder()
                    .article(article)
                    .summaryLine1(result.summaryLine1())
                    .summaryLine2(result.summaryLine2())
                    .summaryLine3(result.summaryLine3())
                    .keyPoint1(result.keyPoint1())
                    .keyPoint2(result.keyPoint2())
                    .keyPoint3(result.keyPoint3())
                    .summarySource(SummarySource.AI_GENERATED)
                    .modelName(modelName)
                    .generatedAt(generatedAt)
                    .build());
        }

        return ArticleDetailResponse.from(article, summary);
    }

    // ──────────────────────────────────────────────────────
    // 기사 등록 / 삭제
    // ──────────────────────────────────────────────────────

    /**
     * 새 기사를 등록한다.
     * 로그인한 회원이 작성자(writer)로 설정된다.
     * 원문 URL이 없으면 null로 저장한다 (가짜 URL을 넣지 않는다).
     * AI 요약 필드가 함께 들어오면 요약도 같이 저장된다.
     */
    @Transactional
    public ArticleDetailResponse createArticle(Long memberId, CreateArticleRequest request) {
        Member writer = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다. id=" + memberId));

        Article article = articleRepository.save(Article.builder()
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .source(request.source() != null ? request.source() : "직접 등록")
                .originalUrl(request.originalUrl() != null && !request.originalUrl().isBlank()
                        ? request.originalUrl() : null)
                .thumbnailUrl(request.thumbnailUrl())
                .videoEmbedUrl(request.videoEmbedUrl())
                .publishedAt(LocalDateTime.now())
                .writer(writer)
                .build());

        // 요약은 기사 등록 후 "AI 요약 생성" 버튼으로만 만들 수 있다
        return ArticleDetailResponse.from(article, null);
    }

    /**
     * 기사를 수정한다. 작성자 본인만 수정할 수 있다.
     * source, originalUrl, AI 요약도 함께 수정할 수 있다.
     */
    @Transactional
    public ArticleDetailResponse updateArticle(Long memberId, Long articleId, UpdateArticleRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다. id=" + articleId));

        checkOwnership(article, memberId);

        article.update(request.category(), request.title(), request.content(),
                request.source(), request.originalUrl());

        // 기사 수정 시 기존 AI 요약은 그대로 유지한다 (요약은 AI 생성 버튼으로만 관리)
        ArticleSummary summary = articleSummaryRepository.findByArticleId(articleId).orElse(null);

        return ArticleDetailResponse.from(article, summary);
    }

    /**
     * 기사를 삭제한다. 작성자 본인만 삭제할 수 있다.
     *
     * 중요: 외래키(FK) 때문에 기사를 바로 삭제하면 에러가 난다.
     * saved_articles, article_read_histories, article_summaries 테이블이
     * articles 테이블을 참조하고 있기 때문이다.
     *
     * 해결: 자식 테이블 데이터를 먼저 삭제한 뒤 기사를 삭제한다.
     * (JPA의 orphanRemoval이나 cascade를 쓸 수도 있지만,
     *  여기서는 명시적으로 삭제해서 흐름이 보이도록 했다.)
     */
    @Transactional
    public void deleteArticle(Long memberId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("기사를 찾을 수 없습니다. id=" + articleId));

        checkOwnership(article, memberId);

        // 외래키로 연결된 관련 데이터를 먼저 삭제한다 (순서 중요!)
        savedArticleRepository.deleteByArticleId(articleId);       // 다른 유저가 저장한 기록
        articleReadHistoryRepository.deleteByArticleId(articleId);  // 다른 유저의 읽기 기록
        articleSummaryRepository.findByArticleId(articleId)         // AI 요약
                .ifPresent(articleSummaryRepository::delete);

        // 자식 데이터를 다 지운 뒤에야 기사 본체를 삭제할 수 있다
        articleRepository.delete(article);
    }

    /**
     * 기사의 작성자가 요청한 회원인지 확인한다.
     * 작성자가 없는 기사(시스템 등록)는 누구나 수정/삭제할 수 없다.
     */
    private void checkOwnership(Article article, Long memberId) {
        if (article.getWriter() == null || !article.getWriter().getId().equals(memberId)) {
            throw new ForbiddenException("본인이 작성한 기사만 수정/삭제할 수 있습니다.");
        }
    }

    /**
     * 기사 작성자만 AI 요약을 생성하거나 재생성할 수 있다.
     *
     * AI 요약도 DB에 저장되는 기사 부가 정보이므로, 임의의 사용자가 덮어쓰지 못하게 막는다.
     */
    private void checkSummaryGenerationPermission(Article article, Long memberId) {
        if (article.getWriter() == null || !article.getWriter().getId().equals(memberId)) {
            throw new ForbiddenException("본인이 작성한 기사만 AI 요약을 생성할 수 있습니다.");
        }
    }

    // ──────────────────────────────────────────────────────
    // 내부 헬퍼 메서드
    // ──────────────────────────────────────────────────────

    /**
     * Page<Article>을 AI 요약 미리보기와 함께 Page<ArticleListResponse>로 변환한다.
     */
    private Page<ArticleListResponse> withSummaryPreview(Page<Article> articles) {
        Map<Long, String> summaryMap = buildSummaryMap(
                articles.getContent().stream().map(Article::getId).toList());

        return articles.map(a -> ArticleListResponse.from(a, summaryMap.get(a.getId())));
    }

    /**
     * List<Article>을 AI 요약 미리보기와 함께 List<ArticleListResponse>로 변환한다.
     */
    private List<ArticleListResponse> toListWithSummary(List<Article> articles) {
        Map<Long, String> summaryMap = buildSummaryMap(
                articles.stream().map(Article::getId).toList());

        return articles.stream()
                .map(a -> ArticleListResponse.from(a, summaryMap.get(a.getId())))
                .toList();
    }

    /**
     * 기사 ID 목록으로 요약 첫째 줄을 일괄 조회하여 Map으로 반환한다.
     */
    private Map<Long, String> buildSummaryMap(List<Long> articleIds) {
        if (articleIds.isEmpty()) return Map.of();
        return articleSummaryRepository.findByArticleIdIn(articleIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getArticle().getId(),
                        ArticleSummary::getSummaryLine1));
    }

    /**
     * 기사 제목에서 의미 있는 키워드를 추출한다.
     *
     * 예: "삼성전자, 차세대 2나노 반도체 양산 돌입"
     *   → ["삼성전자", "차세대", "2나노", "반도체", "양산"]
     *
     * 규칙:
     *   - 특수문자(쉼표, 따옴표 등)를 제거한다
     *   - 공백으로 단어를 분리한다
     *   - 2글자 미만인 단어는 버린다 (조사/접속사 제거)
     *   - 불용어(흔한 단어)를 제거한다
     *   - 최대 5개까지만 사용한다
     */
    private List<String> extractKeywords(String title) {
        String cleaned = title.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");
        String[] words = cleaned.split("\\s+");

        // 한국어 불용어 — 뉴스 제목에 자주 나오지만 검색에 의미 없는 단어들
        Set<String> stopWords = Set.of(
                "그리고", "하지만", "그러나", "또한", "이번", "올해", "이후",
                "위한", "대한", "통해", "관련", "최근", "현재", "오늘",
                "내일", "등이", "것으로", "것이", "하는", "되는"
        );

        return Arrays.stream(words)
                .filter(w -> w.length() >= 2)
                .filter(w -> !stopWords.contains(w))
                .limit(5)
                .toList();
    }
}
