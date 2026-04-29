# NewsPick - AI 뉴스 요약 서비스

뉴스 기사를 등록하면 AI가 3줄 요약과 핵심 포인트를 자동 생성하는 웹 서비스입니다.

> **서비스 주소**: https://43.202.121.142.nip.io
>
> **테스트 계정**: `test@test.com` / `1234` (dev 환경 시드 데이터)

## 주요 기능

- **AI 뉴스 요약**: URL로 기사를 등록하면 본문을 자동 추출(Jsoup + Readability4J)하고 AI(LLaMA 3.3 70B)가 3줄 요약 + 핵심 포인트 생성
- **자동 뉴스 수집**: 네이버 검색 API로 카테고리별 최신 뉴스를 30분 간격 수집, 90일 후 미열람 시 자동 삭제 (저장된 기사는 영구 보존)
- **카테고리별 뉴스 피드**: 정치, 경제, 사회, IT/과학, 세계, 스포츠, 연예 카테고리 분류
- **개인화**: 관심 카테고리 설정, 기사 저장, 읽기 기록, 추천/트렌딩/브리핑
- **인증**: 이메일/비밀번호 + Google/Naver OAuth2 소셜 로그인
- **영상 임베드**: 네이버 뉴스 / YouTube 영상 안전 임베드 (sandbox iframe)

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Backend** | Java 17, Spring Boot 4, Spring Security, Spring Data JPA |
| **Frontend** | React 19, TypeScript, Vite, React Router |
| **Database** | MySQL 8.0, Flyway (스키마 마이그레이션 V1~V5) |
| **인증** | JWT (Access + DB-stateful Refresh + Rotation), Google/Naver OAuth2, OAuth one-time code 교환 |
| **인프라** | Docker Compose, Nginx (HTTPS + 보안 헤더), Let's Encrypt, GitHub Actions CI/CD |
| **AI** | NVIDIA NIM (LLaMA 3.3 70B), OpenAI Chat Completions 호환, SSE 스트리밍 |
| **외부 API** | 네이버 검색 API (뉴스 수집) |
| **기타** | Jsoup, Readability4J, springdoc-openapi, Lombok |

## 아키텍처

```
[Browser] ── [Nginx (HTTPS + 보안 헤더)] ── [Spring Boot + React SPA] ── [MySQL]
                                                      │
                                          ┌───────────┴───────────┐
                                          │                       │
                                   [NVIDIA AI API]         [네이버 검색 API]
```

- **SPA 통합 빌드**: React 앱이 Gradle 빌드 시 `src/main/resources/static/react/`로 번들링, Spring Boot가 함께 서빙
- **Stateful Refresh**: Access Token은 stateless JWT로 빠르게, Refresh Token은 DB 해시 저장으로 즉시 무효화 가능

### 배포 구조

```
[GitHub] ──push──→ [GitHub Actions CI] ──성공──→ [GitHub Actions CD]
                        │                              │
                   build + test                   SSH로 EC2 접속
                                                       │
                                              git pull → docker compose up --build
                                                       │
                                              ┌────────┴────────┐
                                              │   EC2 인스턴스    │
                                              │                  │
                                              │  [Nginx :443]    │
                                              │       ↓          │
                                              │  [Spring Boot]   │
                                              │       ↓          │
                                              │  [MySQL 8.0]     │
                                              │                  │
                                              │  [Certbot]       │
                                              └─────────────────┘
```

### 기사 추출 → AI 요약 흐름

```
사용자가 URL 입력
    ↓
ArticleExtractService.extract(url)
    ├── SSRF 방어: 스킴/IP 화이트리스트 검증 (loopback, private, link-local 차단)
    ├── Jsoup으로 HTML 가져오기 (15초 타임아웃, 1회 재시도)
    ├── 리다이렉트 추적 시 hop마다 SSRF 재검증
    ├── 사이트별 분기: 네이버뉴스 / KBS / 일반
    ├── 제목, 본문, 출처, 썸네일, 영상 URL 추출
    └── 본문 < 50자면 og:description 폴백
    ↓
사용자가 기사 등록 (POST /api/articles)
    ↓
사용자가 "AI 요약 생성" 클릭 (30초 cooldown 적용)
    ↓
AiSummaryService.summarize(content)
    ├── NVIDIA NIM API 호출 (LLaMA 3.3 70B)
    ├── SSE 스트리밍으로 응답 수신
    ├── JSON 파싱 → 3줄 요약 + 핵심 포인트 3개
    └── DB에 저장 (ArticleSummary)
```

