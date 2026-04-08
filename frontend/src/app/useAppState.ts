import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import type { PageName, ThemeMode } from "./AppShell";
import { createArticle } from "../shared/api/articles";
import {
  addInterest,
  getCurrentUser,
  getMyPage,
  listSavedArticleIds,
  loginMember,
  logoutMember,
  removeInterest,
  saveArticle,
  signupMember,
  unsaveArticle,
} from "../shared/api/members";
import type { Category } from "../shared/constants/categories";
import type { ArticleEditorValues } from "../shared/types/article";
import type { CurrentUser } from "../shared/types/member";

type AuthMode = "login" | "signup";
type HomeSection = "cat-tabs-wrap" | "trending-list" | null;

const THEME_STORAGE_KEY = "newspick-theme";

interface AuthModalState {
  open: boolean;
  mode: AuthMode;
  errorMessage: string;
}

function getInitialTheme(): ThemeMode {
  if (typeof window === "undefined") {
    return "light";
  }

  try {
    const savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
    if (savedTheme === "light" || savedTheme === "dark") {
      return savedTheme;
    }
  } catch {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }

  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function derivePageName(pathname: string): PageName {
  if (pathname.startsWith("/detail/")) {
    return "detail";
  }
  if (pathname.startsWith("/mypage")) {
    return "mypage";
  }
  return "home";
}

export function useAppState() {
  const location = useLocation();
  const navigate = useNavigate();

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [activeInterests, setActiveInterests] = useState<Category[]>([]);
  const [savedArticleIds, setSavedArticleIds] = useState<Set<number>>(new Set());
  const [showSampleBanner, setShowSampleBanner] = useState(true);
  const [isCreateEditorOpen, setIsCreateEditorOpen] = useState(false);
  const [homeResetKey, setHomeResetKey] = useState(0);
  const [theme, setTheme] = useState<ThemeMode>(() => getInitialTheme());
  const [pendingHomeSection, setPendingHomeSection] = useState<HomeSection>(null);
  const [authModal, setAuthModal] = useState<AuthModalState>({
    open: false,
    mode: "login",
    errorMessage: "",
  });

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      try {
        const user = await getCurrentUser();
        if (cancelled) {
          return;
        }
        setCurrentUser(user);

        const savedIds = await listSavedArticleIds();
        if (!cancelled) {
          setSavedArticleIds(new Set(savedIds));
        }
      } catch {
        if (!cancelled) {
          setCurrentUser(null);
          setActiveInterests([]);
          setSavedArticleIds(new Set());
        }
      }

      try {
        const myPage = await getMyPage();
        if (!cancelled) {
          setActiveInterests(myPage.interests);
        }
      } catch {
        if (!cancelled) {
          setActiveInterests([]);
        }
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Ignore storage failures and keep the in-memory theme.
    }
  }, [theme]);

  useEffect(() => {
    if (location.pathname !== "/" || !pendingHomeSection) {
      return;
    }

    const timer = window.setTimeout(() => {
      document.getElementById(pendingHomeSection)?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
      setPendingHomeSection(null);
    }, 100);

    return () => window.clearTimeout(timer);
  }, [location.pathname, pendingHomeSection, homeResetKey]);

  async function refreshMyPageState() {
    const [myPageResult, savedIdsResult] = await Promise.allSettled([
      getMyPage(),
      listSavedArticleIds(),
    ]);

    if (myPageResult.status === "fulfilled") {
      setActiveInterests(myPageResult.value.interests);
    } else {
      setActiveInterests([]);
    }

    if (savedIdsResult.status === "fulfilled") {
      setSavedArticleIds(new Set(savedIdsResult.value));
    } else {
      setSavedArticleIds(new Set());
    }
  }

  function openAuthModal(mode: AuthMode) {
    setAuthModal({ open: true, mode, errorMessage: "" });
  }

  function closeAuthModal() {
    setAuthModal((previous) => ({ ...previous, open: false, errorMessage: "" }));
  }

  function setAuthModalMode(mode: AuthMode) {
    setAuthModal({ open: true, mode, errorMessage: "" });
  }

  async function handleAuthSubmit(formData: FormData) {
    try {
      const email = String(formData.get("email") ?? "").trim();
      const password = String(formData.get("password") ?? "");

      if (authModal.mode === "login") {
        const user = await loginMember({ email, password });
        setCurrentUser(user);
      } else {
        const nickname = String(formData.get("nickname") ?? "").trim();
        const user = await signupMember({ email, password, nickname });
        setCurrentUser(user);
      }

      await refreshMyPageState();
      setAuthModal({ open: false, mode: authModal.mode, errorMessage: "" });
    } catch (error) {
      const message = error instanceof Error ? error.message : "인증에 실패했습니다.";
      setAuthModal((previous) => ({ ...previous, errorMessage: message }));
    }
  }

  async function handleLogout() {
    await logoutMember().catch(() => undefined);
    setCurrentUser(null);
    setActiveInterests([]);
    setSavedArticleIds(new Set());
    navigate("/");
  }

  async function handleToggleSave(articleId: number) {
    if (!currentUser) {
      openAuthModal("login");
      return;
    }

    const isSaved = savedArticleIds.has(articleId);

    try {
      if (isSaved) {
        await unsaveArticle(articleId);
        setSavedArticleIds((previous) => {
          const next = new Set(previous);
          next.delete(articleId);
          return next;
        });
      } else {
        await saveArticle(articleId);
        setSavedArticleIds((previous) => {
          const next = new Set(previous);
          next.add(articleId);
          return next;
        });
      }
    } catch {
      await refreshMyPageState();
    }
  }

  async function handleInterestToggle(category: Category) {
    if (!currentUser) {
      openAuthModal("login");
      return;
    }

    const isActive = activeInterests.includes(category);

    try {
      if (isActive) {
        await removeInterest(category);
        setActiveInterests((previous) => previous.filter((item) => item !== category));
      } else {
        await addInterest(category);
        setActiveInterests((previous) => [...previous, category]);
      }
    } catch {
      await refreshMyPageState();
    }
  }

  function navigateHome(section: HomeSection = null) {
    setHomeResetKey((previous) => previous + 1);
    setPendingHomeSection(section);
    navigate("/");
  }

  function handleMyPageNavigation() {
    if (!currentUser) {
      openAuthModal("login");
      return;
    }

    navigate("/mypage");
  }

  function openCreateEditor() {
    if (!currentUser) {
      openAuthModal("login");
      return;
    }

    setIsCreateEditorOpen(true);
  }

  function closeCreateEditor() {
    setIsCreateEditorOpen(false);
  }

  async function handleCreateArticleSubmit(values: ArticleEditorValues) {
    const created = await createArticle(values);
    setIsCreateEditorOpen(false);
    navigate(`/detail/${created.id}`);
  }

  return {
    currentPage: derivePageName(location.pathname),
    currentUser,
    activeInterests,
    savedArticleIds,
    showSampleBanner,
    homeResetKey,
    theme,
    authModal,
    isCreateEditorOpen,
    refreshMyPageState,
    setCurrentUser,
    handleAuthSubmit,
    handleLogout,
    handleToggleSave,
    handleInterestToggle,
    handleCreateArticleSubmit,
    openAuthModal,
    closeAuthModal,
    setAuthModalMode,
    navigateHome,
    handleMyPageNavigation,
    openCreateEditor,
    closeCreateEditor,
    dismissSampleBanner: () => setShowSampleBanner(false),
    toggleTheme: () => setTheme((previous) => (previous === "light" ? "dark" : "light")),
    requestAuthentication: () => openAuthModal("login"),
  };
}
