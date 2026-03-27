# 24timeloss

뉴스를 모아 보고, 기사 URL에서 내용을 추출하고, AI 요약과 개인화 기능까지 붙인 Spring Boot 기반 뉴스 웹앱입니다.

현재 기준으로 이 프로젝트는 다음 흐름을 지원합니다.

- 기사 목록, 상세, 검색, 추천, 트렌딩, 브리핑 조회
- 회원가입 / 로그인 / 로그아웃 / 세션 기반 인증
- 마이페이지: 관심 카테고리, 저장한 기사, 읽기 기록, 닉네임/비밀번호 변경
- 기사 URL 추출 후 등록
- AI 요약 생성
- 네이버 뉴스 내부의 네이버 플레이어 영상 임베드 재생

## 핵심 기능

### 1. 기사 수집 및 등록

- 기사 URL을 입력하면 제목, 출처, 본문, 썸네일을 자동 추출합니다.
- 본문 추출은 `Jsoup` + `Readability4J` fallback 구조를 사용합니다.
- 추출 결과를 기반으로 직접 기사 등록이 가능합니다.

### 2. 영상 처리 정책

현재 영상은 아래 정책으로 동작합니다.

- 네이버 뉴스 기사 안에서 네이버 플레이어 임베드가 확인되면 사이트 내부에서 재생
- 그 외 언론사 영상이거나 임베드 URL을 안정적으로 만들 수 없으면 썸네일 + 원문 링크로 대체

즉, "모든 언론사 영상 재생"이 목표가 아니라, 현재는 "네이버 뉴스 안의 네이버 플레이어"를 안전하게 지원하는 방향입니다.

### 3. AI 요약

- 기사 상세에서 3줄 요약 + 핵심 포인트 3개를 생성합니다.
- OpenAI 호환 Chat Completions API 형식의 모델을 사용할 수 있습니다.
- 기본 설정은 NVIDIA NIM 호환 엔드포인트 기준입니다.
- `AI_MOCK_ENABLED=true`로 두면 실제 API 호출 없이 mock 요약으로 테스트할 수 있습니다.

### 4. 개인화

- 관심 카테고리 기반 추천 기사
- 저장한 기사 관리
- 읽기 기록 저장 및 조회
- 인기 검색어 집계

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| View | Mustache |
| ORM | Spring Data JPA / Hibernate |
| DB | MySQL 8 / H2(Test) |
| Parsing | Jsoup, Readability4J |
| Docs | springdoc-openapi |
| Build | Gradle |

## 프로젝트 구조

```text
src/main/java/org/example/newssummaryproject
├─ domain
│  ├─ member   # 회원, 저장 기사, 읽기 기록, 관심 카테고리, 마이페이지
│  └─ news     # 기사, 요약, 검색 로그, 기사 추출, 추천/트렌딩/브리핑
└─ global
   ├─ api      # health check
   ├─ config   # JPA auditing, password encoder 등
   ├─ exception
   ├─ init     # 개발용 시드 데이터
   └─ view     # index.mustache 진입 페이지
```

## 실행 방법

### 1. 준비물

- Java 17
- MySQL 8.x

### 2. 데이터베이스 생성

```sql
CREATE DATABASE newspick DEFAULT CHARACTER SET utf8mb4;
```

### 3. 환경 변수 설정

프로젝트는 환경 변수로 DB와 AI 설정을 읽습니다.

#### 필수

| 변수명 | 설명 | 예시 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/newspick?...` |
| `DB_USERNAME` | DB 계정 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | `1234` |

#### 선택

| 변수명 | 설명 | 기본값 |
| --- | --- | --- |
| `AI_API_KEY` | AI 요약 API 키 | 빈 값 |
| `AI_MODEL` | 사용할 모델명 | `meta/llama-3.3-70b-instruct` |
| `AI_BASE_URL` | OpenAI 호환 Chat Completions URL | NVIDIA NIM URL |
| `AI_TIMEOUT` | AI 요청 타임아웃(초) | `30` |
| `AI_MOCK_ENABLED` | mock 요약 사용 여부 | `false` |

PowerShell 예시:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/newspick?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="1234"
$env:AI_API_KEY=""
$env:AI_MOCK_ENABLED="true"
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

실행 후 접속:

- 메인 화면: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/health`

## 기본 시드 데이터

DB가 비어 있을 때 개발용 데이터가 자동으로 들어갑니다.

- 테스트 계정
  - 이메일: `test@test.com`
  - 비밀번호: `1234`
- 카테고리별 샘플 기사 및 요약 데이터

## 테스트

```bash
./gradlew test
```

현재 테스트는 통과 기준으로 관리되고 있습니다.

참고:

- 일부 기사 추출 테스트는 외부 뉴스 페이지 구조 변화의 영향을 받을 수 있습니다.
- 네이버 / 언론사 HTML 구조가 바뀌면 추출 로직도 함께 보정해야 합니다.

## 주요 API

### 회원

- `POST /api/members/signup`
- `POST /api/members/login`
- `POST /api/members/logout`
- `GET /api/members/me`

### 기사

- `GET /api/articles`
- `GET /api/articles/{id}`
- `GET /api/articles/search`
- `GET /api/articles/recommendations`
- `GET /api/articles/{id}/related`
- `GET /api/articles/trending`
- `GET /api/articles/briefing`
- `GET /api/articles/popular-keywords`
- `POST /api/articles/extract`
- `POST /api/articles`
- `PATCH /api/articles/{id}`
- `DELETE /api/articles/{id}`
- `POST /api/articles/{id}/generate-summary`

### 마이페이지

- `GET /api/mypage`
- `GET /api/mypage/saved-article-ids`
- `POST /api/mypage/saved-articles/{articleId}`
- `DELETE /api/mypage/saved-articles/{articleId}`
- `POST /api/mypage/interests/{category}`
- `DELETE /api/mypage/interests/{category}`
- `POST /api/mypage/read-history/{articleId}`
- `GET /api/mypage/read-history`
- `PATCH /api/mypage/nickname`
- `PATCH /api/mypage/password`

## 카테고리

- `POLITICS`
- `ECONOMY`
- `SOCIETY`
- `IT_SCIENCE`
- `WORLD`
- `SPORTS`
- `ENTERTAINMENT`

## 현재 한계

- 영상 재생은 네이버 플레이어 중심 정책으로 제한되어 있습니다.
- 기사 추출은 외부 사이트 HTML 구조 변화에 민감합니다.
- 프론트는 현재 `index.mustache` 중심의 단일 템플릿 구조라, UI 코드가 한 파일에 많이 모여 있습니다.
- 세션 기반 인증 구조이므로, 추후 운영 배포 단계에서는 HTTPS / 프록시 / 쿠키 설정을 함께 고려해야 합니다.

## 다음 정리 후보

- 프론트 단일 템플릿 분리
- 테스트를 fixture 기반으로 더 안정화
- Docker / CI/CD / HTTPS 적용
- Redis 캐시 등 운영 환경 실험

