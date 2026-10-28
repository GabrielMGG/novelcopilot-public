import { useEffect, useRef, useState } from "react";
import { ImagePlus, X, RefreshCw, Upload, Undo2 } from "lucide-react";

const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

type StoryCoverFieldProps = {
  currentCoverUrl?: string | null;
  selectedFile: File | null;
  onFileSelect: (file: File | null) => void;
  markedForRemoval?: boolean;
  onRemoveCurrentCover?: () => void;
  error?: string;
  disabled?: boolean;
};

export function StoryCoverField({
  currentCoverUrl,
  selectedFile,
  onFileSelect,
  markedForRemoval,
  onRemoveCurrentCover,
  error,
  disabled,
}: StoryCoverFieldProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [validationError, setValidationError] = useState("");

  // Generate preview from selected file
  useEffect(() => {
    if (!selectedFile) {
      setTimeout(() => setPreviewUrl(null), 0);
      return;
    }

    const objectUrl = URL.createObjectURL(selectedFile);
    setTimeout(() => setPreviewUrl(objectUrl), 0);

    return () => {
      URL.revokeObjectURL(objectUrl);
    };
  }, [selectedFile]);

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    event.target.value = ""; // Reset to allow re-selection

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setValidationError("Escolha uma imagem JPG, PNG ou WebP.");
      onFileSelect(null);
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setValidationError("A imagem deve ter no máximo 5 MB.");
      onFileSelect(null);
      return;
    }

    setValidationError("");
    onFileSelect(file);
  }

  function handleRemoveCurrent() {
    setValidationError("");
    if (onRemoveCurrentCover) {
      onRemoveCurrentCover();
    }
  }

  function handleCancelSelection() {
    setValidationError("");
    if (previewUrl && previewUrl.startsWith("blob:")) {
      URL.revokeObjectURL(previewUrl);
    }
    onFileSelect(null);
    setPreviewUrl(currentCoverUrl ?? null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  function handleChooseFile() {
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    fileInputRef.current?.click();
  }

  const displayUrl = selectedFile
    ? previewUrl
    : markedForRemoval
      ? null
      : currentCoverUrl;

  const displayError = error || validationError;

  return (
    <div className="story-cover-field">
      <div className="story-cover-field-header">
        <div>
          <span className="story-cover-field-label">Imagem de capa</span>
          <p className="story-cover-field-help" id="cover-help-text">
            A imagem será ajustada para 800 × 1200 px. JPG, PNG ou WebP. Máximo 5 MB.
          </p>
        </div>
      </div>

      <div className="story-cover-layout">
        <div className="story-cover-preview">
          {displayUrl ? (
            <img
              src={displayUrl}
              alt="Prévia da capa"
              className="story-cover-preview-image"
            />
          ) : (
            <div className="story-cover-placeholder">
              <ImagePlus size={28} />
              <span>Sem capa</span>
            </div>
          )}
        </div>

        <div className="story-cover-controls">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={handleFileChange}
            className="story-cover-input"
            disabled={disabled}
            aria-describedby="cover-help-text cover-error-text"
            tabIndex={-1}
          />

          {disabled ? (
            <span className="story-cover-status-text">Processando imagem...</span>
          ) : markedForRemoval ? (
            <>
              <span className="story-cover-status-text">A capa será removida ao salvar</span>
              <button
                type="button"
                className="btn btn-secondary story-cover-btn"
                onClick={handleRemoveCurrent} // The parent toggles it
              >
                <Undo2 size={16} />
                Desfazer remoção
              </button>
            </>
          ) : selectedFile ? (
            <>
              <span className="story-cover-status-text">Nova capa selecionada</span>
              <div className="story-cover-btn-group">
                <button
                  type="button"
                  className="btn btn-primary story-cover-btn"
                  onClick={handleChooseFile}
                >
                  <RefreshCw size={16} />
                  Escolher outra
                </button>
                <button
                  type="button"
                  className="btn btn-secondary story-cover-btn"
                  onClick={handleCancelSelection}
                >
                  <X size={16} />
                  Cancelar seleção
                </button>
              </div>
            </>
          ) : currentCoverUrl ? (
            <>
              <span className="story-cover-status-text">Capa atual</span>
              <div className="story-cover-btn-group">
                <button
                  type="button"
                  className="btn btn-primary story-cover-btn"
                  onClick={handleChooseFile}
                >
                  <RefreshCw size={16} />
                  Substituir imagem
                </button>
                <button
                  type="button"
                  className="btn btn-secondary story-cover-btn story-cover-btn--danger"
                  onClick={handleRemoveCurrent}
                >
                  <X size={16} />
                  Remover capa
                </button>
              </div>
            </>
          ) : (
            <button
              type="button"
              className="btn btn-primary story-cover-btn"
              onClick={handleChooseFile}
            >
              <Upload size={16} />
              Escolher imagem
            </button>
          )}
        </div>
      </div>

      {displayError && (
        <div className="story-cover-error" id="cover-error-text" role="alert">
          {displayError}
        </div>
      )}
    </div>
  );
}
