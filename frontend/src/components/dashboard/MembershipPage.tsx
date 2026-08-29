"use client";

/* Training Ledger page: the gym's real membership plans — add, price, and retire them. */
import { useEffect, useState } from "react";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, TableAction } from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import {
  createMembershipPlan,
  deleteMembershipPlan,
  listMembershipPlans,
  type BillingCycle,
  type MembershipPlan,
} from "@/lib/membershipPlans";
import { Layers, Plus, Tag, X } from "lucide-react";

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
  const [formOpen, setFormOpen] = useState(false);

  useEffect(() => {
    if (!gymId) return;
    listMembershipPlans(gymId)
      .then(setPlans)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load plans."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const handleAdd = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!gymId) return;
    setError(null);

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
      setFormOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not add this plan.");
    }
  };

  const handleRemove = async (plan: MembershipPlan) => {
    if (!gymId) return;
    if (plans.length <= 1) return;
    setError(null);
    try {
      await deleteMembershipPlan(gymId, plan.id);
      setPlans((prev) => prev.filter((p) => p.id !== plan.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not remove this plan.");
    }
  };

  const activeCount = plans.filter((p) => p.active).length;

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow="Finance · Membership plans"
        title="The business of belonging."
        description="Every plan members can join, and what it costs them."
        actions={
          <ActionButton icon={<Plus className="size-4" />} onClick={() => setFormOpen((open) => !open)}>
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

      {formOpen && (
        <section className="border border-dashed border-[#d8d8d1] bg-white p-5">
          <div className="mb-4 flex items-center justify-between">
            <p className="ledger-label">New plan</p>
            <button type="button" onClick={() => setFormOpen(false)} className="grid size-7 place-items-center hover:bg-[#efefe9]" aria-label="Close">
              <X className="size-4" />
            </button>
          </div>
          <form className="grid gap-3 sm:grid-cols-4" onSubmit={handleAdd}>
            <label className="flex flex-col gap-1.5 sm:col-span-2">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Plan name</span>
              <input
                name="name"
                required
                placeholder="Studio Monthly"
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Price</span>
              <input
                name="price"
                type="number"
                min="0"
                step="0.01"
                required
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Currency</span>
              <input
                name="currency"
                required
                defaultValue="INR"
                maxLength={3}
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm uppercase outline-none focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5 sm:col-span-2">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Billing cycle</span>
              <select
                name="billingCycle"
                defaultValue="MONTHLY"
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
              >
                {BILLING_CYCLES.map((cycle) => (
                  <option key={cycle.value} value={cycle.value}>
                    {cycle.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1.5 sm:col-span-2">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
                Description (optional)
              </span>
              <input
                name="description"
                placeholder="Unlimited classes, all locations"
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
              />
            </label>
            <div className="flex items-end sm:col-span-4">
              <button
                type="submit"
                className="flex h-10 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
              >
                Save plan
              </button>
            </div>
          </form>
        </section>
      )}

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
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-left">
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
                      <TableAction onClick={() => handleRemove(plan)} disabled={plans.length <= 1}>
                        {plans.length <= 1 ? "Only plan" : "Remove"}
                      </TableAction>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
