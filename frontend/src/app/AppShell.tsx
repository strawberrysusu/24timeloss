import type { ReactNode } from "react";

import { SampleBanner } from "../components/home/SampleBanner";
import { SiteFooter } from "../components/layout/SiteFooter";
import { SiteHeader } from "../components/layout/SiteHeader";
import type { CurrentUser } from "../shared/types/member";

export type PageName = "home" | "detail" | "mypage";
export type ThemeMode = "light" | "dark";

interface AppShellProps {
  currentPage: PageName;
  theme: ThemeMode;
  currentUser: CurrentUser | null;
  showSampleBanner?: boolean;
  children: ReactNode;
  onHomeClick: () => void;
  onCategoryClick: () => void;
  onTrendingClick: () => void;
  onMyPageClick: () => void;
  onLoginClick: () => void;
  onSignupClick: () => void;
  onArticleCreateClick?: () => void;
  onLogout: () => void;
  onDismissSampleBanner?: () => void;
  onThemeToggle: () => void;
}

export function AppShell({
  currentPage,
  theme,
  currentUser,
  showSampleBanner = false,
  children,
  onHomeClick,
  onCategoryClick,
  onTrendingClick,
  onMyPageClick,
  onLoginClick,
  onSignupClick,
  onArticleCreateClick,
  onLogout,
  onDismissSampleBanner,
  onThemeToggle,
}: AppShellProps) {
  return (
    <>
      <SiteHeader
        currentPage={currentPage}
        theme={theme}
        currentUser={currentUser}
        onHomeClick={onHomeClick}
        onCategoryClick={onCategoryClick}
        onTrendingClick={onTrendingClick}
        onMyPageClick={onMyPageClick}
        onLoginClick={onLoginClick}
        onSignupClick={onSignupClick}
        onArticleCreateClick={onArticleCreateClick}
        onLogout={onLogout}
        onThemeToggle={onThemeToggle}
      />
      {showSampleBanner ? <SampleBanner onDismiss={onDismissSampleBanner} /> : null}
      {children}
      <SiteFooter />
    </>
  );
}
