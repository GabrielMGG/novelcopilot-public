import { useMemo, useState, type FormEvent } from "react";
import { useAuth } from "../../hooks/useAuth";
import { updateProfileName } from "../../services/authService";
import "./ProfileSettingsPage.css";

function getProviderLabel(provider?: string) {
  if (provider === "google") return "Google";
  if (provider === "email") return "E-mail e senha";
  return provider ? provider.charAt(0).toUpperCase() + provider.slice(1) : "Conta NovelCopilot";
}

export function ProfileSettingsPage() {
  const { user } = useAuth();
  const metadata = user?.user_metadata;
  const savedName = useMemo(
    () => (metadata?.full_name as string | undefined) ?? (metadata?.name as string | undefined) ?? "",
    [metadata],
  );
  const provider = user?.app_metadata?.provider as string | undefined;
  const avatarUrl = (metadata?.avatar_url as string | undefined) ??
    (metadata?.picture as string | undefined) ?? null;
  const avatarLetter = (savedName[0] ?? user?.email?.[0] ?? "U").toUpperCase();

  const [name, setName] = useState(savedName);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const normalizedName = name.trim();
  const hasChanged = normalizedName !== savedName.trim();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (normalizedName.length < 2) {
      setError("Informe um nome com pelo menos 2 caracteres.");
      return;
    }
    if (normalizedName.length > 80) {
      setError("O nome pode ter no máximo 80 caracteres.");
      return;
    }

    setIsSaving(true);
    try {
      await updateProfileName(normalizedName);
      setName(normalizedName);
      setSuccess("Nome atualizado com sucesso.");
    } catch {
      setError("Não foi possível atualizar seu nome. Tente novamente.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div>
      <h2 className="settings-section-title">Perfil</h2>
      <p className="settings-section-desc">Gerencie as informações que identificam sua conta.</p>

      <section className="profile-summary" aria-label="Resumo da conta">
        {avatarUrl ? (
          <img className="profile-summary__avatar" src={avatarUrl} alt="" />
        ) : (
          <span className="profile-summary__avatar profile-summary__avatar--fallback" aria-hidden="true">{avatarLetter}</span>
        )}
        <div className="profile-summary__identity">
          <strong>{savedName || "Usuário NovelCopilot"}</strong>
          <span>{user?.email}</span>
        </div>
        <span className="profile-summary__provider">Acesso com {getProviderLabel(provider)}</span>
      </section>

      <form className="profile-form" onSubmit={handleSubmit} noValidate>
        <div className="profile-form__heading">
          <h3>Informações pessoais</h3>
          <p>Este nome aparece no menu da sua conta.</p>
        </div>

        <label className="settings-field">
          <span>Nome</span>
          <input
            type="text"
            value={name}
            maxLength={80}
            autoComplete="name"
            onChange={(event) => { setName(event.target.value); setError(""); setSuccess(""); }}
          />
        </label>

        <label className="settings-field">
          <span>E-mail</span>
          <input type="email" value={user?.email ?? ""} readOnly aria-describedby="profile-email-help" />
          <small id="profile-email-help">A alteração de e-mail será disponibilizada em uma etapa própria de segurança.</small>
        </label>

        {error && <div className="settings-message settings-message--error" role="alert">{error}</div>}
        {success && <div className="settings-message settings-message--success" role="status">{success}</div>}

        <div className="profile-form__actions">
          <button className="settings-primary-action" type="submit" disabled={isSaving || !hasChanged}>
            {isSaving ? "Salvando..." : "Salvar alterações"}
          </button>
        </div>
      </form>
    </div>
  );
}
