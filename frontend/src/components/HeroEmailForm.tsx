"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Mail } from "lucide-react";
import { checkEmailExists, requestOtp } from "@/lib/auth";

export default function HeroEmailForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const cleanEmail = email.trim();
    if (!cleanEmail) return;

    setError(null);
    setLoading(true);

    try {
      const { exists } = await checkEmailExists(cleanEmail);
      if (exists) {
        await requestOtp(cleanEmail);
        router.push(`/otp?email=${encodeURIComponent(cleanEmail)}`);
      } else {
        router.push(`/register?email=${encodeURIComponent(cleanEmail)}`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong. Please try again.");
      setLoading(false);
    }
  };

  return (
    <div className="mt-8 flex flex-col gap-2">
      <form
        onSubmit={handleSubmit}
        className="flex w-full flex-col gap-3 sm:flex-row sm:items-center"
      >
        <div className="relative flex-1">
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4 text-ink/40">
            <Mail className="h-5 w-5" />
          </div>
          <input
            type="email"
            name="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            placeholder="Enter your email"
            className="w-full rounded-full border border-ink/18 bg-white py-3.5 pl-11 pr-4 text-base font-medium text-ink shadow-sm outline-none transition-colors placeholder:text-ink/40 focus:border-saffron focus:ring-2 focus:ring-saffron/20"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="button-saffron group justify-center px-7 py-3.5 text-base whitespace-nowrap disabled:opacity-60 cursor-pointer"
        >
          {loading ? (
            "Checking…"
          ) : (
            <>
              Continue{" "}
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </>
          )}
        </button>
      </form>

      {error && (
        <p className="text-sm font-medium text-red-600 pl-2">
          {error}
        </p>
      )}
    </div>
  );
}
