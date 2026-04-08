import { AppShell } from "./AppShell";
import { AppRoutes } from "./AppRoutes";
import { useAppState } from "./useAppState";
import { AuthModal } from "../components/modal/AuthModal";
import { ArticleEditorModal } from "../components/modal/ArticleEditorModal";
import { extractArticleFromUrl } from "../shared/api/articles";
import { CATEGORY_OPTIONS } from "../shared/constants/categories";
import type { ArticleEditorValues } from "../shared/types/article";

function createEmptyArticleValues(): ArticleEditorValues {
  return {
    category: "",
    title: "",
    source: "",
    originalUrl: "",
    thumbnailUrl: "",
    videoEmbedUrl: "",
    content: "",
  };
}

export function AppContent() {
  const app = useAppState();

  return (
    <>
      <AppShell
        currentPage={app.currentPage}
        theme={app.theme}
        currentUser={app.currentUser}
        showSampleBanner={app.currentPage === "home" && app.showSampleBanner}
        onHomeClick={() => app.navigateHome()}
        onCategoryClick={() => app.navigateHome("cat-tabs-wrap")}
        onTrendingClick={() => app.navigateHome("trending-list")}
        onMyPageClick={app.handleMyPageNavigation}
        onLoginClick={() => app.openAuthModal("login")}
        onSignupClick={() => app.openAuthModal("signup")}
        onArticleCreateClick={app.openCreateEditor}
        onLogout={app.handleLogout}
        onDismissSampleBanner={app.dismissSampleBanner}
        onThemeToggle={app.toggleTheme}
      >
        <AppRoutes
          currentUser={app.currentUser}
          activeInterests={app.activeInterests}
          savedArticleIds={app.savedArticleIds}
          homeResetKey={app.homeResetKey}
          onInterestToggle={app.handleInterestToggle}
          onToggleSave={app.handleToggleSave}
          onAuthenticationRequired={app.requestAuthentication}
          onUserUpdated={app.setCurrentUser}
          onSavedArticlesChanged={app.refreshMyPageState}
          onLogout={app.handleLogout}
        />
      </AppShell>

      <AuthModal
        mode={app.authModal.mode}
        open={app.authModal.open}
        errorMessage={app.authModal.errorMessage}
        onClose={app.closeAuthModal}
        onModeChange={app.setAuthModalMode}
        onSubmit={app.handleAuthSubmit}
      />
      <ArticleEditorModal
        open={app.isCreateEditorOpen}
        mode="create"
        categories={CATEGORY_OPTIONS}
        initialValues={createEmptyArticleValues()}
        onClose={app.closeCreateEditor}
        onExtract={extractArticleFromUrl}
        onSubmit={app.handleCreateArticleSubmit}
      />
    </>
  );
}
