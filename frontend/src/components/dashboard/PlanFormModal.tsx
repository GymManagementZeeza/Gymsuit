"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { TriangleAlert, X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import {
  createMembershipPlan,
  updateMembershipPlan,
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

export default function PlanFormModal({
  gymId,
  plan,
  onClose,
  onSaved,
}: {
  gymId: number;
  plan: MembershipPlan | null;
  onClose: () => void;
  onSaved: (plan: MembershipPlan) => void;
}) {
  const isEdit = plan !== null;
  const [price, setPrice] = useState(plan?.price != null ? String(plan.price) : "");
  const [confirmPriceChange, setConfirmPriceChange] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const priceChanged = isEdit && Number(price) !== plan!.price && price !== "";

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (priceChanged && !confirmPriceChange) {
      setError("Please confirm you understand this changes what current members are charged.");
      return;
    }

    setLoading(true);
    const data = new FormData(event.currentTarget);
    const input = {
      name: data.get("name") as string,
      description: (data.get("description") as string) || undefined,
      price: Number(data.get("price")),
      currency: data.get("currency") as string,
      billingCycle: data.get("billingCycle") as BillingCycle,
    };

    try {
      const saved = isEdit ? await updateMembershipPlan(gymId, plan!.id, input) : await createMembershipPlan(gymId, input);
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save this plan.");
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="w-full max-w-lg border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">{isEdit ? "Edit plan" : "New plan"}</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">{isEdit ? plan!.name : "Add a plan"}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-8 place-items-center transition hover:bg-[#efefe9]"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>

        <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
          <div className="grid gap-3 sm:grid-cols-4">
            <label className="flex flex-col gap-1.5 sm:col-span-2">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Plan name</span>
              <input
                name="name"
                required
                defaultValue={plan?.name ?? ""}
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
                value={price}
                onChange={(event) => {
                  setPrice(event.target.value);
                  setConfirmPriceChange(false);
                }}
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Currency</span>
              <input
                name="currency"
                required
                defaultValue={plan?.currency ?? "INR"}
                maxLength={3}
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm uppercase outline-none focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5 sm:col-span-2">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Billing cycle</span>
              <select
                name="billingCycle"
                defaultValue={plan?.billingCycle ?? "MONTHLY"}
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
                defaultValue={plan?.description ?? ""}
                placeholder="Unlimited classes, all locations"
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
              />
            </label>
          </div>

          {priceChanged && (
            <label className="flex cursor-pointer items-start gap-3 border border-[#f0b25a] bg-[#fff6e6] p-3">
              <TriangleAlert className="mt-0.5 size-4 shrink-0 text-[#8f5a13]" />
              <span className="flex flex-col gap-1">
                <span className="text-xs font-bold text-[#5c3c0d]">This changes what current members pay</span>
                <span className="text-[11px] text-[#6b4d1c]">
                  Every member currently on <strong>{plan?.name}</strong> will immediately be charged{" "}
                  {plan?.currency ?? "INR"} {price || "0"} instead of {plan?.currency} {plan?.price} — there&apos;s no
                  separate step, this takes effect as soon as you save.
                </span>
                <span className="mt-1 flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={confirmPriceChange}
                    onChange={(event) => setConfirmPriceChange(event.target.checked)}
                    className="size-4 accent-[#8f5a13]"
                  />
                  <span className="text-[11px] font-semibold text-[#5c3c0d]">I understand, update the price</span>
                </span>
              </span>
            </label>
          )}

          {error && <ErrorBanner message={error} />}

          <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
            <button
              type="button"
              onClick={onClose}
              className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || (priceChanged && !confirmPriceChange)}
              className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
            >
              {loading ? "Saving…" : isEdit ? "Save changes" : "Add plan"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
