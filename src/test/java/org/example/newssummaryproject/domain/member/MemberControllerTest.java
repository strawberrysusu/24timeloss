package org.example.newssummaryproject.domain.member;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private String signupAndGetToken(String email, String password, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private Cookie signupAndGetRefreshCookie(String email, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"1234\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookie("refresh_token");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void signup_success_returns_member_and_token() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@test.com\",\"password\":\"1234\",\"nickname\":\"tester\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.nickname").value("tester"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void signup_sets_http_only_refresh_cookie() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cookie@test.com\",\"password\":\"1234\",\"nickname\":\"cookie-user\"}"))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void signup_token_can_access_me_endpoint() throws Exception {
        String token = signupAndGetToken("auto@test.com", "1234", "auto-user");

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auto@test.com"));
    }

    @Test
    void signup_rejects_blank_email() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"1234\",\"nickname\":\"tester\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_rejects_invalid_email() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\",\"password\":\"1234\",\"nickname\":\"tester\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_rejects_short_password() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pw@test.com\",\"password\":\"12\",\"nickname\":\"tester\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success_returns_access_token() throws Exception {
        signupAndGetToken("login@test.com", "1234", "login-user");
        String token = loginAndGetToken("login@test.com", "1234");

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@test.com"));
    }

    @Test
    void login_rejects_wrong_password() throws Exception {
        signupAndGetToken("wrong@test.com", "1234", "wrong-user");

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_rejects_invalid_token() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_cookie_issues_new_access_token() throws Exception {
        Cookie refreshCookie = signupAndGetRefreshCookie("refresh@test.com", "refresh-user");

        MvcResult refreshResult = mockMvc.perform(post("/api/members/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        String newToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("refresh@test.com"));
    }

    @Test
    void refresh_requires_cookie() throws Exception {
        mockMvc.perform(post("/api/members/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rejects_access_token_in_refresh_cookie() throws Exception {
        String accessToken = signupAndGetToken("access@test.com", "1234", "access-user");

        mockMvc.perform(post("/api/members/refresh")
                        .cookie(new Cookie("refresh_token", accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clears_refresh_cookie() throws Exception {
        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void signup_rejects_duplicate_email() throws Exception {
        signupAndGetToken("dup@test.com", "1234", "original-user");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"5678\",\"nickname\":\"duplicate-user\"}"))
                .andExpect(status().isConflict());
    }

    // ── Refresh Token Rotation 보안 동작 ──

    /**
     * /refresh 후 기존 refresh token은 즉시 폐기되어 다시 사용할 수 없어야 한다.
     */
    @Test
    void refresh_rotates_token_old_one_becomes_unusable() throws Exception {
        Cookie firstCookie = signupAndGetRefreshCookie("rotate@test.com", "rotate-user");

        // 1차 refresh — 정상 동작
        mockMvc.perform(post("/api/members/refresh").cookie(firstCookie))
                .andExpect(status().isOk());

        // 같은(이미 회전된) 쿠키로 다시 시도 — 재사용 탐지 → 401
        mockMvc.perform(post("/api/members/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 재사용 탐지 시 해당 회원의 모든 활성 refresh token이 일괄 폐기되어야 한다.
     * (탈취된 토큰 + 정상 사용자 토큰을 모두 강제 로그아웃하는 보안 정책)
     */
    @Test
    void refresh_token_reuse_revokes_all_tokens_for_member() throws Exception {
        Cookie firstCookie = signupAndGetRefreshCookie("reuse@test.com", "reuse-user");

        // 1차 refresh로 firstCookie를 사용함 → DB에서 revoked 처리됨
        MvcResult firstRefresh = mockMvc.perform(post("/api/members/refresh").cookie(firstCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie secondCookie = firstRefresh.getResponse().getCookie("refresh_token");

        // 이미 폐기된 firstCookie를 다시 시도 → 재사용 탐지 → 회원의 모든 토큰 폐기
        mockMvc.perform(post("/api/members/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized());

        // 이전 단계의 secondCookie도 일괄 폐기되었으므로 사용 불가여야 한다
        mockMvc.perform(post("/api/members/refresh").cookie(secondCookie))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 로그아웃 시 서버 측 refresh token도 폐기되어야 한다.
     * 쿠키만 지우면 클라이언트 측 무효화일 뿐 — 토큰을 갖고 있던 공격자는 여전히 사용 가능.
     */
    @Test
    void logout_revokes_refresh_token_server_side() throws Exception {
        Cookie refreshCookie = signupAndGetRefreshCookie("logout-revoke@test.com", "logout-user");

        // 로그아웃
        mockMvc.perform(post("/api/members/logout").cookie(refreshCookie))
                .andExpect(status().isOk());

        // 같은 쿠키로 refresh 시도 → 폐기되었으므로 401
        mockMvc.perform(post("/api/members/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }
}
