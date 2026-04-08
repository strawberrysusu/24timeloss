import { Navigate, Route, Routes } from "react-router-dom";

import { HomeRoute } from "./routes/HomeRoute";
import { DetailRoute } from "./routes/DetailRoute";
import { MyPageRoute } from "./routes/MyPageRoute";
import type { Category } from "../shared/constants/categories";
import type { CurrentUser } from "../shared/types/member";

interface AppRoutesProps {
  currentUser: CurrentUser | null;
  activeInterests: Category[];
  savedArticleIds: Set<number>;
  homeResetKey: number;
  onInterestToggle: (category: Category) => void;
  onToggleSave: (articleId: number) => Promise<void>;
  onAuthenticationRequired: () => void;
  onUserUpdated: (user: CurrentUser) => void;
  onSavedArticlesChanged: () => Promise<void>;
  onLogout: () => void;
}

export function AppRoutes({
  currentUser,
  activeInterests,
  savedArticleIds,
  homeResetKey,
  onInterestToggle,
  onToggleSave,
  onAuthenticationRequired,
  onUserUpdated,
  onSavedArticlesChanged,
  onLogout,
}: AppRoutesProps) {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <HomeRoute
            resetKey={homeResetKey}
            activeInterests={activeInterests}
            savedArticleIds={savedArticleIds}
            onInterestToggle={onInterestToggle}
            onToggleSave={onToggleSave}
          />
        }
      />
      <Route
        path="/detail/:id"
        element={
          <DetailRoute
            currentUser={currentUser}
            savedArticleIds={savedArticleIds}
            onToggleSave={onToggleSave}
            onAuthenticationRequired={onAuthenticationRequired}
          />
        }
      />
      <Route
        path="/mypage"
        element={
          <MyPageRoute
            currentUser={currentUser}
            onAuthenticationRequired={onAuthenticationRequired}
            onUserUpdated={onUserUpdated}
            onSavedArticlesChanged={onSavedArticlesChanged}
            onLogout={onLogout}
          />
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
