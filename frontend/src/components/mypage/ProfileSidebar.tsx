import type { MyPageData } from "../../shared/types/member";

interface ProfileSidebarProps {
  data: MyPageData;
  activeSection: string;
  onSectionSelect: (section: string) => void;
}

const sections = [
  { id: "feed", label: "내 피드", icon: "📰" },
  { id: "saved", label: "저장한 뉴스", icon: "🔖" },
  { id: "history", label: "읽기 기록", icon: "📖" },
  { id: "interests", label: "관심 분야", icon: "🏷️" },
  { id: "settings", label: "계정 설정", icon: "⚙️" },
] as const;

export function ProfileSidebar({ data, activeSection, onSectionSelect }: ProfileSidebarProps) {
  return (
    <aside className="profile-card" id="profile-card">
      <div className="profile-top">
        <div className="profile-avatar">
          <span>{data.nickname.charAt(0)}</span>
        </div>
        <p className="profile-name">{data.nickname}</p>
        <p className="profile-email">{data.email}</p>
      </div>
      <nav className="side-nav">
        {sections.map((section) => (
          <button
            key={section.id}
            type="button"
            className={`side-nav-item ${activeSection === section.id ? "active" : ""}`}
            onClick={() => onSectionSelect(section.id)}
          >
            <span className="icon">{section.icon}</span>
            <span className="label">{section.label}</span>
          </button>
        ))}
      </nav>
    </aside>
  );
}
