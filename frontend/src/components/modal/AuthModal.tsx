type AuthMode = "login" | "signup";

interface AuthModalProps {
  mode: AuthMode;
  open: boolean;
  errorMessage?: string;
  onClose: () => void;
  onModeChange: (mode: AuthMode) => void;
  onSubmit: (formData: FormData) => void;
}

export function AuthModal({
  mode,
  open,
  errorMessage,
  onClose,
  onModeChange,
  onSubmit,
}: AuthModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modal-overlay show" onClick={onClose}>
      <div className="modal-box" onClick={(event) => event.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>
          &times;
        </button>
        <h2 className="modal-title">{mode === "login" ? "로그인" : "회원가입"}</h2>
        <p className="modal-desc">
          {mode === "login" ? "NewsPick에 오신 것을 환영합니다" : "간단한 가입으로 맞춤 뉴스를 받아보세요"}
        </p>
        {errorMessage ? <div className="modal-error">{errorMessage}</div> : null}
        <form
          onSubmit={(event) => {
            event.preventDefault();
            onSubmit(new FormData(event.currentTarget));
          }}
        >
          {mode === "signup" ? (
            <input className="modal-input" name="nickname" type="text" placeholder="닉네임" />
          ) : null}
          <input className="modal-input" name="email" type="email" placeholder="이메일" />
          <input className="modal-input" name="password" type="password" placeholder="비밀번호" />
          <button className="btn btn-solid modal-submit" type="submit">
            {mode === "login" ? "로그인" : "회원가입"}
          </button>
        </form>
        <div className="modal-divider">
          <span>또는</span>
        </div>
        <a href="/oauth2/authorization/google" className="btn btn-outline modal-google-btn">
          <svg viewBox="0 0 24 24" width="18" height="18" style={{ marginRight: "8px", verticalAlign: "middle" }}>
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Google로 {mode === "login" ? "로그인" : "가입"}
        </a>
        <p className="modal-switch">
          {mode === "login" ? "계정이 없으신가요?" : "이미 계정이 있으신가요?"}
          <button type="button" onClick={() => onModeChange(mode === "login" ? "signup" : "login")}>
            {mode === "login" ? "회원가입" : "로그인"}
          </button>
        </p>
      </div>
    </div>
  );
}
