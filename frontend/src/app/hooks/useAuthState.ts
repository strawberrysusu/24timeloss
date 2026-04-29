import { useEffect, useState } from "react";

import {
  exchangeOAuthCode,
  getCurrentUser,
  loginMember,
  logoutMember,
  signupMember,
} from "../../shared/api/members";
import { clearStoredToken, getStoredToken } from "../../shared/api/http";
import type { CurrentUser } from "../../shared/types/member";

export type AuthMode = "login" | "signup";

export interface AuthModalState {
  open: boolean;
  mode: AuthMode;
  errorMessage: string;
}

export function useAuthState() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [authModal, setAuthModal] = useState<AuthModalState>({
    open: false,
    mode: "login",
    errorMessage: "",
  });

  useEffect(() => {
    let cancelled = false;

    const params = new URLSearchParams(window.location.search);
    const oauthCode = params.get("oauth_code");
    const oauthError = params.get("oauth_error");

    if (oauthError) {
      const message = oauthError === "OAUTH_EMAIL_REQUIRED"
        ? "이메일 동의 없이는 로그인할 수 없습니다. 다시 시도하면서 이메일 제공에 동의해 주세요."
        : "소셜 로그인 중 문제가 발생했습니다.";
      window.alert(message);
      window.history.replaceState({}, "", window.location.pathname);
    }

    async function bootstrap() {
      // OAuth 1회용 코드를 access token으로 교환한다 (URL에서 코드를 즉시 제거하여 노출 시간 최소화).
      if (oauthCode) {
        window.history.replaceState({}, "", window.location.pathname);
        try {
          const user = await exchangeOAuthCode(oauthCode);
          if (!cancelled) {
            setCurrentUser(user);
          }
          return;
        } catch {
          if (!cancelled) {
            clearStoredToken();
            setCurrentUser(null);
            window.alert("소셜 로그인 인증에 실패했습니다. 다시 시도해주세요.");
          }
          return;
        }
      }

      if (!getStoredToken()) {
        setCurrentUser(null);
        return;
      }
      try {
        const user = await getCurrentUser();
        if (!cancelled) {
          setCurrentUser(user);
        }
      } catch {
        if (!cancelled) {
          clearStoredToken();
          setCurrentUser(null);
        }
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  function openAuthModal(mode: AuthMode) {
    setAuthModal({ open: true, mode, errorMessage: "" });
  }

  function closeAuthModal() {
    setAuthModal((previous) => ({ ...previous, open: false, errorMessage: "" }));
  }

  function setAuthModalMode(mode: AuthMode) {
    setAuthModal({ open: true, mode, errorMessage: "" });
  }

  async function submitAuthForm(formData: FormData) {
    try {
      const email = String(formData.get("email") ?? "").trim();
      const password = String(formData.get("password") ?? "");

      let user: CurrentUser;

      if (authModal.mode === "login") {
        user = await loginMember({ email, password });
      } else {
        const nickname = String(formData.get("nickname") ?? "").trim();
        user = await signupMember({ email, password, nickname });
      }

      setCurrentUser(user);
      setAuthModal({ open: false, mode: authModal.mode, errorMessage: "" });
      return user;
    } catch (error) {
      const message = error instanceof Error ? error.message : "인증에 실패했습니다.";
      setAuthModal((previous) => ({ ...previous, errorMessage: message }));
      return null;
    }
  }

  async function logout() {
    await logoutMember();
    setCurrentUser(null);
  }

  return {
    currentUser,
    authModal,
    setCurrentUser,
    openAuthModal,
    closeAuthModal,
    setAuthModalMode,
    submitAuthForm,
    logout,
  };
}
