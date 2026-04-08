import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useMyPageData } from "../../features/mypage/useMyPageData";
import { MyPage } from "../../pages/MyPage";
import type { CurrentUser } from "../../shared/types/member";

interface MyPageRouteProps {
  currentUser: CurrentUser | null;
  onAuthenticationRequired: () => void;
  onUserUpdated: (user: CurrentUser) => void;
  onSavedArticlesChanged: () => Promise<void>;
  onLogout: () => void;
}

export function MyPageRoute({
  currentUser,
  onAuthenticationRequired,
  onUserUpdated,
  onSavedArticlesChanged,
  onLogout,
}: MyPageRouteProps) {
  const navigate = useNavigate();
  const [activeSection, setActiveSection] = useState("feed");

  const myPage = useMyPageData({
    enabled: Boolean(currentUser),
    onAuthenticationRequired,
    onUserUpdated,
    onSavedArticlesChanged,
  });

  useEffect(() => {
    if (!currentUser) {
      onAuthenticationRequired();
      navigate("/", { replace: true });
    }
  }, [currentUser, navigate, onAuthenticationRequired]);

  if (!currentUser) {
    return null;
  }

  if (myPage.loading) {
    return (
      <main className="container" style={{ padding: "48px 24px" }}>
        <p className="loading-msg">마이페이지를 불러오는 중...</p>
      </main>
    );
  }

  if (!myPage.data) {
    return (
      <main className="container" style={{ padding: "48px 24px" }}>
        <p className="loading-msg">{myPage.errorMessage || "마이페이지를 불러오지 못했습니다."}</p>
      </main>
    );
  }

  return (
    <MyPage
      data={myPage.data}
      history={myPage.history}
      activeSection={activeSection}
      onSectionSelect={(section) => {
        setActiveSection(section);
        const sectionElement = document.getElementById(`section-${section}`);
        sectionElement?.scrollIntoView({ behavior: "smooth", block: "start" });
      }}
      onArticleClick={(articleId) => navigate(`/detail/${articleId}`)}
      onInterestToggle={(category) => void myPage.toggleInterest(category)}
      onRemoveSaved={(articleId) => void myPage.removeSavedArticle(articleId)}
      onUpdateNickname={myPage.changeNickname}
      onUpdatePassword={myPage.changePassword}
      onLogout={onLogout}
    />
  );
}
