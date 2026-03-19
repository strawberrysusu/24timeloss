package org.example.newssummaryproject.domain.member;

import org.example.newssummaryproject.domain.news.Article;
import org.example.newssummaryproject.domain.news.ArticleRepository;
import org.example.newssummaryproject.domain.news.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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

    private MockHttpSession session;
    private Article article;

    @BeforeEach
    void setUp() throws Exception {
        session = new MockHttpSession();

        // 회원가입 (세션 자동 설정)
        mockMvc.perform(post("/api/members/signup")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mypage@test.com\",\"password\":\"1234\",\"nickname\":\"마이페이지\"}"));

        // 기사 생성
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
        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("마이페이지"))
                .andExpect(jsonPath("$.streak").isNumber())
                .andExpect(jsonPath("$.readArticleCount").value(0));
    }

    @Test
    void 기사_저장_후_목록에_포함() throws Exception {
        // 저장
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId()).session(session))
                .andExpect(status().isOk());

        // 저장 ID 목록 확인
        mockMvc.perform(get("/api/mypage/saved-article-ids").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(article.getId()));

        // 마이페이지에서도 확인
        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedArticles[0].id").value(article.getId()));
    }

    @Test
    void 기사_저장_취소() throws Exception {
        // 저장
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId()).session(session));

        // 취소
        mockMvc.perform(delete("/api/mypage/saved-articles/" + article.getId()).session(session))
                .andExpect(status().isOk());

        // 목록에서 제거 확인
        mockMvc.perform(get("/api/mypage/saved-article-ids").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void 중복_저장시_409() throws Exception {
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId()).session(session));

        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId()).session(session))
                .andExpect(status().isConflict());
    }

    @Test
    void 관심분야_추가_삭제() throws Exception {
        // 추가
        mockMvc.perform(post("/api/mypage/interests/IT_SCIENCE").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(jsonPath("$.interests[0]").value("IT_SCIENCE"));

        // 삭제
        mockMvc.perform(delete("/api/mypage/interests/IT_SCIENCE").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(jsonPath("$.interests").isEmpty());
    }

    @Test
    void 읽기기록_저장_후_마이페이지_반영() throws Exception {
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId()).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(jsonPath("$.readArticleCount").value(1))
                .andExpect(jsonPath("$.streak").value(1));

        mockMvc.perform(get("/api/mypage/read-history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value(article.getId()));
    }

    @Test
    void 같은기사_같은날_중복읽기_기록_한번만() throws Exception {
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId()).session(session));
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId()).session(session));

        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(jsonPath("$.readArticleCount").value(1));
    }

    // ── 닉네임 변경 테스트 ──

    @Test
    void 닉네임_변경_성공() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"));

        // 마이페이지에서도 변경 확인
        mockMvc.perform(get("/api/mypage").session(session))
                .andExpect(jsonPath("$.nickname").value("새닉네임"));
    }

    @Test
    void 닉네임_빈값이면_400() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .session(session)
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
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"1234\",\"newPassword\":\"5678\"}"))
                .andExpect(status().isOk());

        // 새 비밀번호로 로그인 확인
        MockHttpSession newSession = new MockHttpSession();
        mockMvc.perform(post("/api/members/login")
                        .session(newSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mypage@test.com\",\"password\":\"5678\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 비밀번호_변경_현재비밀번호_틀리면_400() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"5678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 비밀번호_변경_새비밀번호_짧으면_400() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"1234\",\"newPassword\":\"12\"}"))
                .andExpect(status().isBadRequest());
    }
}
