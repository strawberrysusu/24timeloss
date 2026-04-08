package org.example.newssummaryproject.domain.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ArticleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ArticleRepository articleRepository;

    private Article article1;
    private Article article2;

    /** 회원가입하고 JWT 토큰을 반환하는 헬퍼 */
    private String signupAndGetToken(String email, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"1234\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @BeforeEach
    void setUp() {
        article1 = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("AI 신기술 발표")
                .originalUrl("https://example.com/ai-" + System.nanoTime())
                .source("테크뉴스")
                .content("AI 관련 본문 내용")
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build());

        article2 = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("클라우드 서비스 확대")
                .originalUrl("https://example.com/cloud-" + System.nanoTime())
                .source("IT뉴스")
                .content("클라우드 관련 본문")
                .publishedAt(LocalDateTime.now())
                .build());
    }

    /**
     * 회원가입 후 기사를 등록하고, 토큰과 기사 ID를 반환하는 헬퍼 메서드다.
     */
    private record AuthAndArticle(String token, Long articleId) {}

    private AuthAndArticle signupAndCreateArticle(String email, String nickname) throws Exception {
        String token = signupAndGetToken(email, nickname);

        MvcResult result = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"내가 쓴 기사\",\"content\":\"본문 내용이며 충분히 길어야 합니다\",\"source\":\"테스트\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long articleId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        return new AuthAndArticle(token, articleId);
    }

    // ── 기사 조회 ──

    @Test
    void 기사_목록_조회() throws Exception {
        mockMvc.perform(get("/api/articles?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void 카테고리별_기사_조회() throws Exception {
        mockMvc.perform(get("/api/articles?category=IT_SCIENCE&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("IT_SCIENCE"));
    }

    @Test
    void 기사_상세_조회() throws Exception {
        mockMvc.perform(get("/api/articles/" + article1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(article1.getId()))
                .andExpect(jsonPath("$.title").value("AI 신기술 발표"))
                .andExpect(jsonPath("$.content").value("AI 관련 본문 내용"));
    }

    @Test
    void 존재하지_않는_기사_404() throws Exception {
        mockMvc.perform(get("/api/articles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ── 검색 ──

    @Test
    void 키워드_검색() throws Exception {
        mockMvc.perform(get("/api/articles/search?keyword=AI&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(containsString("AI")));
    }

    @Test
    void 빈_검색어_400() throws Exception {
        mockMvc.perform(get("/api/articles/search?keyword=&page=0&size=10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("검색어를 입력해주세요."));
    }

    @Test
    void 공백만_검색어_400() throws Exception {
        mockMvc.perform(get("/api/articles/search")
                        .param("keyword", "   ")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    // ── 관련 기사 / 트렌딩 / 브리핑 ──

    @Test
    void 관련_기사_조회() throws Exception {
        mockMvc.perform(get("/api/articles/" + article1.getId() + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 트렌딩_기사_조회() throws Exception {
        mockMvc.perform(get("/api/articles/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 브리핑_조회() throws Exception {
        mockMvc.perform(get("/api/articles/briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").isString())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test
    void 추천_기사_비로그인() throws Exception {
        mockMvc.perform(get("/api/articles/recommendations?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── 인기 검색어 ──

    @Test
    void 인기_검색어_조회() throws Exception {
        mockMvc.perform(get("/api/articles/popular-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void 검색_후_인기_검색어에_반영() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/articles/search?keyword=AI&page=0&size=10"));
        }
        mockMvc.perform(get("/api/articles/popular-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("AI"));
    }

    @Test
    void 기사_상세_조회시_조회수_증가() throws Exception {
        mockMvc.perform(get("/api/articles/" + article1.getId()));
        mockMvc.perform(get("/api/articles/" + article1.getId()));
        mockMvc.perform(get("/api/articles/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(article1.getId()));
    }

    // ── 기사 등록 (POST /api/articles) ──

    @Test
    void 기사_등록_성공() throws Exception {
        String token = signupAndGetToken("writer@test.com", "작성자");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"테스트 등록 기사\",\"content\":\"등록 본문이며 10자 이상입니다\",\"source\":\"테스트\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("테스트 등록 기사"))
                .andExpect(jsonPath("$.category").value("ECONOMY"))
                .andExpect(jsonPath("$.writerNickname").value("작성자"));
    }

    @Test
    void 기사_등록_원문URL_없으면_null() throws Exception {
        String token = signupAndGetToken("nourl@test.com", "작성자2");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"IT_SCIENCE\",\"title\":\"URL 없는 기사\",\"content\":\"본문 내용이며 충분히 길어야 합니다\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").doesNotExist());
    }

    @Test
    void 비로그인_기사_등록_401() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"제목입니다\",\"content\":\"본문 내용이며 10자 이상입니다\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 기사_등록_제목없으면_400() throws Exception {
        String token = signupAndGetToken("admin2@test.com", "관리자2");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"\",\"content\":\"본문 내용이며 10자 이상입니다\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 기사_등록_본문짧으면_400() throws Exception {
        String token = signupAndGetToken("admin3@test.com", "관리자3");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"정상 제목\",\"content\":\"짧음\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── 기사 수정 (PATCH /api/articles/{id}) ──

    @Test
    void 기사_수정_성공() throws Exception {
        var auth = signupAndCreateArticle("editor@test.com", "수정자");

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정된 제목입니다\",\"content\":\"수정된 본문이며 충분히 길어야 합니다\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목입니다"))
                .andExpect(jsonPath("$.content").value("수정된 본문이며 충분히 길어야 합니다"))
                .andExpect(jsonPath("$.category").value("ECONOMY"));
    }

    @Test
    void 기사_수정_출처_빈값으로_삭제() throws Exception {
        var auth = signupAndCreateArticle("clr@test.com", "삭제테스트");

        mockMvc.perform(get("/api/articles/" + auth.articleId()))
                .andExpect(jsonPath("$.source").value("테스트"));

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").doesNotExist());
    }

    @Test
    void 기사_수정_카테고리만_변경() throws Exception {
        var auth = signupAndCreateArticle("cat@test.com", "카테수정");

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"IT_SCIENCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("IT_SCIENCE"))
                .andExpect(jsonPath("$.title").value("내가 쓴 기사"));
    }

    @Test
    void 비로그인_기사_수정_401() throws Exception {
        mockMvc.perform(patch("/api/articles/" + article1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정 시도\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 다른_사람_기사_수정_403() throws Exception {
        var authA = signupAndCreateArticle("ownerA@test.com", "작성자A");

        String tokenB = signupAndGetToken("otherB@test.com", "타인B");

        mockMvc.perform(patch("/api/articles/" + authA.articleId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"남의 기사 수정 시도\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 시스템_기사_수정_403() throws Exception {
        String token = signupAndGetToken("sys@test.com", "시스템");

        mockMvc.perform(patch("/api/articles/" + article1.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"시스템 기사 수정 시도\"}"))
                .andExpect(status().isForbidden());
    }

    // ── 기사 삭제 (DELETE /api/articles/{id}) ──

    @Test
    void 기사_삭제_성공() throws Exception {
        var auth = signupAndCreateArticle("del@test.com", "삭제자");

        mockMvc.perform(delete("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/articles/" + auth.articleId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 비로그인_기사_삭제_401() throws Exception {
        mockMvc.perform(delete("/api/articles/" + article1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 존재하지않는_기사_삭제_404() throws Exception {
        String token = signupAndGetToken("del2@test.com", "삭제자2");

        mockMvc.perform(delete("/api/articles/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void 다른_사람_기사_삭제_403() throws Exception {
        var authC = signupAndCreateArticle("ownerC@test.com", "작성자C");

        String tokenD = signupAndGetToken("otherD@test.com", "타인D");

        mockMvc.perform(delete("/api/articles/" + authC.articleId())
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 저장된_기사_삭제시_저장기록도_삭제() throws Exception {
        var auth = signupAndCreateArticle("cascade@test.com", "캐스케이드");

        mockMvc.perform(post("/api/mypage/saved-articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/mypage/saved-article-ids")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── AI 요약 생성 (POST /api/articles/{id}/generate-summary) ──

    @Test
    void AI_요약_생성_성공() throws Exception {
        var auth = signupAndCreateArticle("ai@test.com", "AI테스터");

        mockMvc.perform(post("/api/articles/" + auth.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.summaryLine1").isNotEmpty())
                .andExpect(jsonPath("$.summary.summaryLine2").isNotEmpty())
                .andExpect(jsonPath("$.summary.summaryLine3").isNotEmpty())
                .andExpect(jsonPath("$.summary.keyPoint1").isNotEmpty())
                .andExpect(jsonPath("$.summary.keyPoint2").isNotEmpty())
                .andExpect(jsonPath("$.summary.keyPoint3").isNotEmpty());
    }

    @Test
    void AI_요약_생성후_상세조회시_요약_포함() throws Exception {
        var auth = signupAndCreateArticle("ai2@test.com", "AI테스터2");

        mockMvc.perform(get("/api/articles/" + auth.articleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").doesNotExist());

        mockMvc.perform(post("/api/articles/" + auth.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/articles/" + auth.articleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.summaryLine1").isNotEmpty());
    }

    @Test
    void AI_요약_재생성_덮어쓰기() throws Exception {
        var auth = signupAndCreateArticle("ai3@test.com", "AI테스터3");

        mockMvc.perform(post("/api/articles/" + auth.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/articles/" + auth.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void 비로그인_AI_요약_생성_401() throws Exception {
        mockMvc.perform(post("/api/articles/" + article1.getId() + "/generate-summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 다른_회원_기사_AI_요약_생성_403() throws Exception {
        var authOwner = signupAndCreateArticle("ai-owner@test.com", "AI작성자");

        String otherToken = signupAndGetToken("ai-other@test.com", "다른회원");

        mockMvc.perform(post("/api/articles/" + authOwner.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 작성자_없는_기사_AI_요약_생성_403() throws Exception {
        String token = signupAndGetToken("ai5@test.com", "AI테스터5");

        mockMvc.perform(post("/api/articles/" + article1.getId() + "/generate-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 존재하지않는_기사_AI_요약_404() throws Exception {
        String token = signupAndGetToken("ai4@test.com", "AI테스터4");

        mockMvc.perform(post("/api/articles/999999/generate-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── 에러 응답 형식 통일 ──

    @Test
    void 에러응답_형식_통일_검증() throws Exception {
        // 404 — NOT_FOUND
        mockMvc.perform(get("/api/articles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        // 401 — UNAUTHORIZED
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 409 — DUPLICATE (이메일 중복)
        signupAndGetToken("dup@test.com", "원본");
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"5678\",\"nickname\":\"복사\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE"));

        // 400 — VALIDATION_ERROR (이메일 형식)
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\",\"password\":\"1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // 403 — FORBIDDEN (다른 사람 기사 수정)
        String token = signupAndGetToken("err403@test.com", "에러검증");
        mockMvc.perform(patch("/api/articles/" + article1.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"권한없는 수정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
