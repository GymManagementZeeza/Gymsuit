"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { TriangleAlert, X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import {
  getPlanCurrentMembers,
  reassignAndDeleteMembershipPlan,
  type MembershipPlan,
  type PlanMember,
} from "@/lib/membershipPlans";

export default function PlanReassignModal({
  gymId,
  plan,
  otherPlans,
  onClose,
  onDeleted,
}: {
  gymId: number;
  plan: MembershipPlan;
  otherPlans: MembershipPlan[];
  onClose: () => void;
  onDeleted: (planId: number) => void;
}) {
  const [members, setMembers] = useState<PlanMember[]>([]);
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [replacementId, setReplacementId] = useState<number | "">(otherPlans[0]?.id ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getPlanCurrentMembers(gymId, plan.id)
      .then(setMembers)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load members on this plan."))
      .finally(() => setLoadingMembers(false));
  }, [gymId, plan.id]);

  const handleConfirm = async () => {
    if (!replacementId) {
      setError("Choose a plan to move these members to.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await reassignAndDeleteMembershipPlan(gymId, plan.id, Number(replacementId));
      onDeleted(plan.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not move members and delete this plan.");
      setSubmitting(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Remove plan</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">{plan.name}</h2>
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

        <div className="flex flex-col gap-5 p-5">
          <div className="flex items-start gap-3 border border-[#f0b25a] bg-[#fff6e6] p-3">
            <TriangleAlert className="mt-0.5 size-4 shrink-0 text-[#8f5a13]" />
            <p className="text-[11px] text-[#6b4d1c]">
              {loadingMembers
                ? "Checking who's on this plan…"
                : members.length > 0
                  ? `${members.length} member${members.length === 1 ? " is" : "s are"} currently on this plan. Pick a plan to move them to — this can't be undone.`
                  : "No members are currently on this plan, but it has past subscription history that must move to another plan before it can be deleted."}
            </p>
          </div>

          {!loadingMembers && members.length > 0 && (
            <div className="divide-y divide-[#ebebe5] border border-[#e5e5de]">
              {members.map((member) => (
                <div key={member.memberId} className="flex items-center justify-between px-3 py-2 text-sm">
                  <span className="font-medium">
                    {member.firstName} {member.lastName}
                  </span>
                  <span className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8a8a82]">
                    {member.status}
                  </span>
                </div>
              ))}
            </div>
          )}

          <label className="flex flex-col gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Move them to</span>
            <select
              value={replacementId}
              onChange={(event) => setReplacementId(event.target.value ? Number(event.target.value) : "")}
              className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none focus:border-[#24241f]"
            >
              <option value="">Choose a plan…</option>
              {otherPlans.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.name} — {option.currency} {option.price}
                </option>
              ))}
            </select>
          </label>

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
              type="button"
              onClick={handleConfirm}
              disabled={submitting || loadingMembers || !replacementId}
              className="flex h-9 items-center gap-2 bg-[#f0b25a] px-4 text-xs font-bold text-[#3a2506] transition hover:bg-[#f5c37e] disabled:opacity-60"
            >
              {submitting ? "Moving…" : "Move members & delete plan"}
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}
