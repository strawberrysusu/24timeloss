import { useEffect, useState } from "react";

interface AccountSettingsSectionProps {
  email: string;
  nickname: string;
  onUpdateNickname: (nickname: string) => Promise<void>;
  onUpdatePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  onLogout: () => void;
}

export function AccountSettingsSection({
  email,
  nickname,
  onUpdateNickname,
  onUpdatePassword,
  onLogout,
}: AccountSettingsSectionProps) {
  const [nicknameInput, setNicknameInput] = useState(nickname);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [nicknameError, setNicknameError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [nicknameSuccess, setNicknameSuccess] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");

  useEffect(() => {
    setNicknameInput(nickname);
  }, [nickname]);

  async function handleNicknameSubmit() {
    setNicknameError("");
    setNicknameSuccess("");

    if (!nicknameInput.trim()) {
      setNicknameError("닉네임을 입력해주세요.");
      return;
    }

    try {
      await onUpdateNickname(nicknameInput.trim());
      setNicknameSuccess("닉네임이 변경되었습니다.");
    } catch (error) {
      setNicknameError(error instanceof Error ? error.message : "닉네임 변경에 실패했습니다.");
    }
  }

  async function handlePasswordSubmit() {
    setPasswordError("");
    setPasswordSuccess("");

    if (!currentPassword || !newPassword) {
      setPasswordError("모든 항목을 입력해주세요.");
      return;
    }

    if (newPassword.length < 4) {
      setPasswordError("새 비밀번호는 4자 이상이어야 합니다.");
      return;
    }

    try {
      await onUpdatePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setPasswordSuccess("비밀번호가 변경되었습니다.");
    } catch (error) {
      setPasswordError(error instanceof Error ? error.message : "비밀번호 변경에 실패했습니다.");
    }
  }

  return (
    <section className="settings-card" id="section-settings">
      <div className="settings-card-header">
        <h3 className="settings-card-title">계정 설정</h3>
      </div>
      <div id="mypage-settings" style={{ padding: "4px 0" }}>
        <p style={{ fontSize: "14px", color: "var(--ash)", marginBottom: "16px" }}>이메일: {email}</p>
        <div style={{ marginBottom: "20px" }}>
          <label style={{ fontSize: "13px", fontWeight: 500, color: "var(--ink)", display: "block", marginBottom: "6px" }}>
            닉네임 변경
          </label>
          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
            <input
              className="modal-input"
              type="text"
              value={nicknameInput}
              placeholder="새 닉네임"
              style={{ margin: 0, flex: 1 }}
              onChange={(event) => setNicknameInput(event.target.value)}
            />
            <button className="btn btn-solid btn-md" onClick={handleNicknameSubmit}>
              변경
            </button>
          </div>
          {nicknameError ? <div className="modal-error" style={{ marginTop: "4px" }}>{nicknameError}</div> : null}
          {nicknameSuccess ? <div style={{ marginTop: "4px", fontSize: "12px", color: "var(--success)" }}>{nicknameSuccess}</div> : null}
        </div>
        <div style={{ marginBottom: "20px" }}>
          <label style={{ fontSize: "13px", fontWeight: 500, color: "var(--ink)", display: "block", marginBottom: "6px" }}>
            비밀번호 변경
          </label>
          <input
            className="modal-input"
            type="password"
            value={currentPassword}
            placeholder="현재 비밀번호"
            style={{ marginBottom: "6px" }}
            onChange={(event) => setCurrentPassword(event.target.value)}
          />
          <input
            className="modal-input"
            type="password"
            value={newPassword}
            placeholder="새 비밀번호 (4자 이상)"
            style={{ marginBottom: "6px" }}
            onChange={(event) => setNewPassword(event.target.value)}
          />
          <button className="btn btn-solid btn-md" onClick={handlePasswordSubmit}>
            비밀번호 변경
          </button>
          {passwordError ? <div className="modal-error" style={{ marginTop: "4px" }}>{passwordError}</div> : null}
          {passwordSuccess ? <div style={{ marginTop: "4px", fontSize: "12px", color: "var(--success)" }}>{passwordSuccess}</div> : null}
        </div>
        <button className="btn btn-ghost btn-md" onClick={onLogout}>
          로그아웃
        </button>
      </div>
    </section>
  );
}
