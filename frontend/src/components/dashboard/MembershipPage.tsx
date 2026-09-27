"use client";

/* Training Ledger page: the gym's real membership plans — add, price, and retire them. */
import { useEffect, useState } from "react";
import {
  FlexButton,
  FlexCard,
  FlexEmptyState,
  FlexPageHeader,
  FlexPill,
  FlexStatCard,
} from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import PlanFormModal from "@/components/dashboard/PlanFormModal";
import PlanReassignModal from "@/components/dashboard/PlanReassignModal";
import { useSession } from "@/hooks/useSession";
import { deleteMembershipPlan, listMembershipPlans, type BillingCycle, type MembershipPlan } from "@/lib/membershipPlans";
import { Layers, Plus, Tag } from "lucide-react";

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

export default function MembershipPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formState, setFormState] = useState<"closed" | "create" | MembershipPlan>("closed");
  const [reassignTarget, setReassignTarget] = useState<MembershipPlan | null>(null);

  useEffect(() => {
    if (!gymId) return;
    listMembershipPlans(gymId)
      .then(setPlans)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load plans."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const handleSaved = (plan: MembershipPlan) => {
    setPlans((prev) => (prev.some((p) => p.id === plan.id) ? prev.map((p) => (p.id === plan.id ? plan : p)) : [...prev, plan]));
    setFormState("closed");
  };

  const handleRemove = async (plan: MembershipPlan) => {
    if (!gymId) return;
    if (plans.length <= 1) return;
    setError(null);
    try {
      await deleteMembershipPlan(gymId, plan.id);
      setPlans((prev) => prev.filter((p) => p.id !== plan.id));
    } catch {
      // Plan has current or historical members tied to it — hand off to the reassignment flow.
      setReassignTarget(plan);
    }
  };

  const handleReassignedDeleted = (planId: number) => {
    setPlans((prev) => prev.filter((p) => p.id !== planId));
    setReassignTarget(null);
  };

  const activeCount = plans.filter((p) => p.active).length;

  return (
    <div className="page-enter space-y-7">
      <FlexPageHeader
        title="The business of belonging."
        subtitle="Finance · Membership plans — every plan members can join, and what it costs them."
        actions={
          <FlexButton onClick={() => setFormState("create")}>
            <Plus className="size-4" />
            Add plan
          </FlexButton>
        }
      />

      <section className="grid gap-4 sm:grid-cols-2">
        <FlexStatCard
          label="Active plans"
          value={String(activeCount)}
          detail={`${plans.length} total`}
          icon={<Layers className="size-5" />}
          variant="lime"
        />
        <FlexStatCard
          label="Inactive plans"
          value={String(plans.length - activeCount)}
          detail="not offered to new members"
          icon={<Tag className="size-5" />}
          variant="white"
        />
      </section>

      {error && <ErrorBanner message={error} />}

      <FlexCard>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Plan directory</p>
            <h2 className="mt-2 text-lg font-black tracking-tight text-stone-900">
              {loading ? "Loading plans…" : `${plans.length} plan${plans.length === 1 ? "" : "s"}`}
            </h2>
          </div>
        </div>

        {!loading && plans.length === 0 && (
          <div className="mt-4">
            <FlexEmptyState title="No plans yet — add your first one." />
          </div>
        )}

        {plans.length > 0 && (
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            {plans.map((plan) => (
              <FlexCard key={plan.id} className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-base font-black tracking-tight text-stone-900">{plan.name}</p>
                    {plan.description && (
                      <p className="mt-1 text-xs font-medium text-stone-500">{plan.description}</p>
                    )}
                  </div>
                  <FlexPill tone={plan.active ? "lime" : "stone"}>
                    {plan.active ? "Active" : "Inactive"}
                  </FlexPill>
                </div>

                <div className="mt-5 flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-400">Price</p>
                    <p className="mt-1 font-mono text-2xl font-black tracking-tight text-stone-900">
                      {plan.currency} {plan.price}
                    </p>
                    <p className="mt-1 text-xs font-semibold text-stone-500">{cycleLabel(plan.billingCycle)}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <FlexButton variant="ghost" onClick={() => setFormState(plan)}>
                      Edit
                    </FlexButton>
                    <FlexButton
                      variant="ghost"
                      onClick={plans.length <= 1 ? undefined : () => handleRemove(plan)}
                      className={plans.length <= 1 ? "pointer-events-none opacity-40" : ""}
                    >
                      {plans.length <= 1 ? "Only plan" : "Remove"}
                    </FlexButton>
                  </div>
                </div>
              </FlexCard>
            ))}
          </div>
        )}
      </FlexCard>

      {formState !== "closed" && gymId && (
        <PlanFormModal
          gymId={gymId}
          plan={formState === "create" ? null : formState}
          onClose={() => setFormState("closed")}
          onSaved={handleSaved}
        />
      )}

      {reassignTarget && gymId && (
        <PlanReassignModal
          gymId={gymId}
          plan={reassignTarget}
          otherPlans={plans.filter((p) => p.id !== reassignTarget.id)}
          onClose={() => setReassignTarget(null)}
          onDeleted={handleReassignedDeleted}
        />
      )}
    </div>
  );
}
