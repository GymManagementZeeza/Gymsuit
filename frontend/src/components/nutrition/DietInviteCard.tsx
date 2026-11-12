"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ArrowRight, CheckCircle2 } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { acceptNutritionInvite, previewNutritionInvite, type NutritionInvitePreview } from "@/lib/nutritionist";

/** Public page a client reaches from the invite email. Accepting links their GymSuit app account. */
export default function DietInviteCard() {
  const token = useSearchParams().get("token") ?? "";
  const [preview, setPreview] = useState<NutritionInvitePreview | null>(null);
  const [error, setError] = useState<string | null>(token ? null : "This link is missing its invitation code.");
  const [accepting, setAccepting] = useState(false);
  const [acceptedBy, setAcceptedBy] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    previewNutritionInvite(token)
      .then(setPreview)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load this invitation."));
  }, [token]);

  const accept = async () => {
    setError(null);
    setAccepting(true);
    try {
      const { nutritionistName } = await acceptNutritionInvite(token);
      setAcceptedBy(nutritionistName);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not accept this invitation.");
    } finally {
      setAccepting(false);
    }
  };

  const usable = preview?.status === "PENDING" && new Date(preview.expiresAt) > new Date();
  const name = preview?.nutritionistName ?? "A nutrition expert";

  if (acceptedBy) {
    return (
      <AuthShell
        variant="diet"
        eyebrow="You're connected"
        headline={`${acceptedBy} can now follow your progress.`}
        description="Keep logging meals and weight in the GymSuit app — that's all they need. They can view it, but can't change anything."
      >
        <p className="mt-8 flex items-center gap-2 text-sm font-semibold text-ink/70">
          <CheckCircle2 className="h-5 w-5 text-saffron" /> You can close this page.
        </p>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      variant="diet"
      eyebrow="Nutrition invite"
      headline={preview ? `${name} invited you.` : "Nutrition invite"}
      description={
        preview
          ? `Accepting lets them view the meals, macros and weight you log in the GymSuit app with ${preview.email}. They can't change anything.`
          : error
            ? "We couldn't open this invitation."
            : "Loading your invitation…"
      }
    >
      <div className="mt-9 flex flex-col gap-4">
        {error && <ErrorBanner message={error} />}

        {preview && !usable && (
          <ErrorBanner message="This invitation is no longer valid. Ask your nutrition expert to send a new one." />
        )}

        {preview && usable && !preview.hasAccount && (
          <p className="rounded-xl border border-ink/10 bg-white p-4 text-sm leading-6 text-ink/70">
            There&apos;s no GymSuit app account for <strong>{preview.email}</strong> yet. Install the GymSuit app, sign up
            with this email, then open this link again.
          </p>
        )}

        {preview && usable && (
          <button
            type="button"
            onClick={accept}
            disabled={accepting || !preview.hasAccount}
            className="button-saffron w-full justify-center py-3.5 text-base disabled:opacity-60"
          >
            {accepting ? "Connecting…" : "Accept invitation"} <ArrowRight className="h-4 w-4" />
          </button>
        )}
      </div>
    </AuthShell>
  );
}
