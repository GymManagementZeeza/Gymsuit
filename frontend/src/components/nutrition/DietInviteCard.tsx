"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ArrowRight, CheckCircle2, Phone, UserRound } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { acceptNutritionInvite, previewNutritionInvite, type NutritionInvitePreview } from "@/lib/nutritionist";

const inputShell =
  "flex items-center gap-2.5 rounded-xl border border-ink/15 bg-white px-4 py-3.5 transition-colors focus-within:border-saffron";
const inputClass = "w-full bg-transparent text-sm font-medium text-ink outline-none placeholder:text-ink/35";
const labelClass = "text-xs font-bold uppercase tracking-[0.13em] text-ink/50";

/**
 * Public page a client reaches from the invite email. An existing GymSuit app account is linked with one tap;
 * a new email gets a short sign-up form that creates the account and links it in the same step.
 */
export default function DietInviteCard() {
  const token = useSearchParams().get("token") ?? "";
  const [preview, setPreview] = useState<NutritionInvitePreview | null>(null);
  const [error, setError] = useState<string | null>(token ? null : "This link is missing its invitation code.");
  const [accepting, setAccepting] = useState(false);
  const [acceptedBy, setAcceptedBy] = useState<string | null>(null);
  const [createdAccount, setCreatedAccount] = useState(false);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");

  useEffect(() => {
    if (!token) return;
    previewNutritionInvite(token)
      .then(setPreview)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load this invitation."));
  }, [token]);

  const needsSignup = preview != null && !preview.hasAccount && !preview.staffAccount;

  const accept = async (event?: React.FormEvent) => {
    event?.preventDefault();
    if (accepting) return;
    setError(null);
    setAccepting(true);
    try {
      const { nutritionistName } = await acceptNutritionInvite(
        token,
        needsSignup ? { firstName: firstName.trim(), lastName: lastName.trim(), phone: phone.trim() } : undefined,
      );
      setCreatedAccount(needsSignup);
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
        description={
          createdAccount
            ? `Your GymSuit account is ready. Install the GymSuit app and sign in with ${preview?.email} — you'll get a code by email. Log your meals and weight there; that's all ${acceptedBy} needs.`
            : "Keep logging meals and weight in the GymSuit app — that's all they need. They can view it, but can't change anything."
        }
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
      eyebrow={needsSignup && usable ? "Create your account" : "Nutrition invite"}
      headline={preview ? `${name} invited you.` : "Nutrition invite"}
      description={
        preview
          ? needsSignup && usable
            ? `You're new to GymSuit — add a few details to create your account for ${preview.email}. ${name} will see the meals, macros and weight you log in the GymSuit app, read-only.`
            : `Accepting lets them view the meals, macros and weight you log in the GymSuit app with ${preview.email}. They can't change anything.`
          : error
            ? "We couldn't open this invitation."
            : "Loading your invitation…"
      }
    >
      <div className="mt-9 flex flex-col gap-4">
        {preview && !usable && (
          <ErrorBanner message="This invitation is no longer valid. Ask your nutrition expert to send a new one." />
        )}

        {preview && usable && preview.staffAccount && (
          <ErrorBanner
            message={`${preview.email} is a gym staff or nutrition expert login, so it can't be linked as a client. Ask ${name} to invite the email you use in the GymSuit app.`}
          />
        )}

        {preview && usable && needsSignup && (
          <form className="flex flex-col gap-4" onSubmit={accept}>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="flex flex-col gap-2">
                <span className={labelClass}>First name</span>
                <span className={inputShell}>
                  <UserRound className="h-4 w-4 shrink-0 text-ink/40" />
                  <input
                    required
                    autoComplete="given-name"
                    aria-label="First name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className={inputClass}
                  />
                </span>
              </label>
              <label className="flex flex-col gap-2">
                <span className={labelClass}>Last name</span>
                <span className={inputShell}>
                  <input
                    required
                    autoComplete="family-name"
                    aria-label="Last name"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className={inputClass}
                  />
                </span>
              </label>
            </div>
            <label className="flex flex-col gap-2">
              <span className={labelClass}>Phone</span>
              <span className={inputShell}>
                <Phone className="h-4 w-4 shrink-0 text-ink/40" />
                <input
                  required
                  type="tel"
                  autoComplete="tel"
                  aria-label="Phone"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                  className={inputClass}
                />
              </span>
            </label>
            <p className="text-xs leading-5 text-ink/50">Email: {preview.email} (from your invitation)</p>
            {error && <ErrorBanner message={error} />}
            <button
              type="submit"
              disabled={accepting}
              className="button-saffron w-full justify-center py-3.5 text-base disabled:opacity-60"
            >
              {accepting ? "Creating account…" : "Create account & accept"} <ArrowRight className="h-4 w-4" />
            </button>
          </form>
        )}

        {preview && usable && preview.hasAccount && (
          <>
            {error && <ErrorBanner message={error} />}
            <button
              type="button"
              onClick={() => accept()}
              disabled={accepting}
              className="button-saffron w-full justify-center py-3.5 text-base disabled:opacity-60"
            >
              {accepting ? "Connecting…" : "Accept invitation"} <ArrowRight className="h-4 w-4" />
            </button>
          </>
        )}

        {!preview && error && <ErrorBanner message={error} />}
      </div>
    </AuthShell>
  );
}
