package org.example.newssummaryproject.domain.member;

import com.jayway.jsonpath.JsonPath;
import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.ArticleRepository;
import org.example.newssummaryproject.domain.news.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyPageControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ArticleRepository articleRepository;

    private String token;
    private Article article;

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
    void setUp() throws Exception {
        token = signupAndGetToken("mypage@test.com", "마이페이지");

        article = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("테스트 기사")
                .originalUrl("https://example.com/test-" + System.nanoTime())
                .source("테스트")
                .content("내용")
                .publishedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void 비로그인_마이페이지_401() throws Exception {
        mockMvc.perform(get("/api/mypage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 마이페이지_조회_성공() throws Exception {
        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("마이페이지"))
                .andExpect(jsonPath("$.streak").isNumber())
                .andExpect(jsonPath("$.readArticleCount").value(0));
    }

    @Test
    void 기사_저장_후_목록에_포함() throws Exception {
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage/saved-article-ids")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(article.getId()));

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedArticles[0].id").value(article.getId()));
    }

    @Test
    void 기사_저장_취소() throws Exception {
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId())
                        .header("Authorization", "Bearer " + token));

        mockMvc.perform(delete("/api/mypage/saved-articles/" + article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage/saved-article-ids")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 중복_저장시_409() throws Exception {
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void 관심분야_추가_삭제() throws Exception {
        mockMvc.perform(post("/api/mypage/interests/IT_SCIENCE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.interests[0]").value("IT_SCIENCE"));

        mockMvc.perform(delete("/api/mypage/interests/IT_SCIENCE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.interests").isEmpty());
    }

    @Test
    void 읽기기록_저장_후_마이페이지_반영() throws Exception {
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.readArticleCount").value(1))
                .andExpect(jsonPath("$.streak").value(1));

        mockMvc.perform(get("/api/mypage/read-history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value(article.getId()));
    }

    @Test
    void 같은기사_같은날_중복읽기_기록_한번만() throws Exception {
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId())
                .header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.readArticleCount").value(1));
    }

    // ── 닉네임 변경 테스트 ──

    @Test
    void 닉네임_변경_성공() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"));

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.nickname").value("새닉네임"));
    }

    @Test
    void 닉네임_빈값이면_400() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 비로그인_닉네임_변경_401() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── 비밀번호 변경 테스트 ──

    @Test
    void 비밀번호_변경_성공() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"1234\",\"newPassword\":\"5678\"}"))
                .andExpect(status().isOk());

        // 새 비밀번호로 로그인 확인
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mypage@test.com\",\"password\":\"5678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void 비밀번호_변경_현재비밀번호_틀리면_400() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"5678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 비밀번호_변경_새비밀번호_짧으면_400() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"1234\",\"newPassword\":\"12\"}"))
                .andExpect(status().isBadRequest());
    }
}
