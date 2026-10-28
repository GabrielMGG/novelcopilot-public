import { useTranslation } from "react-i18next";
import type { AiReviewResult } from "../../types/ai";

type ChapterReviewModalProps = {
  review: AiReviewResult;
  onClose: () => void;
  onCopy: (text: string) => void;
};

function formatChapterReviewAsText(review: AiReviewResult) {
  const findings = review.findings.map((item) =>
    `${item.severity.toUpperCase()} — ${item.issue}\nEvidência: ${item.evidence}\nImpacto: ${item.impact}\nSugestão: ${item.suggestion}`
  ).join("\n\n");
  const strengths = review.strengths.length > 0 ? `Pontos fortes\n${review.strengths.join("\n")}` : "";
  const limitations = review.limitations.length > 0 ? `Limitações\n${review.limitations.join("\n")}` : "";
  return [review.summary, findings, strengths, limitations].filter(Boolean).join("\n\n").trim();
}

export function ChapterReviewModal({ review, onClose, onCopy }: ChapterReviewModalProps) {
  const { t } = useTranslation("workspace");
  return (
    <div className="workspace-dialog-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="workspace-dialog chapter-review-dialog" role="dialog" aria-modal="true" aria-labelledby="chapter-review-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="chapter-review-title">{t("editor.chapterReview.title")}</h2>
        <p className="chapter-review-summary">{review.summary}</p>

        <div className="chapter-review-findings">
          {review.findings.length === 0 ? (
            <p>{t("editor.chapterReview.noFindings")}</p>
          ) : review.findings.map((finding, index) => (
            <article key={`${finding.category}-${index}`} className={`chapter-review-finding is-${finding.severity}`}>
              <header><strong>{finding.issue}</strong><span>{t(`editor.chapterReview.severity.${finding.severity}`)}</span></header>
              <p><b>{t("editor.chapterReview.evidence")}:</b> {finding.evidence}</p>
              <p><b>{t("editor.chapterReview.impact")}:</b> {finding.impact}</p>
              <p><b>{t("editor.chapterReview.suggestion")}:</b> {finding.suggestion}</p>
            </article>
          ))}
        </div>

        {review.strengths.length > 0 && (
          <section className="chapter-review-notes">
            <h3>{t("editor.chapterReview.strengths")}</h3>
            <ul>{review.strengths.map((strength, index) => <li key={index}>{strength}</li>)}</ul>
          </section>
        )}
        {review.limitations.length > 0 && (
          <section className="chapter-review-notes">
            <h3>{t("editor.chapterReview.limitations")}</h3>
            <ul>{review.limitations.map((limitation, index) => <li key={index}>{limitation}</li>)}</ul>
          </section>
        )}

        <div className="workspace-dialog-actions">
          <button className="workspace-dialog-secondary" type="button" onClick={() => onCopy(formatChapterReviewAsText(review))}>{t("ai.suggestion.copy")}</button>
          <button className="workspace-dialog-primary" type="button" onClick={onClose}>{t("editor.chapterReview.close")}</button>
        </div>
      </section>
    </div>
  );
}
