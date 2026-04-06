# Frontend Component Map

현재 문서는 제거된 레거시 `index.mustache` 구조를 기준으로, React 마이그레이션 시 어떤 컴포넌트로 나눴는지 기록한 매핑이다.

## 목표 구조

```text
frontend/src
├── app
│   ├── AppShell.tsx
│   └── component-map.ts
├── components
│   ├── detail
│   ├── home
│   ├── layout
│   ├── modal
│   └── mypage
├── pages
│   ├── DetailPage.tsx
│   ├── HomePage.tsx
│   └── MyPage.tsx
└── shared
    ├── constants
    └── types
```

## 기존 화면 섹션 → React 컴포넌트

- `HEADER` → `components/layout/SiteHeader.tsx`
- `샘플 데이터 안내 배너` → `components/home/SampleBanner.tsx`
- `PAGE: HOME / Hero` → `components/home/HeroSection.tsx`
- `PAGE: HOME / Category tabs` → `components/home/CategoryTabs.tsx`
- `PAGE: HOME / News feed` → `components/home/NewsFeedSection.tsx`
- `PAGE: HOME / Sidebar` → `components/home/HomeSidebar.tsx`
- `PAGE: DETAIL / Breadcrumb` → `components/detail/DetailBreadcrumb.tsx`
- `PAGE: DETAIL / Article` → `components/detail/ArticleDetailSection.tsx`
- `PAGE: DETAIL / Related news` → `components/detail/RelatedNewsSection.tsx`
- `PAGE: MYPAGE / Profile sidebar` → `components/mypage/ProfileSidebar.tsx`
- `PAGE: MYPAGE / Stats` → `components/mypage/MyPageOverview.tsx`
- `PAGE: MYPAGE / Interests` → `components/mypage/InterestSettingsSection.tsx`
- `PAGE: MYPAGE / Saved news` → `components/mypage/SavedArticlesSection.tsx`
- `PAGE: MYPAGE / Read history` → `components/mypage/ReadHistorySection.tsx`
- `PAGE: MYPAGE / Account settings` → `components/mypage/AccountSettingsSection.tsx`
- `MODALS / login + signup` → `components/modal/AuthModal.tsx`
- `MODALS / article editor` → `components/modal/ArticleEditorModal.tsx`
- `FOOTER` → `components/layout/SiteFooter.tsx`

## 상태 분리 기준

- 전역 앱 상태
  - 현재 페이지, 로그인 사용자, 저장 기사 ID 집합, 현재 기사 ID
- 홈 화면 상태
  - 현재 카테고리, 검색어, 기사 목록 페이지네이션, 브리핑, 트렌딩
- 상세 화면 상태
  - 기사 상세 데이터, 관련 기사, TTS 상태
- 마이페이지 상태
  - 프로필, 통계, 관심사, 저장 기사, 읽기 기록, 계정 설정 폼
- 모달 상태
  - 인증 모달 모드, 기사 등록/수정 모달 열림 여부

## 다음 마이그레이션 순서

1. `shared/constants`, `shared/types`, `shared/api`를 먼저 고정한다.
2. `AppShell`과 `HomePage`부터 React로 옮긴다.
3. `NewsFeedSection`과 `ArticleDetailSection`의 렌더링 로직을 훅으로 분리한다.
4. 인증/저장/관심사 같은 mutation을 feature hook으로 뽑는다.
5. 마지막에 레거시 템플릿을 제거하고 React 엔트리로 교체한다.
