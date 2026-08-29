"use client";

import { Tag } from "lucide-react";
import type { MembershipPlan } from "@/lib/membershipPlans";

export default function PlanPicker({
  plans,
  selectedId,
  onSelect,
}: {
  plans: MembershipPlan[];
  selectedId: number | null;
  onSelect: (id: number) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      {plans.map((plan) => (
        <button
          key={plan.id}
          type="button"
          onClick={() => onSelect(plan.id)}
          className={`flex items-center justify-between gap-3 border px-4 py-3 text-left transition ${
            selectedId === plan.id ? "border-[#24241f] bg-[#fafaf6]" : "border-[#d8d8d1] hover:bg-[#fafaf6]"
          }`}
        >
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-full bg-[#c7f36a]/30 text-[#5a7a1a]">
              <Tag className="size-4" />
            </span>
            <div>
              <p className="text-sm font-bold">{plan.name}</p>
              <p className="text-xs text-[#76766f]">
                {plan.currency} {plan.price} · {plan.billingCycle.replace("_", " ").toLowerCase()}
              </p>
            </div>
          </div>
          <span
            className={`size-4 shrink-0 rounded-full border-2 ${
              selectedId === plan.id ? "border-[#24241f] bg-[#24241f]" : "border-[#c9c9c1]"
            }`}
          />
        </button>
      ))}
    </div>
  );
}