### OAuth2 로그인 흐름 (one-time code 교환)

```
사용자 → "네이버로 로그인" 클릭
    ↓
Spring Security가 Naver 인증 페이지로 리다이렉트
    ↓
사용자 동의 → Naver가 콜백 URL로 redirect
    ↓
OAuth2LoginSuccessHandler:
    ├── 사용자 정보로 회원 찾기/생성
    ├── 32바이트 SecureRandom one-time code 발급
    ├── DB에 SHA-256 해시만 저장 (60초 수명)
    └── /?oauth_code={code} 로 리다이렉트
    ↓
프론트:
    ├── URL에서 oauth_code 읽기 즉시 history.replaceState로 제거
    ├── POST /api/members/oauth/exchange { code }
    └── 응답: access token (body) + refresh token (httpOnly cookie)
    ↓
서버: code redeem (1회용 보장) + JWT 발급 + refresh DB 등록
```

> **왜 access token을 URL에 직접 안 넣나**: URL은 서버 access log, 프록시 로그, 브라우저 history, 외부 이동 시 referrer 헤더에 기록될 수 있다. 1분 이내 1회용 code는 노출돼도 즉시 무효화 + 만료되어 위험이 작다.

## ERD

```
            ┌────────────┐
            │  members   │
            │────────────│
            │ id (PK)    │
            │ email (UQ) │
            │ password   │
            │ nickname   │
            │ provider   │   ← LOCAL / GOOGLE / NAVER
            │ provider_id│
            └─────┬──────┘
                  │ 1:N
        ┌─────────┼──────────────────────────────┐
        │         │                              │
        │         │                              │
   ┌────▼───┐ ┌───▼────────────┐  ┌──────────────▼────────┐
   │articles│ │ saved_articles │  │article_read_histories │
   │────────│ │ ────────────── │  │ ───────────────────── │
   │id (PK) │ │ id, member_id  │  │ id, member_id         │
   │category│ │ article_id     │  │ article_id            │
   │title   │ │ (member, art)  │  │                       │
   │content │ │  unique        │  │                       │
   │writer  │ └────────────────┘  └───────────────────────┘
   │ NULL=  │       │ N:1                  │ N:1
   │ 시스템 │       └──→ articles ←────────┘
   │ 수집   │
   │ vs    │   ┌──────────────────┐
   │ 사용자│ 1:1│ article_summaries│   ← AI 요약 (3줄 + 핵심 3개)
   └────┬──┘ ──▶│  ──────────────  │
        │      │ summary_line1~3  │
        │      │ key_point1~3     │
        │      │ summary_source   │
        │      └──────────────────┘
        │
        └─ category, view_count, published_at으로 정렬/필터

  ┌──────────────────┐  ┌──────────────────────┐  ┌────────────────┐
  │ refresh_tokens   │  │ oauth_exchange_codes │  │ search_logs    │
  │ ──────────────── │  │ ──────────────────── │  │ ────────────── │
  │ token_hash (UQ)  │  │ code_hash (UQ)       │  │ keyword        │
  │ member_id (FK)   │  │ member_id (FK)       │  │ searched_at    │
  │ expires_at       │  │ expires_at (60초)    │  └────────────────┘
  │ revoked_at       │  │ redeemed_at          │
  └──────────────────┘  └──────────────────────┘
```

## 보안 설계

이 프로젝트에서 의도적으로 신경 쓴 보안 항목들이다.

### 1. SSRF (Server-Side Request Forgery) 방어
**문제**: 사용자가 입력한 URL로 서버가 외부 요청을 보내는 구조 → 내부망 IP로 요청해 AWS metadata, 내부 서비스 등을 조회당할 수 있다.

**대응**:
- URL 스킴 화이트리스트: `http`, `https`만 허용
- DNS resolve 후 모든 IP에 대해 차단 검증:
  - `isLoopbackAddress` (127.x, ::1)
  - `isLinkLocalAddress` (169.254.x — AWS metadata 포함)
  - `isSiteLocalAddress` (10.x, 172.16-31.x, 192.168.x)
  - `isAnyLocalAddress`, `isMulticastAddress`
- 리다이렉트는 수동 추적: hop마다 위 검증 재실행 (한쪽 우회 차단)
- 최대 3 hop 제한

