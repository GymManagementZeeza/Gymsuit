"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import PlanPicker from "@/components/dashboard/PlanPicker";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import type { Member } from "@/lib/members";
import { listMembershipPlans, type MembershipPlan } from "@/lib/membershipPlans";
import { changeMemberPlan, getCurrentSubscription } from "@/lib/memberSubscriptions";

export default function ChangePlanModal({
  gymId,
  member,
  onClose,
  onChanged,
}: {
  gymId: number;
  member: Member;
  onClose: () => void;
  onChanged: () => void;
}) {
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [currentPlanId, setCurrentPlanId] = useState<number | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([listMembershipPlans(gymId), getCurrentSubscription(gymId, member.id)])
      .then(([allPlans, current]) => {
        setPlans(allPlans.filter((p) => p.active));
        setCurrentPlanId(current?.planId ?? null);
        setSelectedId(current?.planId ?? null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load plans."))
      .finally(() => setLoading(false));
  }, [gymId, member.id]);

  const handleSave = async () => {
    if (!selectedId) return;
    setSaving(true);
    setError(null);
    try {
      await changeMemberPlan(gymId, member.id, selectedId);
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not change this member's plan.");
      setSaving(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-md overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Membership plan</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {member.firstName} {member.lastName}
            </h2>
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
          {loading && <p className="text-sm text-[#76766f]">Loading plans…</p>}

          {!loading && plans.length === 0 && (
            <p className="rounded-xl border border-dashed border-[#d8d8d1] p-4 text-sm text-[#76766f]">
              This gym doesn&apos;t have any active membership plans.
            </p>
          )}

          {!loading && plans.length > 0 && (
            <>
              <PlanPicker plans={plans} selectedId={selectedId} onSelect={setSelectedId} />
              <p className="text-xs text-[#76766f]">
                Switching plans cancels their current one and starts the new one as awaiting payment — take payment
                from the member list to activate it.
              </p>
            </>
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
              type="button"
              onClick={handleSave}
              disabled={!selectedId || selectedId === currentPlanId || saving || loading}
              className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save plan"}
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}
