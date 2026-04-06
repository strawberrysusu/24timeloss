import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";

import { AppShell, type PageName, type ThemeMode } from "./app/AppShell";
import { DetailPage } from "./pages/DetailPage";
import { MyPage } from "./pages/MyPage";
import { HomePage } from "./pages/HomePage";
import { AuthModal } from "./components/modal/AuthModal";
import { ArticleEditorModal } from "./components/modal/ArticleEditorModal";
import { useDetailPageData } from "./features/detail/useDetailPageData";
import { useHomePageData } from "./features/home/useHomePageData";
import { useMyPageData } from "./features/mypage/useMyPageData";
import { createArticle, extractArticleFromUrl, updateArticle } from "./shared/api/articles";
import {
  getCurrentUser,
  getMyPage,
  loginMember,
  listSavedArticleIds,
  logoutMember,
  removeInterest,
  saveArticle,
  signupMember,
  addInterest,
  unsaveArticle,
} from "./shared/api/members";
import { CATEGORY_OPTIONS, type Category } from "./shared/constants/categories";
import type { ArticleDetailData, ArticleEditorValues } from "./shared/types/article";
import type { CurrentUser } from "./shared/types/member";

type AuthMode = "login" | "signup";
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

function toArticleEditorValues(article: ArticleDetailData): ArticleEditorValues {
  return {
    category: article.category,
    title: article.title,
    source: article.source ?? "",
    originalUrl: article.originalUrl ?? "",
    thumbnailUrl: article.thumbnailUrl ?? "",
    videoEmbedUrl: article.videoEmbedUrl ?? "",
    content: article.content,
  };
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

function HomeRoute({
  resetKey,
  activeInterests,
  savedArticleIds,
  onInterestToggle,
  onToggleSave,
}: {
  resetKey: number;
  activeInterests: Category[];
  savedArticleIds: Set<number>;
  onInterestToggle: (category: Category) => void;
  onToggleSave: (articleId: number) => Promise<void>;
}) {
  const navigate = useNavigate();
  const home = useHomePageData(savedArticleIds, onToggleSave, resetKey);

  return (
    <HomePage
      keyword={home.keyword}
      selectedCategory={home.selectedCategory}
      categories={home.categories}
      trendingKeywords={home.trendingKeywords}
      articles={home.articles}
      isLastPage={home.isLastPage}
      briefingText={home.briefingText}
      briefingTimeLabel={home.briefingTimeLabel}
      trendingArticles={home.trendingArticles}
      activeInterests={activeInterests}
      savedArticleIds={home.savedArticleIds}
      onKeywordChange={home.setKeyword}
      onSearch={home.searchArticles}
      onKeywordClick={home.applyKeyword}
      onCategorySelect={home.selectCategory}
      onArticleClick={(articleId) => navigate(`/detail/${articleId}`)}
      onToggleSave={home.toggleSave}
      onLoadMore={home.loadMore}
      onInterestToggle={onInterestToggle}
    />
  );
}

function DetailRoute({
  currentUser,
  savedArticleIds,
  onToggleSave,
  onAuthenticationRequired,
}: {
  currentUser: CurrentUser | null;
  savedArticleIds: Set<number>;
  onToggleSave: (articleId: number) => Promise<void>;
  onAuthenticationRequired: () => void;
}) {
  const navigate = useNavigate();
  const params = useParams<{ id: string }>();
  const articleId = Number(params.id);
  const [isEditorOpen, setIsEditorOpen] = useState(false);

  const detail = useDetailPageData({
    articleId,
    currentUserId: currentUser?.id ?? null,
    onDeleted: () => navigate("/"),
    onAuthenticationRequired,
  });

  if (!Number.isFinite(articleId)) {
    return <Navigate to="/" replace />;
  }

  const editableArticle = detail.article;

  return (
    <>
      <DetailPage
        article={detail.article}
        relatedArticles={detail.relatedArticles}
        currentUser={currentUser}
        savedArticleIds={savedArticleIds}
        loading={detail.loading}
        errorMessage={detail.errorMessage}
        onHomeClick={() => navigate("/")}
        onCategoryClick={() => navigate("/")}
        onToggleSave={onToggleSave}
        onGenerateSummary={() => void detail.regenerateSummary()}
        onEditArticle={
          editableArticle && currentUser && editableArticle.writerId === currentUser.id
            ? () => setIsEditorOpen(true)
            : undefined
        }
        onDeleteArticle={() => {
          if (!window.confirm("정말 이 기사를 삭제하시겠습니까?")) {
            return;
          }
          void detail.removeArticle();
        }}
        onArticleClick={(nextArticleId) => navigate(`/detail/${nextArticleId}`)}
      />
      {editableArticle ? (
        <ArticleEditorModal
          open={isEditorOpen}
          mode="edit"
          categories={CATEGORY_OPTIONS}
          initialValues={toArticleEditorValues(editableArticle)}
          onClose={() => setIsEditorOpen(false)}
          onExtract={extractArticleFromUrl}
          onSubmit={async (values) => {
            await updateArticle(editableArticle.id, values);
            setIsEditorOpen(false);
            detail.refresh();
          }}
        />
      ) : null}
    </>
  );
}

function MyPageRoute({
  currentUser,
  onAuthenticationRequired,
  onUserUpdated,
  onSavedArticlesChanged,
  onLogout,
}: {
  currentUser: CurrentUser | null;
  onAuthenticationRequired: () => void;
  onUserUpdated: (user: CurrentUser) => void;
  onSavedArticlesChanged: () => Promise<void>;
  onLogout: () => void;
}) {
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

function AppContent() {
  const location = useLocation();
  const navigate = useNavigate();

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [activeInterests, setActiveInterests] = useState<Category[]>([]);
  const [savedArticleIds, setSavedArticleIds] = useState<Set<number>>(new Set());
  const [showSampleBanner, setShowSampleBanner] = useState(true);
  const [isCreateEditorOpen, setIsCreateEditorOpen] = useState(false);
  const [homeResetKey, setHomeResetKey] = useState(0);
  const [theme, setTheme] = useState<ThemeMode>(() => getInitialTheme());
  const [pendingHomeSection, setPendingHomeSection] = useState<"cat-tabs-wrap" | "trending-list" | null>(null);
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

  const currentPage = derivePageName(location.pathname);

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

  function navigateHome(section: "cat-tabs-wrap" | "trending-list" | null = null) {
    setHomeResetKey((previous) => previous + 1);
    setPendingHomeSection(section);
    navigate("/");
  }

  return (
    <>
      <AppShell
        currentPage={currentPage}
        theme={theme}
        currentUser={currentUser}
        showSampleBanner={currentPage === "home" && showSampleBanner}
        onHomeClick={() => navigateHome()}
        onCategoryClick={() => navigateHome("cat-tabs-wrap")}
        onTrendingClick={() => navigateHome("trending-list")}
        onMyPageClick={() => {
          if (!currentUser) {
            openAuthModal("login");
            return;
          }
          navigate("/mypage");
        }}
        onLoginClick={() => openAuthModal("login")}
        onSignupClick={() => openAuthModal("signup")}
        onArticleCreateClick={() => {
          if (!currentUser) {
            openAuthModal("login");
            return;
          }
          setIsCreateEditorOpen(true);
        }}
        onLogout={handleLogout}
        onDismissSampleBanner={() => setShowSampleBanner(false)}
        onThemeToggle={() => setTheme((previous) => (previous === "light" ? "dark" : "light"))}
      >
        <Routes>
          <Route
            path="/"
            element={
              <HomeRoute
                resetKey={homeResetKey}
                activeInterests={activeInterests}
                savedArticleIds={savedArticleIds}
                onInterestToggle={handleInterestToggle}
                onToggleSave={handleToggleSave}
              />
            }
          />
          <Route
            path="/detail/:id"
            element={
              <DetailRoute
                currentUser={currentUser}
                savedArticleIds={savedArticleIds}
                onToggleSave={handleToggleSave}
                onAuthenticationRequired={() => openAuthModal("login")}
              />
            }
          />
          <Route
            path="/mypage"
            element={
              <MyPageRoute
                currentUser={currentUser}
                onAuthenticationRequired={() => openAuthModal("login")}
                onUserUpdated={setCurrentUser}
                onSavedArticlesChanged={refreshMyPageState}
                onLogout={handleLogout}
              />
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AppShell>

      <AuthModal
        mode={authModal.mode}
        open={authModal.open}
        errorMessage={authModal.errorMessage}
        onClose={() => setAuthModal((previous) => ({ ...previous, open: false, errorMessage: "" }))}
        onModeChange={(mode) => setAuthModal({ open: true, mode, errorMessage: "" })}
        onSubmit={handleAuthSubmit}
      />
      <ArticleEditorModal
        open={isCreateEditorOpen}
        mode="create"
        categories={CATEGORY_OPTIONS}
        initialValues={createEmptyArticleValues()}
        onClose={() => setIsCreateEditorOpen(false)}
        onExtract={extractArticleFromUrl}
        onSubmit={async (values) => {
          const created = await createArticle(values);
          setIsCreateEditorOpen(false);
          navigate(`/detail/${created.id}`);
        }}
      />
    </>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}
