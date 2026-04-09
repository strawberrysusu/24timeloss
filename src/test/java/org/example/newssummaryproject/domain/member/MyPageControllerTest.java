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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ArticleRepository articleRepository;

    private String token;
    private Article article;

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
        token = signupAndGetToken("mypage@test.com", "mypage-user");

        article = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("Test article")
                .originalUrl("https://example.com/test-" + System.nanoTime())
                .source("TestSource")
                .content("Test content")
                .publishedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void mypage_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/mypage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mypage_returns_profile_summary() throws Exception {
        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("mypage-user"))
                .andExpect(jsonPath("$.streak").isNumber())
                .andExpect(jsonPath("$.readArticleCount").value(0));
    }

    @Test
    void saved_article_appears_in_saved_lists() throws Exception {
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
    void unsave_article_removes_saved_state() throws Exception {
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
    void saving_same_article_twice_returns_conflict() throws Exception {
        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(post("/api/mypage/saved-articles/" + article.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void interests_can_be_added_and_removed() throws Exception {
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
    void read_history_updates_count_and_streak() throws Exception {
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
    void duplicate_read_same_day_counts_once() throws Exception {
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId())
                .header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/api/mypage/read-history/" + article.getId())
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.readArticleCount").value(1));
    }

    @Test
    void update_nickname_success() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"updated-name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("updated-name"));

        mockMvc.perform(get("/api/mypage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.nickname").value("updated-name"));
    }

    @Test
    void update_nickname_blank_returns_bad_request() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_nickname_requires_authentication() throws Exception {
        mockMvc.perform(patch("/api/mypage/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"updated-name\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_password_success() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"1234\",\"newPassword\":\"5678\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mypage@test.com\",\"password\":\"5678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void update_password_rejects_wrong_current_password() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"5678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void update_password_rejects_short_new_password() throws Exception {
        mockMvc.perform(patch("/api/mypage/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"1234\",\"newPassword\":\"12\"}"))
                .andExpect(status().isBadRequest());
    }
}
