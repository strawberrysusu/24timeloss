package org.example.newssummaryproject.domain.news;

import com.jayway.jsonpath.JsonPath;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class ArticleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ArticleRepository articleRepository;

    private Article article1;
    private Article article2;

    private record AuthAndArticle(String token, Long articleId) {
    }

    private String signupAndGetToken(String email, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"1234\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private AuthAndArticle signupAndCreateArticle(String email, String nickname) throws Exception {
        String token = signupAndGetToken(email, nickname);

        MvcResult result = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"New article\",\"content\":\"This article body is intentionally long enough for validation.\",\"source\":\"TestSource\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long articleId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        return new AuthAndArticle(token, articleId);
    }

    @BeforeEach
    void setUp() {
        article1 = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("AI update released")
                .originalUrl("https://example.com/ai-" + System.nanoTime())
                .source("TechPress")
                .content("AI article body")
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build());

        article2 = articleRepository.save(Article.builder()
                .category(Category.IT_SCIENCE)
                .title("Cloud service update")
                .originalUrl("https://example.com/cloud-" + System.nanoTime())
                .source("ITNews")
                .content("Cloud article body")
                .publishedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void list_articles_returns_page_content() throws Exception {
        mockMvc.perform(get("/api/articles?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void list_articles_by_category() throws Exception {
        mockMvc.perform(get("/api/articles?category=IT_SCIENCE&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("IT_SCIENCE"));
    }

    @Test
    void get_article_detail() throws Exception {
        mockMvc.perform(get("/api/articles/" + article1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(article1.getId()))
                .andExpect(jsonPath("$.title").value("AI update released"))
                .andExpect(jsonPath("$.content").value("AI article body"));
    }

    @Test
    void get_missing_article_returns_not_found() throws Exception {
        mockMvc.perform(get("/api/articles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void search_by_keyword_returns_matching_article() throws Exception {
        mockMvc.perform(get("/api/articles/search?keyword=AI&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(containsString("AI")));
    }

    @Test
    void search_with_blank_keyword_returns_bad_request() throws Exception {
        mockMvc.perform(get("/api/articles/search?keyword=&page=0&size=10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void search_with_whitespace_keyword_returns_bad_request() throws Exception {
        mockMvc.perform(get("/api/articles/search")
                        .param("keyword", "   ")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_related_articles() throws Exception {
        mockMvc.perform(get("/api/articles/" + article1.getId() + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void get_trending_articles() throws Exception {
        mockMvc.perform(get("/api/articles/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void get_briefing() throws Exception {
        mockMvc.perform(get("/api/articles/briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").isString())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test
    void get_recommendations_without_login() throws Exception {
        mockMvc.perform(get("/api/articles/recommendations?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void get_popular_keywords() throws Exception {
        mockMvc.perform(get("/api/articles/popular-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searches_are_reflected_in_popular_keywords() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/articles/search?keyword=AI&page=0&size=10"));
        }

        mockMvc.perform(get("/api/articles/popular-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("AI"));
    }

    @Test
    void article_detail_increases_trending_rank() throws Exception {
        mockMvc.perform(get("/api/articles/" + article1.getId()));
        mockMvc.perform(get("/api/articles/" + article1.getId()));

        mockMvc.perform(get("/api/articles/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(article1.getId()));
    }

    @Test
    void create_article_success() throws Exception {
        String token = signupAndGetToken("writer@test.com", "writer-user");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"Created article\",\"content\":\"Created article body with enough length.\",\"source\":\"TestSource\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Created article"))
                .andExpect(jsonPath("$.category").value("ECONOMY"))
                .andExpect(jsonPath("$.writerNickname").value("writer-user"));
    }

    @Test
    void create_article_without_original_url_omits_url_field() throws Exception {
        String token = signupAndGetToken("nourl@test.com", "writer-user");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"IT_SCIENCE\",\"title\":\"Article without url\",\"content\":\"Body content is long enough for validation.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").doesNotExist());
    }

    @Test
    void create_article_requires_authentication() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"No auth\",\"content\":\"This body is long enough for validation.\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void create_article_rejects_blank_title() throws Exception {
        String token = signupAndGetToken("admin2@test.com", "admin-two");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"\",\"content\":\"This body is long enough for validation.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_article_rejects_short_content() throws Exception {
        String token = signupAndGetToken("admin3@test.com", "admin-three");

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"ECONOMY\",\"title\":\"Valid title\",\"content\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_article_success() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("editor@test.com", "editor-user");

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"content\":\"Updated article body with enough length.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.content").value("Updated article body with enough length."))
                .andExpect(jsonPath("$.category").value("ECONOMY"));
    }

    @Test
    void update_article_with_blank_source_clears_source() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("clear@test.com", "clear-source-user");

        mockMvc.perform(get("/api/articles/" + auth.articleId()))
                .andExpect(jsonPath("$.source").value("TestSource"));

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").doesNotExist());
    }

    @Test
    void update_article_category_only() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("cat@test.com", "category-editor");

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"IT_SCIENCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("IT_SCIENCE"))
                .andExpect(jsonPath("$.title").value("New article"));
    }

    @Test
    void update_article_requires_authentication() throws Exception {
        mockMvc.perform(patch("/api/articles/" + article1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Unauthorized update\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_other_users_article_returns_forbidden() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("ownerA@test.com", "owner-a");
        String tokenB = signupAndGetToken("otherB@test.com", "other-b");

        mockMvc.perform(patch("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Forbidden update\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void update_seed_article_returns_forbidden() throws Exception {
        String token = signupAndGetToken("sys@test.com", "system-user");

        mockMvc.perform(patch("/api/articles/" + article1.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Seed article update\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_article_success() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("del@test.com", "delete-user");

        mockMvc.perform(delete("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/articles/" + auth.articleId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_article_requires_authentication() throws Exception {
        mockMvc.perform(delete("/api/articles/" + article1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_missing_article_returns_not_found() throws Exception {
        String token = signupAndGetToken("del2@test.com", "delete-two");

        mockMvc.perform(delete("/api/articles/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_other_users_article_returns_forbidden() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("ownerC@test.com", "owner-c");
        String tokenD = signupAndGetToken("otherD@test.com", "other-d");

        mockMvc.perform(delete("/api/articles/" + auth.articleId())
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void delete_article_cascades_saved_article_state() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("cascade@test.com", "cascade-user");

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

    @Test
    void generate_summary_success() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("ai@test.com", "ai-user");

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
    void generated_summary_is_visible_in_article_detail() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("ai2@test.com", "ai-user-two");

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
    void generate_summary_is_idempotent() throws Exception {
        AuthAndArticle auth = signupAndCreateArticle("ai3@test.com", "ai-user-three");

        mockMvc.perform(post("/api/articles/" + auth.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/articles/" + auth.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void generate_summary_requires_authentication() throws Exception {
        mockMvc.perform(post("/api/articles/" + article1.getId() + "/generate-summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void generate_summary_for_other_users_article_returns_forbidden() throws Exception {
        AuthAndArticle authOwner = signupAndCreateArticle("ai-owner@test.com", "ai-owner");
        String otherToken = signupAndGetToken("ai-other@test.com", "ai-other");

        mockMvc.perform(post("/api/articles/" + authOwner.articleId() + "/generate-summary")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void generate_summary_for_seed_article_returns_forbidden() throws Exception {
        String token = signupAndGetToken("ai5@test.com", "ai-user-five");

        mockMvc.perform(post("/api/articles/" + article1.getId() + "/generate-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void generate_summary_for_missing_article_returns_not_found() throws Exception {
        String token = signupAndGetToken("ai4@test.com", "ai-user-four");

        mockMvc.perform(post("/api/articles/999999/generate-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void error_response_format_is_consistent() throws Exception {
        mockMvc.perform(get("/api/articles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        signupAndGetToken("dup@test.com", "duplicate-source");
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"5678\",\"nickname\":\"duplicate-target\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\",\"password\":\"1234\",\"nickname\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String token = signupAndGetToken("err403@test.com", "error-check");
        mockMvc.perform(patch("/api/articles/" + article1.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Forbidden update\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
