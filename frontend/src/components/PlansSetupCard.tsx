"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Tag, Trash2 } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import {
  createMembershipPlan,
  deleteMembershipPlan,
  listMembershipPlans,
  type BillingCycle,
  type MembershipPlan,
} from "@/lib/membershipPlans";

const BILLING_CYCLES: { value: BillingCycle; label: string }[] = [
  { value: "WEEKLY", label: "Weekly" },
  { value: "BIWEEKLY", label: "Every 2 weeks" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "QUARTERLY", label: "Quarterly" },
  { value: "SEMI_ANNUAL", label: "Every 6 months" },
  { value: "ANNUAL", label: "Annual" },
];

function cycleLabel(cycle: BillingCycle) {
  return BILLING_CYCLES.find((c) => c.value === cycle)?.label ?? cycle;
}

export default function PlansSetupCard() {
  const router = useRouter();
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!gymId) return;
    listMembershipPlans(gymId)
      .then(setPlans)
      .catch(() => {})
      .finally(() => setLoadingList(false));
  }, [gymId]);

  const handleAddPlan = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!gymId) return;
    setError(null);
    setSaving(true);

    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      const plan = await createMembershipPlan(gymId, {
        name: data.get("name") as string,
        description: (data.get("description") as string) || undefined,
        price: Number(data.get("price")),
        currency: data.get("currency") as string,
        billingCycle: data.get("billingCycle") as BillingCycle,
      });
      setPlans((prev) => [...prev, plan]);
      form.reset();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not add this plan.");
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async (plan: MembershipPlan) => {
    if (!gymId) return;
    try {
      await deleteMembershipPlan(gymId, plan.id);
      setPlans((prev) => prev.filter((p) => p.id !== plan.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not remove this plan.");
    }
  };

  const goToJoiningFee = () => {
    router.push("/register/owner/joining-fee");
  };

  return (
    <AuthShell
      eyebrow="Set your pricing"
      headline="Add your membership plans."
      description="Every gym needs at least one plan before you can start adding members. Add as many as you like — you can edit these anytime."
      panelWidth="max-w-[480px]"
    >
      <div className="mt-9 flex flex-col gap-5">
        {plans.length > 0 && (
          <ul className="flex flex-col gap-2.5">
            {plans.map((plan) => (
              <li
                key={plan.id}
                className="flex items-center justify-between gap-3 rounded-xl border border-ink/15 bg-white px-4 py-3.5"
              >
                <div className="flex items-center gap-3">
                  <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-saffron/10 text-saffron">
                    <Tag className="h-4 w-4" />
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-ink">{plan.name}</p>
                    <p className="text-xs text-ink/50">
                      {plan.currency} {plan.price} · {cycleLabel(plan.billingCycle)}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleRemove(plan)}
                  disabled={plans.length <= 1}
                  className="grid h-8 w-8 place-items-center text-ink/40 transition-colors hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:text-ink/40"
                  aria-label={`Remove ${plan.name}`}
                  title={plans.length <= 1 ? "A gym must have at least one plan" : undefined}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}

        <form
          className="flex flex-col gap-3 rounded-2xl border border-dashed border-ink/20 p-4"
          onSubmit={handleAddPlan}
        >
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Plan name</span>
            <input
              name="name"
              required
              placeholder="Studio Monthly"
              className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron"
            />
          </label>

          <div className="grid grid-cols-3 gap-3">
            <label className="flex flex-col gap-1.5">
              <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Price</span>
              <input
                name="price"
                type="number"
                min="0"
                step="0.01"
                required
                placeholder="1999"
                className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Currency</span>
              <input
                name="currency"
                required
                defaultValue="INR"
                maxLength={3}
                className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium uppercase text-ink outline-none transition-colors focus:border-saffron"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Billing</span>
              <select
                name="billingCycle"
                defaultValue="MONTHLY"
                className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors focus:border-saffron"
              >
                {BILLING_CYCLES.map((cycle) => (
                  <option key={cycle.value} value={cycle.value}>
                    {cycle.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">
              Description (optional)
            </span>
            <input
              name="description"
              placeholder="Unlimited classes, all locations"
              className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron"
            />
          </label>

          <button
            type="submit"
            disabled={saving || !gymId}
            className="button-quiet mt-1 justify-center py-3 text-sm disabled:opacity-60"
          >
            {saving ? "Adding…" : "Add plan"}
          </button>
        </form>

        {error && <ErrorBanner message={error} />}

        <button
          type="button"
          onClick={goToJoiningFee}
          disabled={!session || loadingList || plans.length === 0}
          className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
        >
          Continue <ArrowRight className="h-4 w-4" />
        </button>
        {!loadingList && plans.length === 0 && (
          <p className="text-center text-xs text-ink/45">Add at least one plan to continue.</p>
        )}
      </div>
    </AuthShell>
  );
}
