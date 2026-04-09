# NewsPick - AI 뉴스 요약 서비스

뉴스 기사를 등록하면 AI가 3줄 요약과 핵심 포인트를 자동 생성하는 웹 서비스입니다.

> **서비스 주소**: https://43.202.121.142.nip.io
> 
> **테스트 계정**: `test@test.com` / `1234` (dev 환경 시드 데이터)

## 주요 기능

- **AI 뉴스 요약**: URL로 기사를 등록하면 본문을 자동 추출(Jsoup + Readability4J)하고 AI(LLaMA 3.3 70B)가 3줄 요약 + 핵심 포인트 생성
- **카테고리별 뉴스 피드**: 정치, 경제, 사회, IT/과학, 세계, 스포츠, 연예 카테고리 분류
- **개인화**: 관심 카테고리 설정, 기사 저장, 읽기 기록, 추천/트렌딩/브리핑
- **인증**: 이메일/비밀번호 + Google OAuth2 로그인 지원
- **영상 임베드**: 네이버 뉴스 내 네이버 플레이어 영상 재생 지원

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Backend** | Java 17, Spring Boot 4, Spring Security, Spring Data JPA |
| **Frontend** | React 19, TypeScript, Vite |
| **Database** | MySQL 8.0, Flyway (스키마 마이그레이션) |
| **인증** | JWT (Access + Refresh Token), Google OAuth2 |
| **인프라** | Docker Compose, Nginx (HTTPS), Let's Encrypt, GitHub Actions CI/CD |
| **AI** | NVIDIA API (LLaMA 3.3 70B), OpenAI Chat Completions 호환 |
| **기타** | Jsoup, Readability4J, springdoc-openapi, Lombok |

## 아키텍처

```
[Browser] ── [Nginx (HTTPS)] ── [Spring Boot + React SPA] ── [MySQL]
                                          │
                                   [NVIDIA AI API]
```

- **SPA 통합 빌드**: React 앱이 Gradle 빌드 시 `src/main/resources/static/react/`로 번들링, Spring Boot가 함께 서빙
- **Stateless 인증**: 세션 없이 JWT로 API 인증, Refresh Token은 httpOnly 쿠키로 관리

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
    ├── Jsoup으로 HTML 가져오기 (15초 타임아웃, 1회 재시도)
    ├── 사이트별 분기: 네이버뉴스 / KBS / 일반
    ├── 제목, 본문, 출처, 썸네일, 영상 URL 추출
    └── 본문 < 50자면 og:description 폴백
    ↓
사용자가 기사 등록 (POST /api/articles)
    ↓
사용자가 "AI 요약 생성" 클릭
    ↓
AiSummaryService.summarize(content)
    ├── NVIDIA NIM API 호출 (LLaMA 3.3 70B)
    ├── SSE 스트리밍으로 응답 수신
    ├── JSON 파싱 → 3줄 요약 + 핵심 포인트 3개
    └── DB에 저장 (ArticleSummary)
```

## 기술 선택 이유

| 기술 | 선택 이유 |
|------|----------|
| **Spring Boot** | Java 백엔드 표준. Security, JPA, OAuth2 등 인증/DB 관련 생태계가 잘 갖춰져 있다 |
| **JWT (Access + Refresh)** | 세션 서버 없이 Stateless 인증. Access Token은 짧게, Refresh Token은 httpOnly 쿠키로 보안과 UX를 모두 확보 |
| **Google OAuth2** | 소셜 로그인 경험. Spring Security OAuth2 Client로 서버사이드 처리하여 토큰이 프론트에 노출되지 않음 |
| **Flyway** | `ddl-auto=update`는 운영에서 위험. SQL 기반 버전 관리로 스키마 변경을 추적하고 롤백 가능 |
| **Docker Compose** | 개발/운영 환경 통일. "내 PC에서는 되는데" 문제 방지. MySQL, Nginx, App을 한 번에 구성 |
| **Nginx + Let's Encrypt** | 무료 HTTPS. 리버스 프록시로 Spring Boot를 직접 노출하지 않음 |
| **GitHub Actions CI/CD** | push만 하면 빌드→테스트→배포 자동 실행. 수동 SSH 접속 없이 배포 가능 |
| **EC2** | 프리 티어로 충분한 트래픽. Docker만 설치하면 바로 운영 가능 |
| **React SPA** | 페이지 전환 없이 빠른 UX. Spring Boot에 번들링하여 별도 프론트 서버 불필요 |

## 알려진 제한 사항

- **기사 추출**: 사이트별 HTML 구조에 의존하므로 구조 변경 시 추출 실패 가능
- **nip.io 도메인**: IP 기반 임시 도메인. 실서비스에는 정식 도메인 필요
- **운영 모니터링**: Actuator health 엔드포인트만 존재. 로그 수집/알림 미구축
- **AI 요약**: NVIDIA NIM API 무료 크레딧 기반. 크레딧 소진 시 mock 모드로 전환 필요

## 프로젝트 구조

```
src/main/java/.../newssummaryproject/
├── domain/
│   ├── member/          # 회원, 저장 기사, 읽기 기록, 관심 카테고리, 마이페이지
│   │   └── dto/
│   └── news/            # 기사 CRUD, AI 요약, 검색, 추천, 트렌딩, 브리핑
│       └── dto/
└── global/
    ├── api/             # Health check, 외부 API 클라이언트
    ├── config/          # Security, JWT, OAuth2, JPA Auditing
    ├── exception/       # 전역 예외 처리
    ├── init/            # 개발용 시드 데이터 (@Profile("dev"))
    └── view/            # SPA 라우팅 지원

