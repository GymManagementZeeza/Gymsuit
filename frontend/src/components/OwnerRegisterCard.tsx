"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowRight } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import UpiIdModal from "@/components/UpiIdModal";
import { registerOwner, saveSession } from "@/lib/auth";
import { getGym, type Gym } from "@/lib/gyms";

function Field({
  label,
  name,
  required,
  type = "text",
  placeholder,
}: {
  label: string;
  name: string;
  required?: boolean;
  type?: string;
  placeholder?: string;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        placeholder={placeholder}
        className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron"
      />
    </label>
  );
}

export default function OwnerRegisterCard() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const email = searchParams.get("email") ?? "";

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingGym, setPendingGym] = useState<Gym | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const data = new FormData(event.currentTarget);
    try {
      const session = await registerOwner({
        email,
        gymName: data.get("gymName") as string,
        addressLine: (data.get("addressLine") as string) || undefined,
        city: (data.get("city") as string) || undefined,
        state: (data.get("state") as string) || undefined,
        postalCode: (data.get("postalCode") as string) || undefined,
        country: (data.get("country") as string) || undefined,
        gymPhone: (data.get("gymPhone") as string) || undefined,
        firstName: data.get("firstName") as string,
        lastName: data.get("lastName") as string,
        phone: (data.get("phone") as string) || undefined,
      });
      saveSession(session);
      const gym = await getGym(session.gymId!);
      setPendingGym(gym);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create your gym. Please try again.");
      setLoading(false);
    }
  };

  const continueToPlans = () => router.push("/register/owner/plans");

  return (
    <AuthShell
      eyebrow="Set up your gym"
      headline="Let's get your gym set up."
      description={`Creating your GymSuite account as the owner${email ? ` for ${email}` : ""}.`}
      panelWidth="max-w-[460px]"
    >
      <form className="mt-9 flex flex-col gap-5" onSubmit={handleSubmit}>
        <div className="flex flex-col gap-3">
          <p className="text-xs font-bold uppercase tracking-[0.13em] text-ink/40">Your gym</p>
          <Field label="Gym name" name="gymName" required placeholder="Iron Temple Fitness" />
          <Field label="Address" name="addressLine" placeholder="12 MG Road" />
          <div className="grid grid-cols-2 gap-3">
            <Field label="City" name="city" />
            <Field label="State" name="state" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Postal code" name="postalCode" />
            <Field label="Country" name="country" />
          </div>
          <Field label="Gym phone" name="gymPhone" type="tel" />
        </div>

        <div className="flex flex-col gap-3">
          <p className="text-xs font-bold uppercase tracking-[0.13em] text-ink/40">
            About you (the owner)
          </p>
          <div className="grid grid-cols-2 gap-3">
            <Field label="First name" name="firstName" required />
            <Field label="Last name" name="lastName" required />
          </div>
          <Field label="Your phone" name="phone" type="tel" />
        </div>

        {error && <ErrorBanner message={error} />}

        <button
          type="submit"
          disabled={loading || !email}
          className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
        >
          {loading ? "Creating your gym…" : "Create gym & continue"} <ArrowRight className="h-4 w-4" />
        </button>
      </form>

      <div className="my-8 h-px w-full bg-ink/10" />

      <Link
        href={`/register${email ? `?email=${encodeURIComponent(email)}` : ""}`}
        className="text-sm font-semibold text-ink/50 transition-colors hover:text-ink"
      >
        ← Back
      </Link>

      {pendingGym && (
        <UpiIdModal
          gymId={pendingGym.id}
          gym={pendingGym}
          onClose={continueToPlans}
          onSkip={continueToPlans}
          onSaved={continueToPlans}
        />
      )}
    </AuthShell>
  );
}
