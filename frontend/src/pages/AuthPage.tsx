import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import {
    signInWithEmail,
    signInWithGoogle,
    signUpWithEmail,
} from "../services/authService";
import { consumeAuthReturnPath } from "../services/authReturnPath";
import { useLoadingPresentation } from "../hooks/useLoadingPresentation";
import { LoadingScreen } from "../components/ui/LoadingScreen";
import "../styles/AuthPage.css";
import { Navigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";



type AuthMode = "login" | "register";

export function AuthPage() {
    const { t } = useTranslation("auth");
    const { user, isAuthLoading } = useAuth();
    const navigate = useNavigate();
    const loadingPresentation = useLoadingPresentation(isAuthLoading);

    const [authMode, setAuthMode] = useState<AuthMode>("login");
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [message, setMessage] = useState("");

    const isRegisterMode = authMode === "register";

    if (loadingPresentation.blockContent) {
        return loadingPresentation.showLoading ? <LoadingScreen /> : null;
    }

    if (user) {
        return <Navigate to="/" replace />;
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!email.trim() || !password.trim()) {
            setMessage(t("missingEmailPassword"));
            return;
        }

        if (isRegisterMode && !name.trim()) {
            setMessage(t("missingName"));
            return;
        }

        try {
            setIsSubmitting(true);
            setMessage("");

            if (isRegisterMode) {
                await signUpWithEmail({
                    name,
                    email,
                    password,
                });

                setMessage(t("registerSuccess"));
                return;
            }

            await signInWithEmail({
                email,
                password,
            });

            navigate(consumeAuthReturnPath(), { replace: true });
        } catch (error) {
            setMessage(
                error instanceof Error ? error.message : t("genericError")
            );
        } finally {
            setIsSubmitting(false);
        }
    }

    async function handleGoogleSignIn() {
        try {
            setIsSubmitting(true);
            setMessage("");

            await signInWithGoogle();
        } catch (error) {
            setMessage(error instanceof Error ? error.message : t("genericError"));
            setIsSubmitting(false);
        }
    }
    return (
        <main className="auth-page">
            <section className="auth-inspiration-panel">
                <div className="auth-inspiration-brand">
                    <span className="auth-logo-mark">N</span>
                    <strong>{t("brand")}</strong>
                </div>

                <div className="auth-inspiration-copy">
                    <h1>{t("inspirationTitle")}</h1>
                    <p>{t("inspirationSubtitle")}</p>
                </div>

                <div className="auth-illustration" aria-hidden="true">
                    <div className="auth-illustration-quill" />
                    <div className="auth-illustration-line line-one" />
                    <div className="auth-illustration-line line-two" />
                    <div className="auth-illustration-line line-three" />
                </div>
            </section>

            <section className="auth-form-panel">
                <div className="auth-form-card">
                    <div className="auth-brand">
                        <span>{t("brand")}</span>

                        <h1>
                            {isRegisterMode ? t("registerTitle") : t("loginTitle")}
                        </h1>

                        <p>
                            {isRegisterMode
                                ? t("registerSubtitle")
                                : t("loginSubtitle")}
                        </p>
                    </div>

                    <form className="auth-form" onSubmit={handleSubmit}>
                        {isRegisterMode && (
                            <label className="auth-field">
                                <span>{t("name")}</span>
                                <input
                                    value={name}
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder={t("namePlaceholder")}
                                />
                            </label>
                        )}

                        <label className="auth-field">
                            <span>{t("email")}</span>
                            <input
                                type="email"
                                autoComplete="email"
                                value={email}
                                onChange={(event) => setEmail(event.target.value)}
                                placeholder={t("emailPlaceholder")}
                            />
                        </label>

                        <label className="auth-field">
                            <span>{t("password")}</span>
                            <input
                                type="password"
                                autoComplete="current-password"
                                value={password}
                                onChange={(event) => setPassword(event.target.value)}
                                placeholder={t("passwordPlaceholder")}
                            />
                        </label>

                        {message && <p className="auth-message">{message}</p>}

                        <button className="auth-submit" type="submit" disabled={isSubmitting}>
                            {isSubmitting
                                ? t("loading")
                                : isRegisterMode
                                    ? t("submitRegister")
                                    : t("submitLogin")}
                        </button>

                    </form>

                    <button
                        className="auth-mode-button"
                        type="button"
                        onClick={() => {
                            setAuthMode(isRegisterMode ? "login" : "register");
                            setMessage("");
                        }}
                    >
                        {isRegisterMode ? t("switchToLogin") : t("switchToRegister")}
                    </button>

                    <div className="auth-divider">
                        <span>{t("or")}</span>
                    </div>

                    <button
                        className="auth-google-button"
                        type="button"
                        onClick={handleGoogleSignIn}
                        disabled={isSubmitting}
                    >
                        <span className="auth-google-icon">G</span>
                        {t("continueWithGoogle")}
                    </button>
                </div>
            </section>
        </main>
    );
}
