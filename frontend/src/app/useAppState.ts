import { useNavigate } from "react-router-dom";

import { useAuthState } from "./hooks/useAuthState";
import { useCreateArticleState } from "./hooks/useCreateArticleState";
import { useHomeNavigationState } from "./hooks/useHomeNavigationState";
import { usePersonalizationState } from "./hooks/usePersonalizationState";
import { useThemeState } from "./hooks/useThemeState";

export function useAppState() {
  const navigate = useNavigate();
  const authState = useAuthState();
  const themeState = useThemeState();
  const homeNavigation = useHomeNavigationState();
  const personalizationState = usePersonalizationState(authState.currentUser);
  const createArticleState = useCreateArticleState();

  async function handleAuthSubmit(formData: FormData) {
    const user = await authState.submitAuthForm(formData);
    if (user) {
      await personalizationState.refreshMyPageState();
    }
  }

  async function handleLogout() {
    await authState.logout();
    personalizationState.clearPersonalization();
    createArticleState.closeCreateEditor();
    navigate("/");
  }

  async function handleToggleSave(articleId: number) {
    if (!authState.currentUser) {
      authState.openAuthModal("login");
      return;
    }

    await personalizationState.toggleSave(articleId);
  }

  async function handleInterestToggle(category: Parameters<typeof personalizationState.toggleInterest>[0]) {
    if (!authState.currentUser) {
      authState.openAuthModal("login");
      return;
    }

    await personalizationState.toggleInterest(category);
  }

  function handleMyPageNavigation() {
    if (!authState.currentUser) {
      authState.openAuthModal("login");
      return;
    }

    navigate("/mypage");
  }

  function openCreateEditor() {
    if (!authState.currentUser) {
      authState.openAuthModal("login");
      return;
    }

    createArticleState.openCreateEditor();
  }

  return {
    currentPage: homeNavigation.currentPage,
    currentUser: authState.currentUser,
    activeInterests: personalizationState.activeInterests,
    savedArticleIds: personalizationState.savedArticleIds,
    showSampleBanner: homeNavigation.showSampleBanner,
    homeResetKey: homeNavigation.homeResetKey,
    theme: themeState.theme,
    authModal: authState.authModal,
    isCreateEditorOpen: createArticleState.isCreateEditorOpen,
    refreshMyPageState: personalizationState.refreshMyPageState,
    setCurrentUser: authState.setCurrentUser,
    handleAuthSubmit,
    handleLogout,
    handleToggleSave,
    handleInterestToggle,
    handleCreateArticleSubmit: createArticleState.handleCreateArticleSubmit,
    openAuthModal: authState.openAuthModal,
    closeAuthModal: authState.closeAuthModal,
    setAuthModalMode: authState.setAuthModalMode,
    navigateHome: homeNavigation.navigateHome,
    handleMyPageNavigation,
    openCreateEditor,
    closeCreateEditor: createArticleState.closeCreateEditor,
    dismissSampleBanner: homeNavigation.dismissSampleBanner,
    toggleTheme: themeState.toggleTheme,
    requestAuthentication: () => authState.openAuthModal("login"),
  };
}
