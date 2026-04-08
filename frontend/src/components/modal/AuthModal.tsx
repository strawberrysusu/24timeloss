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
