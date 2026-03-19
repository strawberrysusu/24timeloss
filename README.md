# NewsPick - 뉴스 개인화/저장 서비스

뉴스 기사를 읽고, 저장하고, 관심 분야를 설정해서 개인화된 뉴스 피드를 받을 수 있는 백엔드 API 서비스입니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.3
- Spring Data JPA + Hibernate
- MySQL 8.0 (운영) / H2 (테스트)
- Mustache (임시 프론트엔드)
- Gradle

## 주요 기능

| 기능 | 설명 |
|------|------|
| 회원가입 / 로그인 | 이메일 기반, BCrypt 비밀번호 암호화, 세션 인증 |
| 기사 목록 / 상세 | 카테고리 필터, 페이지네이션, 조회수 추적 |
| 키워드 검색 | 제목+본문 검색, 인기 검색어 집계 |
| 기사 저장 | 북마크 기능, 중복 저장 방지 |
| 관심 분야 | 7개 카테고리 중 선택, 추천 기사에 반영 |
| 읽기 기록 | 자동 저장, 연속 방문 스트릭 계산 |
| 마이페이지 | 읽은 기사 수, 스트릭, 관심 분야, 저장 기사 종합 |
| 기사 등록 | 로그인 사용자만 가능, AI 요약 선택 입력 |
| 트렌딩 | 조회수 기반 인기 기사 |
| AI 브리핑 | 최신 기사 요약 조합 |

## 실행 방법

### 1. MySQL 준비

```sql
CREATE DATABASE newspick DEFAULT CHARACTER SET utf8mb4;
```

### 2. 환경변수 설정

이 프로젝트는 **OS 환경변수**에서 DB 접속 정보를 읽습니다.
`.env` 파일을 자동으로 로드하지 않으므로, 아래 중 한 가지 방식으로 설정하세요.

**방법 A) IDE 실행 설정에 환경변수 추가** (IntelliJ 권장)

Run Configuration > Environment variables 에 아래 값을 추가합니다.

```
DB_URL=jdbc:mysql://localhost:3306/newspick?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=본인_비밀번호
```

**방법 B) 터미널에서 직접 지정 후 실행**

```bash
# Linux / Mac
export DB_URL="jdbc:mysql://localhost:3306/newspick?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
export DB_USERNAME=root
export DB_PASSWORD=본인_비밀번호
./gradlew bootRun

# Windows PowerShell
$env:DB_URL="jdbc:mysql://localhost:3306/newspick?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="본인_비밀번호"
./gradlew bootRun
```

> 환경변수를 설정하지 않으면 `application.properties`의 기본값(`root` / `1234`)이 사용됩니다.

### 3. 실행

```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:8080` 으로 접속합니다.

### 4. 테스트

```bash
./gradlew test
```

테스트는 H2 메모리 DB를 사용하므로 MySQL 없이도 실행됩니다.

## API 명세

### 회원 API

| 메서드 | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/members/signup` | 회원가입 | X |
| POST | `/api/members/login` | 로그인 | X |
| POST | `/api/members/logout` | 로그아웃 | O |
| GET | `/api/members/me` | 내 정보 조회 | O |

### 기사 API

| 메서드 | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/articles?category=IT_SCIENCE&page=0&size=10` | 기사 목록 | X |
| GET | `/api/articles/{id}` | 기사 상세 | X |
| GET | `/api/articles/search?keyword=AI&page=0&size=10` | 키워드 검색 | X |
| GET | `/api/articles/recommendations?page=0&size=10` | 추천 기사 | 선택 |
| GET | `/api/articles/{id}/related` | 관련 기사 | X |
| GET | `/api/articles/trending` | 트렌딩 | X |
| GET | `/api/articles/briefing` | AI 브리핑 | X |
| GET | `/api/articles/popular-keywords` | 인기 검색어 | X |
| POST | `/api/articles` | 기사 등록 | O (본인 작성자 설정) |
| PATCH | `/api/articles/{id}` | 기사 수정 | O (작성자만) |
| DELETE | `/api/articles/{id}` | 기사 삭제 | O (작성자만) |

### 마이페이지 API

| 메서드 | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/mypage` | 마이페이지 종합 | O |
| GET | `/api/mypage/saved-article-ids` | 저장한 기사 ID 목록 | O |
| POST | `/api/mypage/saved-articles/{articleId}` | 기사 저장 | O |
| DELETE | `/api/mypage/saved-articles/{articleId}` | 기사 저장 취소 | O |
| POST | `/api/mypage/interests/{category}` | 관심 분야 추가 | O |
| DELETE | `/api/mypage/interests/{category}` | 관심 분야 삭제 | O |
| POST | `/api/mypage/read-history/{articleId}` | 읽기 기록 저장 | O |
| GET | `/api/mypage/read-history` | 읽은 기사 목록 | O |
| PATCH | `/api/mypage/nickname` | 닉네임 변경 | O |
| PATCH | `/api/mypage/password` | 비밀번호 변경 | O |

### 카테고리 목록

`POLITICS` | `ECONOMY` | `SOCIETY` | `IT_SCIENCE` | `WORLD` | `SPORTS` | `ENTERTAINMENT`

## ERD

```
members (1) ──── (N) saved_articles (N) ──── (1) articles
   │                                               │
   ├── (N) member_interests                        ├── (1) article_summaries
   │                                               │
   ├── (N) article_read_histories (N) ─────── (1) ─┘
   │
   └── (1) ──── (N) articles (writer_id)     search_logs (독립)
```

## 프로젝트 구조

```
src/main/java/
├── domain/
│   ├── common/          # BaseTimeEntity (생성/수정 시간 자동 관리)
│   ├── member/          # 회원, 저장기사, 관심분야, 읽기기록, 마이페이지
│   └── news/            # 기사, AI요약, 검색로그, 카테고리
└── global/
    ├── config/          # JPA Auditing, PasswordEncoder
    ├── exception/       # 커스텀 예외, 글로벌 예외 핸들러
    ├── init/            # 샘플 데이터 초기화
    └── view/            # 페이지 렌더링
```
