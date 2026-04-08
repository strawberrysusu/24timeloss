import type { PageName, ThemeMode } from "../../app/AppShell";
import type { CurrentUser } from "../../shared/types/member";

interface SiteHeaderProps {
  currentPage: PageName;
  theme: ThemeMode;
  currentUser: CurrentUser | null;
  onHomeClick: () => void;
  onCategoryClick: () => void;
  onTrendingClick: () => void;
  onMyPageClick: () => void;
  onLoginClick: () => void;
  onSignupClick: () => void;
  onArticleCreateClick?: () => void;
  onLogout: () => void;
  onThemeToggle: () => void;
}

export function SiteHeader({
  currentPage,
  theme,
  currentUser,
  onHomeClick,
  onCategoryClick,
  onTrendingClick,
  onMyPageClick,
  onLoginClick,
  onSignupClick,
  onArticleCreateClick,
  onLogout,
  onThemeToggle,
}: SiteHeaderProps) {
  return (
    <header className="site-header" data-page={currentPage}>
      <div className="container">
        <button className="logo-btn" onClick={onHomeClick}>
          <div className="logo-icon">
            <span>N</span>
          </div>
          <span className="logo-text">NewsPick</span>
        </button>

        <nav className="main-nav">
          <button onClick={onHomeClick}>홈</button>
          <button onClick={onCategoryClick}>카테고리</button>
          <button onClick={onTrendingClick}>트렌딩</button>
          <button onClick={onMyPageClick}>마이페이지</button>
        </nav>

        <div className="header-actions">
          <button
            className="btn btn-ghost theme-toggle"
            type="button"
            aria-pressed={theme === "dark"}
            onClick={onThemeToggle}
          >
            {theme === "light" ? "다크 모드" : "라이트 모드"}
          </button>
          {currentUser ? (
            <>
              {onArticleCreateClick ? (
                <button className="btn btn-ghost" onClick={onArticleCreateClick}>
                  + 기사 등록
                </button>
              ) : null}
              <span style={{ fontSize: "13px", color: "var(--ink)", fontWeight: 500 }}>
                {currentUser.nickname}님
              </span>
              <button className="btn btn-ghost" onClick={onLogout}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <button className="btn btn-ghost" onClick={onLoginClick}>
                로그인
              </button>
              <button className="btn btn-solid" onClick={onSignupClick}>
                회원가입
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
