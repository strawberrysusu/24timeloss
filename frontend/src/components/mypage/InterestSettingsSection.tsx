import { CATEGORY_LABELS, CATEGORY_OPTIONS } from "../../shared/constants/categories";
import type { Category } from "../../shared/constants/categories";

interface InterestSettingsSectionProps {
  interests: Category[];
  onToggle: (category: Category) => void;
}

export function InterestSettingsSection({
  interests,
  onToggle,
}: InterestSettingsSectionProps) {
  const categories = CATEGORY_OPTIONS.flatMap((category) =>
    category.value === null ? [] : [category.value],
  );

  return (
    <section className="settings-card" id="section-interests">
      <div className="settings-card-header">
        <h3 className="settings-card-title">관심 분야 설정</h3>
        <span className="settings-card-hint">선택한 항목 기반으로 피드가 구성됩니다</span>
      </div>
      <div className="interest-tags">
        {categories.map((category) => (
          <button
            key={category}
            className={`interest-btn ${interests.includes(category) ? "on" : "off"}`}
            onClick={() => onToggle(category)}
          >
            {CATEGORY_LABELS[category]}
          </button>
        ))}
      </div>
    </section>
  );
}
