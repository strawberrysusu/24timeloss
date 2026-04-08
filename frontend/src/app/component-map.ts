export interface ComponentNode {
  name: string;
  path: string;
  sourceAnchor: string;
  responsibility: string;
  children?: ComponentNode[];
}

export const componentMap: ComponentNode[] = [
  {
    name: "AppShell",
    path: "frontend/src/app/AppShell.tsx",
    sourceAnchor: "index.mustache / body",
    responsibility: "공통 레이아웃과 페이지 전환 컨테이너",
    children: [
      {
        name: "SiteHeader",
        path: "frontend/src/components/layout/SiteHeader.tsx",
        sourceAnchor: "index.mustache / HEADER",
        responsibility: "로고, 메인 내비게이션, 인증 액션",
      },
      {
        name: "SiteFooter",
        path: "frontend/src/components/layout/SiteFooter.tsx",
        sourceAnchor: "index.mustache / FOOTER",
        responsibility: "푸터 링크와 서비스 정보",
      },
    ],
  },
  {
    name: "HomePage",
    path: "frontend/src/pages/HomePage.tsx",
    sourceAnchor: "index.mustache / PAGE: HOME",
    responsibility: "홈 화면 조립",
    children: [
      {
        name: "SampleBanner",
        path: "frontend/src/components/home/SampleBanner.tsx",
        sourceAnchor: "index.mustache / sample-banner",
        responsibility: "시연용 데이터 안내 배너",
      },
      {
        name: "HeroSection",
        path: "frontend/src/components/home/HeroSection.tsx",
        sourceAnchor: "index.mustache / Hero",
        responsibility: "서비스 소개와 검색 입력",
      },
      {
        name: "CategoryTabs",
        path: "frontend/src/components/home/CategoryTabs.tsx",
        sourceAnchor: "index.mustache / cat-tabs-wrap",
        responsibility: "카테고리 필터 탭",
      },
      {
        name: "NewsFeedSection",
        path: "frontend/src/components/home/NewsFeedSection.tsx",
        sourceAnchor: "index.mustache / news-feed",
        responsibility: "기사 카드 목록과 더보기 버튼",
      },
      {
        name: "HomeSidebar",
        path: "frontend/src/components/home/HomeSidebar.tsx",
        sourceAnchor: "index.mustache / sidebar",
        responsibility: "브리핑, 트렌딩, 관심 분야 카드 묶음",
      },
    ],
  },
  {
    name: "DetailPage",
    path: "frontend/src/pages/DetailPage.tsx",
    sourceAnchor: "index.mustache / PAGE: DETAIL",
    responsibility: "기사 상세 화면 조립",
    children: [
      {
        name: "DetailBreadcrumb",
        path: "frontend/src/components/detail/DetailBreadcrumb.tsx",
        sourceAnchor: "index.mustache / detail-breadcrumb",
        responsibility: "홈 > 카테고리 > 기사 경로 표시",
      },
      {
        name: "ArticleDetailSection",
        path: "frontend/src/components/detail/ArticleDetailSection.tsx",
        sourceAnchor: "index.mustache / article-detail",
        responsibility: "기사 본문, AI 요약, TTS, 저장 액션",
      },
      {
        name: "RelatedNewsSection",
        path: "frontend/src/components/detail/RelatedNewsSection.tsx",
        sourceAnchor: "index.mustache / related-news",
        responsibility: "관련 기사 카드 목록",
      },
    ],
  },
  {
    name: "MyPage",
    path: "frontend/src/pages/MyPage.tsx",
    sourceAnchor: "index.mustache / PAGE: MYPAGE",
    responsibility: "마이페이지 조립",
    children: [
      {
        name: "ProfileSidebar",
        path: "frontend/src/components/mypage/ProfileSidebar.tsx",
        sourceAnchor: "index.mustache / profile-card",
        responsibility: "프로필 요약과 섹션 내비게이션",
      },
      {
        name: "MyPageOverview",
        path: "frontend/src/components/mypage/MyPageOverview.tsx",
        sourceAnchor: "index.mustache / section-feed",
        responsibility: "읽기/저장/연속 방문 통계",
      },
      {
        name: "InterestSettingsSection",
        path: "frontend/src/components/mypage/InterestSettingsSection.tsx",
        sourceAnchor: "index.mustache / section-interests",
        responsibility: "관심 카테고리 토글",
      },
      {
        name: "SavedArticlesSection",
        path: "frontend/src/components/mypage/SavedArticlesSection.tsx",
        sourceAnchor: "index.mustache / section-saved",
        responsibility: "저장 기사 목록",
      },
      {
        name: "ReadHistorySection",
        path: "frontend/src/components/mypage/ReadHistorySection.tsx",
        sourceAnchor: "index.mustache / section-history",
        responsibility: "읽기 기록 목록",
      },
      {
        name: "AccountSettingsSection",
        path: "frontend/src/components/mypage/AccountSettingsSection.tsx",
        sourceAnchor: "index.mustache / section-settings",
        responsibility: "닉네임/비밀번호 변경, 로그아웃",
      },
    ],
  },
  {
    name: "Modals",
    path: "frontend/src/components/modal",
    sourceAnchor: "index.mustache / MODALS",
    responsibility: "인증 및 기사 등록/수정 모달",
    children: [
      {
        name: "AuthModal",
        path: "frontend/src/components/modal/AuthModal.tsx",
        sourceAnchor: "index.mustache / modal-login + modal-signup",
        responsibility: "로그인/회원가입 모드 전환",
      },
      {
        name: "ArticleEditorModal",
        path: "frontend/src/components/modal/ArticleEditorModal.tsx",
        sourceAnchor: "index.mustache / modal-admin",
        responsibility: "기사 등록 및 수정 폼",
      },
    ],
  },
];
