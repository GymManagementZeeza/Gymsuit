"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Mail, ShieldCheck, UserRound } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import {
  checkEmailExists,
  getSession,
  logout,
  registerNutritionist,
  requestOtp,
  saveSession,
  sendNutritionistSignupCode,
  verifyOtp,
} from "@/lib/auth";

type Step = "email" | "name" | "code";
type Mode = "login" | "register";

const RESEND_COOLDOWN_SECONDS = 30;
const inputShell =
  "flex items-center gap-2.5 rounded-xl border border-ink/15 bg-white px-4 py-3.5 transition-colors focus-within:border-saffron";
const inputClass = "w-full bg-transparent text-sm font-medium text-ink outline-none placeholder:text-ink/35";
const labelClass = "text-xs font-bold uppercase tracking-[0.13em] text-ink/50";

/**
 * diet.gymsuit.app sign-in and sign-up for standalone nutrition experts. One email field decides the path:
 * an existing account gets a sign-in code; a new email asks for a name, then a verification code that
 * creates the account. No gym is involved at any step.
 */
export default function DietAuthCard() {
  const router = useRouter();
  const [step, setStep] = useState<Step>("email");
  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  // Already signed in as a nutrition expert on this site — go straight to the workspace.
  useEffect(() => {
    if (getSession()?.role === "NUTRITIONIST") router.replace("/nutritiondashboard");
  }, [router]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => setCooldown((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const fail = (err: unknown) => {
    setError(err instanceof Error ? err.message : "Something went wrong. Please try again.");
    setLoading(false);
  };

  const sendCode = async (forMode: Mode, address: string) => {
    if (forMode === "login") await requestOtp(address);
    else await sendNutritionistSignupCode(address);
    setCooldown(RESEND_COOLDOWN_SECONDS);
  };

  const handleEmail = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    const address = email.trim().toLowerCase();
    try {
      const { exists } = await checkEmailExists(address);
      setEmail(address);
      if (exists) {
        await sendCode("login", address);
        setMode("login");
        setStep("code");
      } else {
        setMode("register");
        setStep("name");
      }
      setLoading(false);
    } catch (err) {
      fail(err);
    }
  };

  const handleName = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await sendCode("register", email);
      setStep("code");
      setLoading(false);
    } catch (err) {
      fail(err);
    }
  };

  const handleCode = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const session =
        mode === "login"
          ? await verifyOtp(email, otp)
          : await registerNutritionist({ email, otp, firstName: firstName.trim(), lastName: lastName.trim() });
      if (session.role !== "NUTRITIONIST") {
        // A gym owner, trainer or member typed their email here — their workspace is on the main site.
        await logout();
        throw new Error("This email belongs to a gym account. Sign in at gymsuit.app instead.");
      }
      saveSession(session);
      router.push("/nutritiondashboard");
    } catch (err) {
      fail(err);
    }
  };

  const resend = async () => {
    setError(null);
    try {
      await sendCode(mode, email);
    } catch (err) {
      fail(err);
    }
  };

  const startOver = () => {
    setStep("email");
    setOtp("");
    setError(null);
  };

  const copy = {
    email: ["Nutrition expert", "Sign in or create your account.", "Enter your email — new here? We'll set you up in a minute. No gym needed."],
    name: ["New account", "Tell us your name.", `This is how clients will see you in invites sent from ${email}.`],
    code: [
      mode === "login" ? "Check your inbox" : "Verify your email",
      "Enter the 6-digit code.",
      `We sent it to ${email}.`,
    ],
  }[step];

  return (
    <AuthShell variant="diet" eyebrow={copy[0]} headline={copy[1]} description={copy[2]}>
      <div className="mt-9 flex flex-col gap-5">
        {step === "email" && (
          <form className="flex flex-col gap-4" onSubmit={handleEmail}>
            <label className="flex flex-col gap-2">
              <span className={labelClass}>Email</span>
              <span className={inputShell}>
                <Mail className="h-4 w-4 shrink-0 text-ink/40" />
                <input
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@yourpractice.com"
                  className={inputClass}
                />
              </span>
            </label>
            {error && <ErrorBanner message={error} />}
            <button type="submit" disabled={loading} className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60">
              {loading ? "Checking…" : "Continue with email"} <ArrowRight className="h-4 w-4" />
            </button>
          </form>
        )}

        {step === "name" && (
          <form className="flex flex-col gap-4" onSubmit={handleName}>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="flex flex-col gap-2">
                <span className={labelClass}>First name</span>
                <span className={inputShell}>
                  <UserRound className="h-4 w-4 shrink-0 text-ink/40" />
                  <input required autoComplete="given-name" value={firstName} onChange={(e) => setFirstName(e.target.value)} className={inputClass} />
                </span>
              </label>
              <label className="flex flex-col gap-2">
                <span className={labelClass}>Last name</span>
                <span className={inputShell}>
                  <input required autoComplete="family-name" value={lastName} onChange={(e) => setLastName(e.target.value)} className={inputClass} />
                </span>
              </label>
            </div>
            {error && <ErrorBanner message={error} />}
            <button type="submit" disabled={loading} className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60">
              {loading ? "Sending code…" : "Send verification code"} <ArrowRight className="h-4 w-4" />
            </button>
            <button type="button" onClick={startOver} className="text-sm font-semibold text-ink/50 hover:text-ink">
              ← Use a different email
            </button>
          </form>
        )}

        {step === "code" && (
          <form className="flex flex-col gap-4" onSubmit={handleCode}>
            <label className="flex flex-col gap-2">
              <span className={labelClass}>Code</span>
              <span className={inputShell}>
                <ShieldCheck className="h-4 w-4 shrink-0 text-ink/40" />
                <input
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  required
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
                  placeholder="123456"
                  className={`${inputClass} tracking-[0.3em]`}
                />
              </span>
            </label>
            {error && <ErrorBanner message={error} />}
            <button
              type="submit"
              disabled={loading || otp.length !== 6}
              className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
            >
              {loading ? "Verifying…" : mode === "login" ? "Sign in" : "Create account"} <ArrowRight className="h-4 w-4" />
            </button>
            <div className="flex items-center justify-between text-sm font-semibold text-ink/50">
              <button type="button" onClick={startOver} className="hover:text-ink">
                ← Different email
              </button>
              <button type="button" onClick={resend} disabled={cooldown > 0} className="hover:text-ink disabled:opacity-50">
                {cooldown > 0 ? `Resend in ${cooldown}s` : "Resend code"}
              </button>
            </div>
          </form>
        )}
      </div>
    </AuthShell>
  );
}
