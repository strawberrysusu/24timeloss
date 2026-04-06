import type { Category } from "../../shared/constants/categories";

interface CategoryTabsProps {
  categories: readonly { label: string; value: Category | null }[];
  selectedCategory: Category | null;
  onSelect: (category: Category | null) => void;
}

export function CategoryTabs({
  categories,
  selectedCategory,
  onSelect,
}: CategoryTabsProps) {
  return (
    <div className="cat-tabs-wrap" id="cat-tabs-wrap">
      <div className="container">
        <div className="cat-tabs">
          {categories.map((category) => {
            const isActive = category.value === selectedCategory;
            return (
              <button
                key={category.label}
                className={`cat-pill ${isActive ? "active" : ""}`}
                onClick={() => onSelect(category.value)}
              >
                {category.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