**테스트 케이스**: `127.0.0.1/`, `169.254.169.254/latest/meta-data/`, `10.0.0.1/` 등 → 모두 거부

### 2. Refresh Token Rotation + 재사용 탐지
**문제**: stateless JWT는 발급 후 만료까지 무효화 불가. 탈취되면 만료 전까지 무한 access token 발급 가능.

**대응**:
- refresh token의 SHA-256 해시만 DB에 저장 (raw token 미저장 → DB 유출 시에도 활성 토큰 안전)
- `/refresh` 호출 시:
  1. 해시 조회 → 활성 여부 확인
  2. 즉시 `revoked_at` 기록 (rotation)
  3. 새 토큰 발급 + DB 등록
- **재사용 탐지**: 이미 폐기된 토큰이 다시 들어오면 → 해당 회원의 모든 활성 토큰 일괄 폐기 (정상 사용자도 강제 로그아웃하더라도 탈취 의심 상황에서 안전 우선)
- 로그아웃 시 서버 측에서 해당 토큰 폐기 (기존 stateless 구조는 쿠키만 지웠음)

### 3. OAuth Token URL 노출 방지
**문제**: OAuth 성공 후 access token을 `/?token={JWT}` query로 전달하면 access log, 프록시, 브라우저 history, referrer로 누출 가능.

**대응**: 1회용 교환 코드 패턴
- 32바이트 SecureRandom 코드 (DB에 SHA-256 해시만, 60초 수명)
- 프론트가 `POST /api/members/oauth/exchange { code }` → access token 수령
- code는 즉시 redeem → 재사용 불가
- 프론트는 URL을 즉시 `history.replaceState`로 정리

### 4. iframe Sandbox + URL Allowlist
**문제**: 사용자가 등록한 영상 임베드 URL이 iframe `src`로 들어가면 외부 페이지 임베드 → 클릭재킹/피싱/추적 위험.

**대응**:
- 백엔드 검증: `videoEmbedUrl`은 `tv.naver.com/embed/`, `youtube.com/embed/`, `youtube-nocookie.com/embed/` 도메인만 허용
- 일반 URL: `http://` 또는 `https://`로 시작해야 함
- iframe에 `sandbox="allow-scripts allow-presentation allow-popups allow-popups-to-escape-sandbox"` + `referrerPolicy="no-referrer"`

### 5. Rate Limiting & 외부 API 보호
- AI 요약 재생성: 같은 기사당 30초 cooldown (인메모리 ConcurrentHashMap)
- 프론트 재생성 버튼에 confirm 다이얼로그
- AI API 응답 코드별 친절한 에러 메시지 (429, 503, 504)

### 6. 페이지네이션 안전 가드
- `page` < 0 → 0으로 클램프, `size`는 1~50으로 제한
- 거대값 요청으로 인한 메모리/DB 부하 차단

### 7. JWT 운영 시크릿 강제
- prod 환경에서 기본 dev 시크릿 사용 시 **앱 시작 차단** (운영자가 환경변수 설정 누락하지 않도록)

