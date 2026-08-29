"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, Building2, Search, Tag } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import {
  dashboardPathForRole,
  listPublicPlans,
  registerMember,
  saveSession,
  searchGyms,
  type Gender,
  type GymSearchResult,
  type PublicMembershipPlan,
} from "@/lib/auth";

function cycleLabel(cycle: string) {
  switch (cycle) {
    case "WEEKLY":
      return "week";
    case "BIWEEKLY":
      return "2 weeks";
    case "MONTHLY":
      return "month";
    case "QUARTERLY":
      return "quarter";
    case "SEMI_ANNUAL":
      return "6 months";
    case "ANNUAL":
      return "year";
    default:
      return cycle.toLowerCase();
  }
}

function GymSearchStep({ onSelect }: { onSelect: (gym: GymSearchResult) => void }) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<GymSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const trimmedQuery = query.trim();

  useEffect(() => {
    if (trimmedQuery.length < 2) {
      return;
    }
    const handle = setTimeout(async () => {
      setLoading(true);
      setError(null);
      try {
        setResults(await searchGyms(trimmedQuery));
      } catch (err) {
        setError(err instanceof Error ? err.message : "Could not search gyms.");
      } finally {
        setLoading(false);
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [trimmedQuery]);

  const visibleResults = trimmedQuery.length < 2 ? [] : results;

  return (
    <div className="mt-9 flex flex-col gap-4">
      <label className="flex flex-col gap-2">
        <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">
          Find your gym
        </span>
        <span className="flex items-center gap-2.5 rounded-xl border border-ink/15 bg-white px-4 py-3.5 transition-colors focus-within:border-saffron">
          <Search className="h-4 w-4 shrink-0 text-ink/40" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by gym name"
            className="w-full bg-transparent text-sm font-medium text-ink outline-none placeholder:text-ink/35"
          />
        </span>
      </label>

      {error && <ErrorBanner message={error} />}
      {loading && <p className="text-sm text-ink/50">Searching…</p>}

      {!loading && trimmedQuery.length >= 2 && visibleResults.length === 0 && !error && (
        <p className="text-sm text-ink/50">No gyms matched &ldquo;{trimmedQuery}&rdquo;.</p>
      )}

      <div className="flex flex-col gap-2.5">
        {visibleResults.map((gym) => (
          <button
            key={gym.id}
            type="button"
            onClick={() => onSelect(gym)}
            className="group flex items-center gap-3 rounded-xl border border-ink/15 bg-white px-4 py-3.5 text-left shadow-sm transition-colors hover:border-saffron"
          >
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-sage/10 text-sage">
              <Building2 className="h-4 w-4" />
            </span>
            <span>
              <span className="block text-sm font-semibold text-ink">{gym.name}</span>
              {(gym.city || gym.state) && (
                <span className="block text-xs text-ink/50">
                  {[gym.city, gym.state].filter(Boolean).join(", ")}
                </span>
              )}
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}

function PlanSelectStep({
  gym,
  onSelect,
  onBack,
}: {
  gym: GymSearchResult;
  onSelect: (plan: PublicMembershipPlan) => void;
  onBack: () => void;
}) {
  const [plans, setPlans] = useState<PublicMembershipPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listPublicPlans(gym.id)
      .then(setPlans)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load plans."))
      .finally(() => setLoading(false));
  }, [gym.id]);

  return (
    <div className="mt-9 flex flex-col gap-4">
      <div className="flex items-center justify-between rounded-xl border border-sage/30 bg-sage/10 px-4 py-3">
        <span className="text-sm font-semibold text-ink">Joining {gym.name}</span>
        <button type="button" onClick={onBack} className="text-xs font-bold text-ink/50 hover:text-ink">
          Change
        </button>
      </div>

      <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Choose a plan</span>

      {error && <ErrorBanner message={error} />}
      {loading && <p className="text-sm text-ink/50">Loading plans…</p>}

      {!loading && plans.length === 0 && !error && (
        <p className="text-sm text-ink/50">This gym hasn&apos;t published any plans yet — check back soon.</p>
      )}

      <div className="flex flex-col gap-2.5">
        {plans.map((plan) => (
          <button
            key={plan.id}
            type="button"
            onClick={() => onSelect(plan)}
            className="group flex items-center justify-between gap-3 rounded-xl border border-ink/15 bg-white px-4 py-3.5 text-left shadow-sm transition-colors hover:border-saffron"
          >
            <span className="flex items-center gap-3">
              <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-saffron/10 text-saffron">
                <Tag className="h-4 w-4" />
              </span>
              <span>
                <span className="block text-sm font-semibold text-ink">{plan.name}</span>
                {plan.description && <span className="block text-xs text-ink/50">{plan.description}</span>}
              </span>
            </span>
            <span className="shrink-0 text-sm font-bold text-ink">
              {plan.currency} {plan.price}
              <span className="text-xs font-medium text-ink/40">/{cycleLabel(plan.billingCycle)}</span>
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}

function Field({
  label,
  name,
  required,
  type = "text",
  placeholder,
  defaultValue,
}: {
  label: string;
  name: string;
  required?: boolean;
  type?: string;
  placeholder?: string;
  defaultValue?: string;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        placeholder={placeholder}
        defaultValue={defaultValue}
        className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron"
      />
    </label>
  );
}

function MemberDetailsStep({
  email,
  gym,
  plan,
  onBack,
  onChangePlan,
}: {
  email: string;
  gym: GymSearchResult;
  plan: PublicMembershipPlan;
  onBack: () => void;
  onChangePlan: () => void;
}) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const data = new FormData(event.currentTarget);
    const gender = data.get("gender") as string;

    try {
      const session = await registerMember({
        email: data.get("email") as string,
        gymId: gym.id,
        planId: plan.id,
        firstName: data.get("firstName") as string,
        lastName: data.get("lastName") as string,
        phone: data.get("phone") as string,
        dateOfBirth: (data.get("dateOfBirth") as string) || undefined,
        gender: gender ? (gender as Gender) : undefined,
        waiverAccepted: data.get("waiverAccepted") === "on",
      });
      saveSession(session);
      router.push(dashboardPathForRole(session.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not complete registration.");
      setLoading(false);
    }
  };

  return (
    <form className="mt-9 flex flex-col gap-5" onSubmit={handleSubmit}>
      <div className="flex items-center justify-between rounded-xl border border-sage/30 bg-sage/10 px-4 py-3">
        <span className="text-sm font-semibold text-ink">Joining {gym.name}</span>
        <button type="button" onClick={onBack} className="text-xs font-bold text-ink/50 hover:text-ink">
          Change
        </button>
      </div>

      <div className="flex items-center justify-between rounded-xl border border-saffron/30 bg-saffron/10 px-4 py-3">
        <span className="text-sm font-semibold text-ink">
          {plan.name} · {plan.currency} {plan.price}/{cycleLabel(plan.billingCycle)}
        </span>
        <button type="button" onClick={onChangePlan} className="text-xs font-bold text-ink/50 hover:text-ink">
          Change
        </button>
      </div>

      <Field label="Email" name="email" type="email" defaultValue={email} required />
      <div className="grid grid-cols-2 gap-3">
        <Field label="First name" name="firstName" required />
        <Field label="Last name" name="lastName" required />
      </div>
      <Field label="Phone" name="phone" type="tel" required />
      <div className="grid grid-cols-2 gap-3">
        <Field label="Date of birth" name="dateOfBirth" type="date" />
        <label className="flex flex-col gap-2">
          <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Gender</span>
          <select
            name="gender"
            defaultValue=""
            className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors focus:border-saffron"
          >
            <option value="">Prefer not to say</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </label>
      </div>

      <label className="flex items-start gap-2.5 text-sm text-ink/70">
        <input type="checkbox" name="waiverAccepted" required className="mt-0.5 h-4 w-4 accent-saffron" />
        I accept the gym&apos;s liability waiver and terms of membership.
      </label>

      {error && <ErrorBanner message={error} />}

      <button
        type="submit"
        disabled={loading}
        className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
      >
        {loading ? "Joining…" : "Join & continue"} <ArrowRight className="h-4 w-4" />
      </button>
    </form>
  );
}

export default function MemberRegisterCard() {
  const searchParams = useSearchParams();
  const email = searchParams.get("email") ?? "";
  const [gym, setGym] = useState<GymSearchResult | null>(null);
  const [plan, setPlan] = useState<PublicMembershipPlan | null>(null);

  return (
    <AuthShell
      eyebrow="Join your gym"
      headline={plan ? "Just a few details." : gym ? "Choose your plan." : "Find your gym."}
      description={
        plan
          ? `Setting up your membership${email ? ` for ${email}` : ""}.`
          : gym
            ? `Pick the plan you'd like to join ${gym.name} on.`
            : "Search for the gym you train at to get started."
      }
      panelWidth="max-w-[440px]"
    >
      {gym && plan ? (
        <MemberDetailsStep
          email={email}
          gym={gym}
          plan={plan}
          onBack={() => {
            setGym(null);
            setPlan(null);
          }}
          onChangePlan={() => setPlan(null)}
        />
      ) : gym ? (
        <PlanSelectStep gym={gym} onSelect={setPlan} onBack={() => setGym(null)} />
      ) : (
        <GymSearchStep onSelect={setGym} />
      )}

      <div className="my-8 h-px w-full bg-ink/10" />

      <Link
        href={`/register${email ? `?email=${encodeURIComponent(email)}` : ""}`}
        className="text-sm font-semibold text-ink/50 transition-colors hover:text-ink"
      >
        ← Back
      </Link>
    </AuthShell>
  );
}
