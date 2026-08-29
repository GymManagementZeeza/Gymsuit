"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, ShieldCheck } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { dashboardPathForRole, requestOtp, saveSession, verifyOtp } from "@/lib/auth";

const RESEND_COOLDOWN_SECONDS = 30;

export default function OtpCard() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const email = searchParams.get("email") ?? "";

  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(RESEND_COOLDOWN_SECONDS);
  const [resending, setResending] = useState(false);

  useEffect(() => {
    if (!email) {
      router.replace("/login");
    }
  }, [email, router]);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => setResendCooldown((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const handleVerify = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const session = await verifyOtp(email, otp);
      saveSession(session);
      router.push(dashboardPathForRole(session.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not verify that code.");
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError(null);
    setResending(true);
    try {
      await requestOtp(email);
      setResendCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not resend the code.");
    } finally {
      setResending(false);
    }
  };

  return (
    <AuthShell
      eyebrow="Check your email"
      headline="Enter your code."
      description={
        email
          ? `We sent a 6-digit code to ${email}. It expires in 5 minutes.`
          : "We sent a 6-digit code to your email."
      }
    >
      <form className="mt-9 flex flex-col gap-4" onSubmit={handleVerify}>
        <label className="flex flex-col gap-2">
          <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">
            6-digit code
          </span>
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

        {error && <ErrorBanner message={error} />}

        <button
          type="submit"
          disabled={loading || otp.length !== 6}
          className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
        >
          {loading ? "Verifying…" : "Verify & continue"} <ArrowRight className="h-4 w-4" />
        </button>
      </form>

      <div className="mt-6 flex items-center justify-between text-sm">
        <button
          type="button"
          onClick={handleResend}
          disabled={resendCooldown > 0 || resending}
          className="font-semibold text-ink/60 transition-colors hover:text-ink disabled:cursor-not-allowed disabled:text-ink/30"
        >
          {resendCooldown > 0 ? `Resend code in ${resendCooldown}s` : resending ? "Sending…" : "Resend code"}
        </button>
        <Link href="/login" className="font-semibold text-ink/60 transition-colors hover:text-ink">
          Use a different email
        </Link>
      </div>
    </AuthShell>
  );
}
