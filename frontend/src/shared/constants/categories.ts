export const CATEGORY_OPTIONS = [
  { label: "전체", value: null },
  { label: "정치", value: "POLITICS" },
  { label: "경제", value: "ECONOMY" },
  { label: "사회", value: "SOCIETY" },
  { label: "IT·과학", value: "IT_SCIENCE" },
  { label: "세계", value: "WORLD" },
  { label: "스포츠", value: "SPORTS" },
  { label: "연예", value: "ENTERTAINMENT" },
] as const;

export const CATEGORY_LABELS = {
  POLITICS: "정치",
  ECONOMY: "경제",
  SOCIETY: "사회",
  IT_SCIENCE: "IT·과학",
  WORLD: "세계",
  SPORTS: "스포츠",
  ENTERTAINMENT: "연예",
} as const;

export type Category = keyof typeof CATEGORY_LABELS;
