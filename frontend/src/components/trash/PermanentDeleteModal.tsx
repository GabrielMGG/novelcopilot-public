type PermanentDeleteModalProps = {
  storyName: string;
  isDeleting: boolean;
  onClose: () => void;
  onConfirm: () => void;
};

export function PermanentDeleteModal({
  storyName,
  isDeleting,
  onClose,
  onConfirm,
}: PermanentDeleteModalProps) {
  return (
    <div className="modal-backdrop">
      <div 
        className="story-modal permanent-delete-modal" 
        role="dialog" 
        aria-modal="true"
      >
        <div className="modal-header">
          <div>
            <h2>Excluir definitivamente?</h2>
          </div>
        </div>

        <p style={{ marginTop: "12px", fontSize: "15px", color: "var(--ink)" }}>
          Excluir "{storyName}" definitivamente?
        </p>
        <p style={{ marginTop: "8px", fontSize: "13px", color: "var(--ink-soft)" }}>
          A história, seus capítulos, personagens, informações do mundo e outros dados relacionados serão apagados e não poderão ser recuperados.
        </p>

        <div className="modal-actions" style={{ marginTop: "24px" }}>
          <button
            className="secondary-button"
            type="button"
            onClick={onClose}
            disabled={isDeleting}
          >
            Cancelar
          </button>
          <button
            className="btn-primary"
            type="button"
            onClick={onConfirm}
            disabled={isDeleting}
            style={{ background: "#d93838", borderColor: "#d93838" }}
          >
            {isDeleting ? "Excluindo..." : "Excluir definitivamente"}
          </button>
        </div>
      </div>
    </div>
  );
}