### 8. Nginx 보안 헤더 (HTTPS)
- `X-Frame-Options: SAMEORIGIN`: 클릭재킹 방지
- `X-Content-Type-Options: nosniff`: MIME-sniffing 방지
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`
- `Strict-Transport-Security: max-age=31536000` (HSTS 1년)

## 주요 트러블슈팅 / 의사결정 기록

### 1. AI 요약 실패 시 페이지 전체가 무너지던 UX 버그
**증상**: 요약 재생성 시 cooldown / 429 / 503 에러가 반환되면 기사 본문까지 사라지고 에러 메시지만 표시.

**원인**: `useDetailPageData.regenerateSummary`가 실패 시 `setArticleErrorMessage`를 호출했는데, 이 상태는 페이지 전체 에러용. 요약 카드 안에서만 표시했어야 함.

**해결**: 요약 에러 상태(`summaryErrorMessage`)를 페이지 에러와 분리. 요약 카드 안에 인라인 에러 박스로 표시 + 생성 중 버튼 비활성화. 기존 요약과 본문은 그대로 유지.

### 2. JWT 같은 밀리초 발급 시 unique 제약 위반
**증상**: refresh token DB 도입 후 테스트에서 unique constraint violation 발생.

**원인**: 같은 사용자에게 같은 밀리초에 발급된 JWT는 바이트가 동일 → SHA-256 해시도 동일 → unique 위반.

**해결**: JWT 클레임에 `jti`(UUID)를 추가해 매 토큰을 고유하게 만들었다. 이는 RFC 7519의 표준 클레임이기도 함.

### 3. `@Modifying` UPDATE 후 stale entity 문제
**증상**: 재사용 탐지 테스트 — `revokeAllForMember()` 호출 후 같은 트랜잭션에서 다시 조회하면 변경 전 상태로 보임.

**원인**: Hibernate 1차 캐시. JPQL UPDATE는 DB는 갱신하지만 영속성 컨텍스트의 엔티티는 그대로.

**해결**: `@Modifying(clearAutomatically = true, flushAutomatically = true)`로 UPDATE 후 컨텍스트 비우기.

### 4. Spring Boot 4의 Flyway auto-config 제거
**증상**: Spring Boot 3 → 4 업그레이드 후 `spring.flyway.*` 자동 설정이 동작 안 함.

**해결**: `FlywayConfig`에 수동으로 `Flyway` bean 등록 + `BeanDefinitionRegistryPostProcessor`로 EntityManagerFactory보다 먼저 실행되도록 의존성 강제. `@ConditionalOnProperty`로 테스트에서 비활성화 존중.

### 5. .env 변경이 컨테이너에 반영 안 됨
**증상**: EC2의 `.env` 파일에 `NAVER_LOGIN_CLIENT_ID` 추가 후 `docker compose restart`했지만 컨테이너에는 환경변수가 없음.

**원인**: prod 컴포즈가 `environment:` 섹션에 변수를 명시 나열하는 구조. 목록에 없는 변수는 `.env`에 있어도 전달 안 됨.

**해결**: 컴포즈 파일의 `app.environment`에 변수를 추가. `docker compose up -d --force-recreate`로 컨테이너 재생성 (단순 restart는 환경변수 재로딩 안 함).

### 6. 자동수집 기사 무한 적재 우려
**문제**: 30분마다 카테고리당 5건씩 수집 → 1년 ≈ 36만 건. 검색 품질 저하 + AI 요약 비용 낭비 가능.

**의사결정**: "최신 자동수집 + 미열람 90일 = 자동삭제, 누가 저장하면 영구 보존" 정책으로 결정. `writer IS NULL AND created_at < cutoff AND saved_articles에 없음` 조건. 사용자 직접 작성 기사는 절대 안 건드림.

**효과**: 면접에서 *"운영 부담 고려해서 데이터 라이프사이클 정책 분리"*로 어필 가능.

## 프로젝트 성격 및 사용 제한 (중요)

- **학습용/포트폴리오 프로젝트**입니다. 상업적 서비스가 아닙니다.
- **AI 요약**: NVIDIA API Catalog의 Trial 크레딧을 사용합니다. NVIDIA 약관상 Trial은 평가/데모 목적이며, 실제 production 운영에는 별도 구독이 필요합니다. 본 프로젝트는 데모/평가 범위 내에서만 운영합니다.
- **뉴스 본문**: 외부 뉴스 기사의 본문을 추출하여 AI 요약을 생성합니다. 모든 기사 상세에 **원문 출처 링크**를 함께 제공하며, 비상업적 학습 목적으로만 사용합니다. 저작권자의 요청이 있을 경우 즉시 삭제할 수 있도록 운영자가 관리합니다.
- **사용량 보호**: 외부 AI 호출 폭주를 막기 위해 같은 기사당 30초 cooldown과 재생성 확인 다이얼로그를 적용합니다.

## 알려진 제한 사항

- **기사 추출**: 사이트별 HTML 구조에 의존하므로 구조 변경 시 추출 실패 가능
- **nip.io 도메인**: IP 기반 임시 도메인. 실서비스에는 정식 도메인 필요
- **운영 모니터링**: Actuator health 엔드포인트만 존재. 로그 수집/알림 미구축
- **AI 요약**: NVIDIA NIM API 무료 크레딧 기반. 크레딧 소진 시 mock 모드로 전환 필요
- **AI cooldown 저장소**: 인메모리 Map 기반이라 서버 재시작/스케일아웃 시 초기화됩니다 (단일 인스턴스 운영 전제)
- **Refresh token 정리**: 만료된 token도 DB에 남아있음 (volume이 작아 별도 cleanup 스케줄러 미구현)

## 프로젝트 구조

```
src/main/java/.../newssummaryproject/
├── domain/
│   ├── member/          # 회원, refresh token, OAuth code, 저장/읽기/관심
│   │   └── dto/
│   └── news/            # 기사 CRUD, AI 요약, 검색, 추천, 트렌딩, 자동수집, retention
│       └── dto/
└── global/
    ├── api/             # Health check, 외부 API 클라이언트
    ├── config/          # Security, JWT, OAuth2, JPA Auditing, Scheduling, Flyway
    ├── exception/       # 전역 예외 처리
    ├── init/            # 개발용 시드 데이터 (@Profile("dev"))
    └── view/            # SPA 라우팅 지원

