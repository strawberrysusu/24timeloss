import { useEffect, useState } from "react";

import {
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

    async function bootstrap() {
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
