import { AccountSettingsSection } from "../components/mypage/AccountSettingsSection";
import { InterestSettingsSection } from "../components/mypage/InterestSettingsSection";
import { MyPageOverview } from "../components/mypage/MyPageOverview";
import { ProfileSidebar } from "../components/mypage/ProfileSidebar";
import { ReadHistorySection } from "../components/mypage/ReadHistorySection";
import { SavedArticlesSection } from "../components/mypage/SavedArticlesSection";
import type { Category } from "../shared/constants/categories";
import type { ReadHistoryItem } from "../shared/types/article";
import type { MyPageData } from "../shared/types/member";

interface MyPageProps {
  data: MyPageData;
  history: ReadHistoryItem[];
  activeSection: string;
  onSectionSelect: (section: string) => void;
  onArticleClick: (articleId: number) => void;
  onInterestToggle: (category: Category) => void;
  onRemoveSaved: (articleId: number) => void;
  onUpdateNickname: (nickname: string) => Promise<void>;
  onUpdatePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  onLogout: () => void;
}

export function MyPage({
  data,
  history,
  activeSection,
  onSectionSelect,
  onArticleClick,
  onInterestToggle,
  onRemoveSaved,
  onUpdateNickname,
  onUpdatePassword,
  onLogout,
}: MyPageProps) {
  return (
    <div id="page-mypage" className="page active">
      <div className="container">
        <div className="mypage-grid">
          <ProfileSidebar data={data} activeSection={activeSection} onSectionSelect={onSectionSelect} />
          <main id="mypage-main">
            <MyPageOverview data={data} />
            <InterestSettingsSection
              interests={data.interests}
              onToggle={onInterestToggle}
            />
            <SavedArticlesSection
              articles={data.savedArticles}
              onArticleClick={onArticleClick}
              onRemove={onRemoveSaved}
            />
            <ReadHistorySection
              history={history}
              onArticleClick={onArticleClick}
            />
            <AccountSettingsSection
              email={data.email}
              nickname={data.nickname}
              onUpdateNickname={onUpdateNickname}
              onUpdatePassword={onUpdatePassword}
              onLogout={onLogout}
            />
          </main>
        </div>
      </div>
    </div>
  );
}