frontend/src/
├── app/                 # 라우팅, 전역 상태 훅 (useAuthState 등)
├── components/          # UI 컴포넌트 (modal, layout, home, detail, mypage)
├── features/            # 페이지별 데이터 로딩 훅
├── pages/               # 페이지 컴포넌트
└── shared/              # API 클라이언트, 타입, 유틸, 상수
```

## 테스트 전략

- `@SpringBootTest` + H2 인메모리 DB로 통합 테스트
- `@Tag("integration")`으로 외부 의존 테스트 분리 (뉴스 사이트 크롤링)
- CI에서 단위 테스트는 필수 게이트, 통합 테스트는 별도 실행 (외부 사이트 변경에 유연)
- 보안 핵심 동작 테스트:
  - SSRF: loopback / AWS metadata / private IP 거부
  - Refresh rotation: 이전 토큰 재사용 시 401
  - 재사용 탐지: revoke된 토큰 재사용 시 회원 전체 토큰 일괄 폐기
  - 로그아웃: 서버 측 토큰 폐기 확인
  - OAuth code 1회 사용 후 재사용 거부
  - Retention 정리: 사용자 작성 / 저장된 기사는 안 건드림

## CI/CD

- **CI**: GitHub Actions → Gradle build + test → 통합 테스트(별도)
- **CD**: CI 성공 → EC2에 SSH → `docker compose up --build` → 헬스 체크
- Docker Compose: 개발용(`docker-compose.yml`)과 운영용(`docker-compose.prod.yml`) 분리
- Nginx: 인증서 유무에 따라 HTTP/HTTPS 자동 선택 (템플릿 기반)
- 컨테이너 timezone: `TZ=Asia/Seoul` 명시 (스케줄러 기준 시각 보장)

## 실행 방법

### 로컬 개발 (Gradle 직접 실행)

```bash
# 사전 조건: Java 17, Node.js 20, MySQL 실행 중

# 1. 환경 변수 파일 생성
cp .env.example .env
# .env에서 DB_PASSWORD 등 설정

# 2. 실행 (프론트엔드 빌드 자동 포함)
./gradlew bootRun

# http://localhost:8080 접속
```

### Docker로 실행

```bash
# 개발 환경 (MySQL + App)
docker compose up -d
# http://localhost:8080 접속
```

### 프론트엔드 개발 서버 (HMR)

```bash
# 터미널 1: 백엔드
./gradlew bootRun

# 터미널 2: 프론트엔드 (Vite dev server)
cd frontend && npm install && npm run dev
# http://localhost:5173 (API는 프록시로 8080 전달)
```

### Google 로그인 활성화 (선택)

1. [Google Cloud Console](https://console.cloud.google.com/)에서 OAuth2 클라이언트 생성
2. 승인된 리디렉션 URI: `http://localhost:8080/login/oauth2/code/google`
3. `.env`에 추가:
   ```
   GOOGLE_CLIENT_ID=your-client-id
   GOOGLE_CLIENT_SECRET=your-client-secret
   ```

### Naver 로그인 활성화 (선택)

