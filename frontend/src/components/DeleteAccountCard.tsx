"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight, CheckCircle2, Mail, ShieldCheck, Trash2 } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { clearSession, deleteAccountWithToken, requestOtp, verifyOtp } from "@/lib/auth";

const RESEND_COOLDOWN_SECONDS = 30;

type Step = "email" | "code" | "done";

/**
 * Self-service account deletion for app users (App Store / Google Play
 * requirement): email → one-time code → permanent delete. Works without
 * being signed in; the verified OTP session is used once for the delete
 * and never saved.
 */
export default function DeleteAccountCard() {
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  // Kept after a successful verify so a failed delete can be retried
  // without a new code (the code is single-use).
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => setResendCooldown((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const message = (err: unknown, fallback: string) => {
    if (err instanceof TypeError) return "Couldn't reach GymSuit. Check your connection and try again.";
    return err instanceof Error ? err.message : fallback;
  };

  const handleSendCode = async (event?: React.FormEvent) => {
    event?.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await requestOtp(email.trim().toLowerCase());
      setStep("code");
      setOtp("");
      setResendCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(message(err, "Could not send the code."));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      let token = accessToken;
      if (!token) {
        const session = await verifyOtp(email.trim().toLowerCase(), otp);
        token = session.token;
        setAccessToken(token);
      }
      await deleteAccountWithToken(token);
      // If this browser was signed in to the same account, drop that session too.
      clearSession();
      setStep("done");
    } catch (err) {
      setError(message(err, "Could not delete your account."));
    } finally {
      setLoading(false);
    }
  };

  if (step === "done") {
    return (
      <AuthShell
        eyebrow="Account deleted"
        headline="Your account is gone."
        description={`The GymSuit account for ${email.trim()} and its personal data have been permanently deleted. You've been signed out everywhere.`}
      >
        <div className="mt-9 flex items-start gap-3 rounded-xl border border-ink/10 bg-white px-4 py-4 text-sm leading-6 text-ink/70">
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
          <p>You can uninstall the GymSuit app. If you sign up again later, you&apos;ll start with a new, empty account.</p>
        </div>
        <Link href="/" className="button-saffron mt-6 w-full justify-center py-3.5 text-base">
          Back to GymSuit <ArrowRight className="h-4 w-4" />
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      eyebrow="Delete account"
      headline={step === "email" ? "Delete your GymSuit account." : "Confirm with your code."}
      description={
        step === "email"
          ? "Enter the email you use with GymSuit. We'll send a one-time code to confirm it's you."
          : `We sent a 6-digit code to ${email.trim()}. It expires in 5 minutes.`
      }
    >
      {step === "email" ? (
        <form className="mt-9 flex flex-col gap-4" onSubmit={handleSendCode}>
          <label className="flex flex-col gap-2">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Email</span>
            <span className="flex items-center gap-2.5 rounded-xl border border-ink/15 bg-white px-4 py-3.5 transition-colors focus-within:border-saffron">
              <Mail className="h-4 w-4 shrink-0 text-ink/40" />
              <input
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="w-full bg-transparent text-base text-ink outline-none placeholder:text-ink/30"
              />
            </span>
          </label>

          <div className="rounded-xl border border-ink/10 bg-white px-4 py-4 text-sm leading-6 text-ink/70">
            <p className="font-semibold text-ink">What gets deleted</p>
            <p className="mt-1">
              Your profile, sign-in, health and step data, workouts, food logs, challenge progress, points and
              badges. This can&apos;t be undone.
            </p>
          </div>

          {error && <ErrorBanner message={error} />}

          <button
            type="submit"
            disabled={loading || !email.trim()}
            className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
          >
            {loading ? "Sending code…" : "Send code"} <ArrowRight className="h-4 w-4" />
          </button>
        </form>
      ) : (
        <form className="mt-9 flex flex-col gap-4" onSubmit={handleDelete}>
          {!accessToken && (
            <label className="flex flex-col gap-2">
              <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">6-digit code</span>
              <span className="flex items-center gap-2.5 rounded-xl border border-ink/15 bg-white px-4 py-3.5 transition-colors focus-within:border-saffron">
                <ShieldCheck className="h-4 w-4 shrink-0 text-ink/40" />
                <input
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  required
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
                  placeholder="123456"
                  className="w-full bg-transparent text-lg font-semibold tracking-[0.3em] text-ink outline-none placeholder:text-ink/25"
                />
              </span>
            </label>
          )}

          <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3.5 text-sm leading-6 text-red-800">
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(e) => setConfirmed(e.target.checked)}
              className="mt-1 h-4 w-4 shrink-0 accent-red-600"
            />
            <span>I understand my account and all its data will be permanently deleted.</span>
          </label>

          {error && <ErrorBanner message={error} />}

          <button
            type="submit"
            disabled={loading || !confirmed || (!accessToken && otp.length !== 6)}
            className="mt-1 inline-flex w-full items-center justify-center gap-2 rounded-full bg-red-600 px-6 py-3.5 text-base font-bold text-white transition-colors hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Trash2 className="h-4 w-4" />
            {loading ? "Deleting…" : accessToken ? "Try deleting again" : "Delete my account"}
          </button>
        </form>
      )}

      {step === "code" && !accessToken && (
        <div className="mt-6 flex items-center justify-between text-sm">
          <button
            type="button"
            onClick={() => handleSendCode()}
            disabled={resendCooldown > 0 || loading}
            className="font-semibold text-ink/60 transition-colors hover:text-ink disabled:cursor-not-allowed disabled:text-ink/30"
          >
            {resendCooldown > 0 ? `Resend code in ${resendCooldown}s` : "Resend code"}
          </button>
          <button
            type="button"
            onClick={() => {
              setStep("email");
              setError(null);
              setConfirmed(false);
            }}
            className="font-semibold text-ink/60 transition-colors hover:text-ink"
          >
            Use a different email
          </button>
        </div>
      )}
    </AuthShell>
  );
}