frontend/src/
├── app/                 # 라우팅, 전역 상태 훅
├── components/          # UI 컴포넌트 (modal, layout, home, detail, mypage)
├── features/            # 페이지별 데이터 로딩 훅
├── pages/               # 페이지 컴포넌트
└── shared/              # API 클라이언트, 타입, 유틸, 상수
```

## 주요 구현 포인트

### JWT 인증 (Access + Refresh Token)
- **Access Token**: 응답 body로 전달 → 프론트가 localStorage 저장 → `Authorization: Bearer` 헤더로 전송
- **Refresh Token**: httpOnly 쿠키 전달 → JS 접근 불가(XSS 방어) → 브라우저 자동 전송
- **Refresh Token Rotation**: 재발급 시 refresh도 새로 교체
- **환경별 쿠키 정책**: prod=`Secure; SameSite=Strict`, dev=`SameSite=Lax`
- **운영 시크릿 강제**: prod 환경에서 기본 dev 시크릿 사용 시 앱 시작 차단

### Google OAuth2 로그인
- Spring Security OAuth2 Client로 서버 사이드 인증 처리
- 기존 이메일 회원이면 자동 로그인, 신규면 자동 가입
- OAuth2 인증 후 JWT 발급 → 동일한 토큰 기반 인증 사용
- Google 자격증명 미설정 시 OAuth2 자동 비활성화 (기존 인증 정상 동작)

### 테스트 전략
- `@SpringBootTest` + H2 인메모리 DB로 통합 테스트
- `@Tag("integration")`으로 외부 의존 테스트 분리 (뉴스 사이트 크롤링)
- CI에서 단위 테스트는 필수 게이트, 통합 테스트는 별도 실행

### CI/CD
- **CI**: GitHub Actions → Gradle build + test → 통합 테스트(별도)
- **CD**: CI 성공 → EC2에 SSH → `docker compose up --build` → 헬스 체크
- Docker Compose: 개발용(`docker-compose.yml`)과 운영용(`docker-compose.prod.yml`) 분리
- Nginx: 인증서 유무에 따라 HTTP/HTTPS 자동 선택 (템플릿 기반)

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
| `POST` | `/api/members/refresh` | 토큰 재발급 |
| `POST` | `/api/members/logout` | 로그아웃 |
| `GET` | `/api/members/me` | 내 정보 조회 |
| `GET` | `/api/articles` | 기사 목록 |
| `GET` | `/api/articles/{id}` | 기사 상세 |
| `POST` | `/api/articles` | 기사 등록 |
| `POST` | `/api/articles/extract` | URL에서 기사 추출 |
| `POST` | `/api/articles/{id}/generate-summary` | AI 요약 생성 |
| `GET` | `/api/articles/search` | 검색 |
| `GET` | `/api/articles/recommendations` | 추천 기사 |
| `GET` | `/api/articles/trending` | 트렌딩 |
| `GET` | `/api/mypage` | 마이페이지 |

## 환경 변수

| 변수 | 필수 | 설명 | 기본값 |
|------|------|------|--------|
| `DB_PASSWORD` | O | MySQL 비밀번호 | - |
| `JWT_SECRET` | O (prod) | JWT 서명 키 (32자 이상) | dev용 기본값 |
| `AI_API_KEY` | - | NVIDIA AI API 키 | - |
| `AI_MOCK_ENABLED` | - | Mock 요약 사용 여부 | `false` |
| `GOOGLE_CLIENT_ID` | - | Google OAuth2 클라이언트 ID | - |
| `GOOGLE_CLIENT_SECRET` | - | Google OAuth2 시크릿 | - |
| `APP_HOST` | O (prod) | 운영 도메인 (Nginx/인증서용) | - |

## 기본 시드 데이터

DB가 비어 있을 때 개발 환경(`dev` 프로필)에서 자동으로 샘플 데이터가 들어갑니다.

- 테스트 계정: `test@test.com` / `1234`
- 카테고리별 샘플 기사 및 AI 요약 데이터
