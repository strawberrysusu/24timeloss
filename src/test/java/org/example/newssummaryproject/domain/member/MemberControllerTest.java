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

    @Autowired MockMvc mockMvc;

    /** 회원가입하고 access token을 반환하는 헬퍼 */
    private String signupAndGetToken(String email, String password, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    /** 회원가입하고 refresh cookie를 반환하는 헬퍼 */
    private Cookie signupAndGetRefreshCookie(String email, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"1234\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookie("refresh_token");
    }

    /** 로그인하고 access token을 반환하는 헬퍼 */
    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void 회원가입_성공() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@test.com\",\"password\":\"1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.nickname").value("테스터"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void 회원가입시_refresh_쿠키가_httpOnly로_발급된다() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cookie@test.com\",\"password\":\"1234\",\"nickname\":\"쿠키\"}"))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void 회원가입_후_토큰으로_인증된다() throws Exception {
        String token = signupAndGetToken("auto@test.com", "1234", "자동로그인");

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auto@test.com"));
    }

    @Test
    void 회원가입_이메일_빈값이면_400() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 회원가입_이메일_형식오류_400() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\",\"password\":\"1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 회원가입_비밀번호_짧으면_400() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pw@test.com\",\"password\":\"12\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_성공_토큰_발급() throws Exception {
        signupAndGetToken("login@test.com", "1234", "로그인");
        String token = loginAndGetToken("login@test.com", "1234");

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@test.com"));
    }

    @Test
    void 로그인_잘못된_비밀번호_400() throws Exception {
        signupAndGetToken("wrong@test.com", "1234", "테스터");

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비로그인_상태에서_me_호출시_401() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 잘못된_토큰으로_me_호출시_401() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ── Refresh Token (httpOnly 쿠키) 테스트 ──

    @Test
    void refresh_쿠키로_새_accessToken_발급() throws Exception {
        Cookie refreshCookie = signupAndGetRefreshCookie("rt@test.com", "RT테스트");

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
                .andExpect(jsonPath("$.email").value("rt@test.com"));
    }

    @Test
    void refresh_쿠키_없으면_401() throws Exception {
        mockMvc.perform(post("/api/members/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessToken으로_refresh_요청시_401() throws Exception {
        String accessToken = signupAndGetToken("at@test.com", "1234", "AT테스트");

        // access token을 refresh 쿠키로 넣으면 거부
        mockMvc.perform(post("/api/members/refresh")
                        .cookie(new Cookie("refresh_token", accessToken)))
                .andExpect(status().isUnauthorized());
    }

    // ── 로그아웃 ──

    @Test
    void 로그아웃시_refresh_쿠키_삭제() throws Exception {
        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void 이메일_중복_가입_409() throws Exception {
        signupAndGetToken("dup@test.com", "1234", "원본");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"5678\",\"nickname\":\"복사\"}"))
                .andExpect(status().isConflict());
    }
}