1. [네이버 개발자센터](https://developers.naver.com/apps)에서 "Application 등록"
2. **사용 API: "네이버 로그인"** 체크 (검색 API와 다른 종류)
3. 제공 정보: 이메일, 이름 체크 (이메일은 **필수**로 설정)
4. Callback URL: `http://localhost:8080/login/oauth2/code/naver`
5. `.env`에 추가:
   ```
   NAVER_LOGIN_CLIENT_ID=your-client-id
   NAVER_LOGIN_CLIENT_SECRET=your-client-secret
   ```

### 자동 뉴스 수집 활성화 (선택)

```bash
# .env
NAVER_CLIENT_ID=검색_API_키
NAVER_CLIENT_SECRET=검색_API_시크릿
NAVER_NEWS_COLLECT_ENABLED=true     # 기본 false
NAVER_NEWS_COLLECT_SIZE=5            # 카테고리당 수집 개수
ARTICLE_RETENTION_DAYS=90            # 미열람 자동삭제 (0=비활성)
```

## 테스트

```bash
# 단위 테스트 (외부 의존 제외)
./gradlew test

# 통합 테스트 (외부 뉴스 사이트 크롤링)
./gradlew integrationTest

# 전체 빌드 (프론트엔드 + 백엔드 + 테스트)
./gradlew build
```

## API 문서

개발 환경에서 Swagger UI 확인 가능: http://localhost:8080/swagger-ui.html

### 주요 API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/members/signup` | 회원가입 |
| `POST` | `/api/members/login` | 로그인 |
| `POST` | `/api/members/refresh` | 토큰 재발급 (rotation + 재사용 탐지) |
| `POST` | `/api/members/logout` | 로그아웃 (서버 측 토큰 폐기) |
| `POST` | `/api/members/oauth/exchange` | OAuth 1회용 코드 → access token 교환 |
| `GET` | `/api/members/me` | 내 정보 조회 |
| `GET` | `/oauth2/authorization/google` | Google 로그인 시작 |
| `GET` | `/oauth2/authorization/naver` | Naver 로그인 시작 |
| `GET` | `/api/articles` | 기사 목록 (page/size 자동 클램프) |
| `GET` | `/api/articles/{id}` | 기사 상세 |
| `POST` | `/api/articles` | 기사 등록 (URL 검증) |
| `PATCH` | `/api/articles/{id}` | 기사 수정 (작성자만) |
| `DELETE` | `/api/articles/{id}` | 기사 삭제 (작성자만) |
| `POST` | `/api/articles/extract` | URL에서 기사 추출 (SSRF 방어) |
| `POST` | `/api/articles/{id}/generate-summary` | AI 요약 생성 (30초 cooldown) |
| `GET` | `/api/articles/search` | 검색 |
| `GET` | `/api/articles/recommendations` | 추천 기사 |
| `GET` | `/api/articles/trending` | 트렌딩 |
| `GET` | `/api/mypage` | 마이페이지 |

## 환경 변수

| 변수 | 필수 | 설명 | 기본값 |
|------|------|------|--------|
| `DB_URL` | O | MySQL 접속 URL | - |
| `DB_USERNAME` | O | MySQL 사용자명 | - |
| `DB_PASSWORD` | O | MySQL 비밀번호 | - |
| `JWT_SECRET` | O (prod) | JWT 서명 키 (32자 이상) | dev용 기본값 |
| `JWT_ACCESS_EXPIRATION_HOURS` | - | Access token 수명 | `1` |
| `JWT_REFRESH_EXPIRATION_DAYS` | - | Refresh token 수명 | `7` |
| `AI_API_KEY` | - | NVIDIA AI API 키 | - |
| `AI_MOCK_ENABLED` | - | Mock 요약 사용 여부 | `false` |
| `GOOGLE_CLIENT_ID` | - | Google OAuth2 클라이언트 ID | - |
| `GOOGLE_CLIENT_SECRET` | - | Google OAuth2 시크릿 | - |
| `NAVER_LOGIN_CLIENT_ID` | - | 네이버 로그인 OAuth2 ID | - |
| `NAVER_LOGIN_CLIENT_SECRET` | - | 네이버 로그인 OAuth2 시크릿 | - |
| `NAVER_CLIENT_ID` | - | 네이버 검색 API ID (뉴스 수집용, 로그인과 별개) | - |
| `NAVER_CLIENT_SECRET` | - | 네이버 검색 API 시크릿 | - |
| `NAVER_NEWS_COLLECT_ENABLED` | - | 자동수집 ON/OFF | `false` |
| `NAVER_NEWS_COLLECT_SIZE` | - | 카테고리당 수집 개수 | `5` |
| `ARTICLE_RETENTION_DAYS` | - | 미열람 자동수집 기사 보관 기간 (0=비활성) | `0` |
| `APP_HOST` | O (prod) | 운영 도메인 (Nginx/인증서용) | - |

## 기본 시드 데이터

DB가 비어 있을 때 개발 환경(`dev` 프로필)에서 자동으로 샘플 데이터가 들어갑니다.

- 테스트 계정: `test@test.com` / `1234`
- 카테고리별 샘플 기사 및 AI 요약 데이터
