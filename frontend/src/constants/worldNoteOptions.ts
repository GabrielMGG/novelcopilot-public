import type {
  WorldNoteCategory,
  WorldNoteImportance,
  WorldNoteStatus,
} from "../types/worldNote";

export const worldNoteCategoryOptions: {
  value: WorldNoteCategory;
  labelKey: string;
}[] = [
  { value: "", labelKey: "category.empty" },
  { value: "LOCATION", labelKey: "category.location" },
  { value: "FACTION", labelKey: "category.faction" },
  { value: "CULTURE", labelKey: "category.culture" },
  { value: "HISTORY", labelKey: "category.history" },
  { value: "MAGIC_SYSTEM", labelKey: "category.magicSystem" },
  { value: "RULE", labelKey: "category.rule" },
  { value: "CREATURE", labelKey: "category.creature" },
  { value: "ITEM", labelKey: "category.item" },
  { value: "RELIGION", labelKey: "category.religion" },
  { value: "POLITICS", labelKey: "category.politics" },
  { value: "ECONOMY", labelKey: "category.economy" },
  { value: "OTHER", labelKey: "category.other" },
];

export const worldNoteImportanceOptions: {
  value: WorldNoteImportance;
  labelKey: string;
}[] = [
  { value: "", labelKey: "importance.empty" },
  { value: "HIGH", labelKey: "importance.high" },
  { value: "MEDIUM", labelKey: "importance.medium" },
  { value: "LOW", labelKey: "importance.low" },
];

export const worldNoteStatusOptions: {
  value: WorldNoteStatus;
  labelKey: string;
}[] = [
  { value: "", labelKey: "status.empty" },
  { value: "CANON", labelKey: "status.canon" },
  { value: "DRAFT", labelKey: "status.draft" },
  { value: "ARCHIVED", labelKey: "status.archived" },
];