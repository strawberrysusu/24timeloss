package org.example.newssummaryproject.domain.member;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.newssummaryproject.domain.member.dto.LoginRequest;
import org.example.newssummaryproject.domain.member.dto.MemberResponse;
import org.example.newssummaryproject.domain.member.dto.SignupRequest;
import org.example.newssummaryproject.global.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * ── 회원 컨트롤러 (HTTP 요청 진입점) ──
 *
 * 흐름: 브라우저/프론트 → ★ Controller → Service → Repository → DB
 *
 * 이 클래스는 회원 관련 HTTP 요청을 받아서 MemberService에 위임하고,
 * 결과를 JSON으로 변환해서 응답한다.
 *
 * 핵심 개념:
 *   - @RestController: @Controller + @ResponseBody의 합체.
 *              메서드 반환값을 자동으로 JSON으로 변환해서 HTTP 응답 본문에 넣는다.
 *   - @RequestMapping("/api/members"): 이 컨트롤러의 모든 엔드포인트 URL 앞에
 *              /api/members가 자동으로 붙는다. 예: @PostMapping("/signup") → POST /api/members/signup
 *   - ResponseEntity<T>: HTTP 상태 코드(200, 201, 404 등)를 직접 지정할 수 있는 응답 래퍼.
 *              ResponseEntity.ok(body) → 200 OK + body
 *              ResponseEntity.status(CREATED).body(body) → 201 Created + body
 *
 * 인증 방식 — 세션(Session) 기반:
 *   1. 로그인 성공 → 서버가 세션에 memberId를 저장 (쿠키로 세션ID를 브라우저에 전달)
 *   2. 이후 요청마다 브라우저가 자동으로 세션 쿠키를 보냄
 *   3. 서버가 세션에서 memberId를 꺼내서 "누가 요청했는지" 파악
 *   4. 로그아웃 → 세션 삭제 → 이후 요청에서 memberId를 꺼낼 수 없음 → 비로그인 처리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입 — POST /api/members/signup
     *
     * @Valid: SignupRequest의 @NotBlank, @Email, @Size 검증을 자동 실행한다.
     *         검증에 실패하면 MethodArgumentNotValidException이 발생하고
     *         GlobalExceptionHandler가 400(VALIDATION_ERROR) 응답을 만든다.
     * @RequestBody: HTTP 요청 본문(JSON)을 SignupRequest 객체로 자동 변환한다.
     * HttpSession: Spring이 자동으로 현재 세션을 주입해 준다.
     */
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request, HttpSession session) {
        MemberResponse response = memberService.signup(request);
        // 회원가입 즉시 로그인 상태로 만든다 (세션에 memberId 저장)
        session.setAttribute("memberId", response.id());
        // 201 Created: 새 리소스(회원)가 생성되었음을 나타낸다
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인 — POST /api/members/login
     *
     * 성공하면 세션에 memberId를 저장한다.
     * 이후 다른 API에서 세션을 통해 "이 사용자가 로그인 중"임을 확인한다.
     */
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        MemberResponse response = memberService.login(request);
        session.setAttribute("memberId", response.id());
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃 — POST /api/members/logout
     *
     * session.invalidate()로 서버 측 세션 데이터를 모두 삭제한다.
     * 브라우저의 세션 쿠키는 남아있지만, 서버에 해당 세션이 없으므로
     * 이후 요청에서 memberId를 꺼낼 수 없다 → 비로그인 상태가 된다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    /**
     * 내 정보 조회 — GET /api/members/me
     *
     * 로그인한 사용자가 자기 정보를 볼 때 사용한다.
     * 세션에서 memberId를 꺼내고, 그 ID로 DB에서 회원을 조회한다.
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(HttpSession session) {
        Long memberId = getLoginMemberId(session);
        return ResponseEntity.ok(memberService.getMember(memberId));
    }

    /**
     * 세션에서 로그인된 회원 ID를 꺼내는 공용 헬퍼 메서드다.
     *
     * static인 이유: ArticleController, MyPageController 등 다른 컨트롤러에서도
     * MemberController.getLoginMemberId(session) 형태로 호출하기 위해서다.
     * → 로그인 확인 로직을 한 곳에 모아서 중복 코드를 방지한다.
     *
     * memberId가 없으면(= 로그인 안 됨) UnauthorizedException(401)을 던진다.
     */
    public static Long getLoginMemberId(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return memberId;
    }
}
