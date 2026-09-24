"use client";

/* Training Ledger page: the gym's real membership plans — add, price, and retire them. */
import { useEffect, useState } from "react";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, TableAction } from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import PlanFormModal from "@/components/dashboard/PlanFormModal";
import { ExpandableRow, DetailRow } from "@/components/dashboard/ExpandableRow";
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
      <PageHeading
        eyebrow="Finance · Membership plans"
        title="The business of belonging."
        description="Every plan members can join, and what it costs them."
        actions={
          <ActionButton icon={<Plus className="size-4" />} onClick={() => setFormState("create")}>
            Add plan
          </ActionButton>
        }
      />

      <section className="grid gap-4 md:grid-cols-2">
        <MetricCard
          label="Active plans"
          value={String(activeCount)}
          detail={`${plans.length} total`}
          tone="lime"
          icon={<Layers className="size-5" />}
        />
        <MetricCard
          label="Inactive plans"
          value={String(plans.length - activeCount)}
          detail="not offered to new members"
          tone="paper"
          icon={<Tag className="size-5" />}
        />
      </section>

      {error && <ErrorBanner message={error} />}

      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
          <div>
            <p className="ledger-label">Plan directory</p>
            <h2 className="mt-2 text-xl font-bold">{loading ? "Loading plans…" : `${plans.length} plan${plans.length === 1 ? "" : "s"}`}</h2>
          </div>
        </div>

        {!loading && plans.length === 0 && (
          <p className="p-8 text-center text-sm text-[#76766f]">No plans yet — add your first one.</p>
        )}

        {plans.length > 0 && (
          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[640px] text-left">
              <thead className="border-b border-[#e7e7e1] bg-[#fafaf6]">
                <tr className="text-[10px] uppercase tracking-[0.12em] text-[#75756e]">
                  <th className="px-5 py-3">Plan</th>
                  <th className="px-4 py-3">Price</th>
                  <th className="px-4 py-3">Billing</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-[#ededE7]">
                {plans.map((plan) => (
                  <tr key={plan.id}>
                    <td className="px-5 py-4">
                      <p className="text-sm font-bold">{plan.name}</p>
                      {plan.description && <p className="mt-1 text-[11px] text-[#7a7a73]">{plan.description}</p>}
                    </td>
                    <td className="mono px-4 py-4 text-sm font-bold">
                      {plan.currency} {plan.price}
                    </td>
                    <td className="px-4 py-4 text-xs text-[#696962]">{cycleLabel(plan.billingCycle)}</td>
                    <td className="px-4 py-4 text-xs font-semibold">{plan.active ? "Active" : "Inactive"}</td>
                    <td className="px-5 py-4">
                      <div className="flex items-center justify-end gap-1.5">
                        <TableAction onClick={() => setFormState(plan)}>Edit</TableAction>
                        <TableAction onClick={() => handleRemove(plan)} disabled={plans.length <= 1}>
                          {plans.length <= 1 ? "Only plan" : "Remove"}
                        </TableAction>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {plans.length > 0 && (
          <div className="divide-y divide-[#ededE7] md:hidden">
            {plans.map((plan) => (
              <ExpandableRow
                key={plan.id}
                summary={
                  <div className="flex items-center gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold">{plan.name}</p>
                      <p className="mt-0.5 text-xs text-[#8a8a82]">{cycleLabel(plan.billingCycle)}</p>
                    </div>
                    <span className="mono shrink-0 text-sm font-bold">
                      {plan.currency} {plan.price}
                    </span>
                    <span
                      className={`shrink-0 text-[10px] font-bold uppercase tracking-[0.08em] ${
                        plan.active ? "text-[#3f7a1f]" : "text-[#8a8a82]"
                      }`}
                    >
                      {plan.active ? "Active" : "Inactive"}
                    </span>
                  </div>
                }
              >
                <div className="space-y-1">
                  <DetailRow label="Price">
                    {plan.currency} {plan.price}
                  </DetailRow>
                  <DetailRow label="Billing">{cycleLabel(plan.billingCycle)}</DetailRow>
                  <DetailRow label="Status">{plan.active ? "Active" : "Inactive"}</DetailRow>
                  {plan.description && <DetailRow label="About">{plan.description}</DetailRow>}
                  <div className="flex items-center gap-2 pt-3">
                    <TableAction onClick={() => setFormState(plan)}>Edit</TableAction>
                    <TableAction onClick={() => handleRemove(plan)} disabled={plans.length <= 1}>
                      {plans.length <= 1 ? "Only plan" : "Remove"}
                    </TableAction>
                  </div>
                </div>
              </ExpandableRow>
            ))}
          </div>
        )}
      </section>

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
