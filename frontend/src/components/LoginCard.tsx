"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, Mail } from "lucide-react";
import GoogleSignInButton from "@/components/GoogleSignInButton";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { checkEmailExists, requestOtp } from "@/lib/auth";

export default function LoginCard() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const email = new FormData(event.currentTarget).get("email") as string;

    try {
      const { exists } = await checkEmailExists(email);
      if (exists) {
        await requestOtp(email);
        router.push(`/otp?email=${encodeURIComponent(email)}`);
      } else {
        router.push(`/register?email=${encodeURIComponent(email)}`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong. Please try again.");
      setLoading(false);
    }
  };

  return (
    <AuthShell
      eyebrow="Member & owner login"
      headline="Welcome back."
      description="Sign in with Google or continue with your email — no password to remember."
    >
      <div className="mt-9 flex flex-col gap-5">
        <GoogleSignInButton />

        <div className="flex items-center gap-3 text-xs font-bold uppercase tracking-[0.13em] text-ink/40">
          <span className="h-px flex-1 bg-ink/10" />
          or
          <span className="h-px flex-1 bg-ink/10" />
        </div>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <label className="flex flex-col gap-2">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">
              Email
            </span>
            <span className="flex items-center gap-2.5 rounded-xl border border-ink/15 bg-white px-4 py-3.5 transition-colors focus-within:border-saffron">
              <Mail className="h-4 w-4 shrink-0 text-ink/40" />
              <input
                type="email"
                name="email"
                required
                placeholder="you@yourgym.com"
                className="w-full bg-transparent text-sm font-medium text-ink outline-none placeholder:text-ink/35"
              />
            </span>
          </label>

          {error && <ErrorBanner message={error} />}

          <button
            type="submit"
            disabled={loading}
            className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
          >
            {loading ? "Checking…" : "Continue with email"} <ArrowRight className="h-4 w-4" />
          </button>
        </form>
      </div>

      <div className="my-8 h-px w-full bg-ink/10" />

      <p className="text-sm leading-6 text-ink/60">
        New to GymSuite?{" "}
        <Link href="/#pricing" className="font-semibold text-ink hover:text-saffron">
          Start free for 20 members
        </Link>
      </p>
      <Link
        href="/"
        className="mt-6 inline-flex items-center gap-2 text-sm font-semibold text-ink/50 transition-colors hover:text-ink"
      >
        ← Back to home
      </Link>
    </AuthShell>
  );
}
