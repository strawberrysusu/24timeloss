package org.example.newssummaryproject.domain.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void 회원가입_성공() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@test.com\",\"password\":\"1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.nickname").value("테스터"));
    }

    @Test
    void 회원가입_후_세션이_설정된다() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/members/signup")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"auto@test.com\",\"password\":\"1234\",\"nickname\":\"자동로그인\"}"))
                .andExpect(status().isCreated());

        // SecurityContext가 세션에 저장되었으므로 /me 호출이 성공해야 한다
        mockMvc.perform(get("/api/members/me").session(session))
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
    void 로그인_성공_후_세션설정() throws Exception {
        // 가입
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"login@test.com\",\"password\":\"1234\",\"nickname\":\"로그인\"}"));

        // 새 세션으로 로그인
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/members/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.com\",\"password\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@test.com"));

        // 세션으로 /me 접근
        mockMvc.perform(get("/api/members/me").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void 로그인_잘못된_비밀번호_400() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"wrong@test.com\",\"password\":\"1234\",\"nickname\":\"테스터\"}"));

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
    void 로그아웃_후_me_호출시_401() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/members/signup")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"out@test.com\",\"password\":\"1234\",\"nickname\":\"로그아웃\"}"));

        mockMvc.perform(post("/api/members/logout").session(session))
                .andExpect(status().isOk());

        // 로그아웃 후 세션이 무효화되었으므로 새 세션으로 요청 → 401
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 이메일_중복_가입_409() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"dup@test.com\",\"password\":\"1234\",\"nickname\":\"원본\"}"));

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"5678\",\"nickname\":\"복사\"}"))
                .andExpect(status().isConflict());
    }
}
