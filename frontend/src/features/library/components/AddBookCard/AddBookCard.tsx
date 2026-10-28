import { useTranslation } from "react-i18next";
import { IconPlus } from "../../../../components/icons/Icons";

type AddBookCardProps = {
  onClick: () => void;
};

export function AddBookCard({ onClick }: AddBookCardProps) {
  const { t } = useTranslation("library");

  return (
    <button className="book book-add" type="button" onClick={onClick}>
      <span className="book-add-circle">
        <IconPlus />
      </span>

      <span className="book-add-label">{t("addBook.label")}</span>
    </button>
  );
}
