"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { ChevronLeft, X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { getTrainerCompensation, type TrainerCompensation } from "@/lib/trainerCompensation";
import { getManagerCompensation, type ManagerCompensation } from "@/lib/managerCompensation";
import { recordTransaction } from "@/lib/transactions";
import { paymentMethodLabel, type PaymentMethod } from "@/lib/memberPayments";

const PAYMENT_METHODS: PaymentMethod[] = ["CASH", "UPI", "BANK_TRANSFER", "STRIPE", "RAZORPAY", "OTHER"];

export type PayableStaff = {
  id: number;
  firstName: string;
  lastName: string;
  role: "Trainer" | "Manager";
  subtitle?: string;
};

type Compensation = TrainerCompensation | ManagerCompensation;

function payLabel(compensation: Compensation | null) {
  if (!compensation) return "No pay rate set";
  if (compensation.payType === "HOURLY") return `${compensation.currency} ${compensation.hourlyRate} / hour`;
  return `${compensation.currency} ${compensation.monthlySalary} / month`;
}

function getCompensation(gymId: number, staff: PayableStaff) {
  return staff.role === "Trainer" ? getTrainerCompensation(gymId, staff.id) : getManagerCompensation(gymId, staff.id);
}

function PayForm({
  gymId,
  staff,
  onPaid,
  onBack,
}: {
  gymId: number;
  staff: PayableStaff;
  onPaid: () => void;
  onBack: () => void;
}) {
  const [compensation, setCompensation] = useState<Compensation | null>(null);
  const [loadingCompensation, setLoadingCompensation] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getCompensation(gymId, staff)
      .then(setCompensation)
      .catch(() => {})
      .finally(() => setLoadingCompensation(false));
  }, [gymId, staff]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSaving(true);

    const data = new FormData(event.currentTarget);
    const amount = Number(data.get("amount"));
    const currency = data.get("currency") as string;
    const paymentMethod = data.get("paymentMethod") as PaymentMethod;
    const occurredOn = data.get("occurredOn") as string;
    const notes = (data.get("notes") as string) || undefined;

    try {
      await recordTransaction(gymId, {
        direction: "EXPENSE",
        description: `Pay — ${staff.firstName} ${staff.lastName}`,
        amount,
        currency,
        paymentMethod,
        trainerId: staff.role === "Trainer" ? staff.id : undefined,
        managerId: staff.role === "Manager" ? staff.id : undefined,
        occurredOn,
        notes,
      });
      onPaid();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not record this payment.");
      setSaving(false);
    }
  };

  const suggestedAmount = compensation?.payType === "SALARY" ? compensation.monthlySalary : undefined;

  return (
    <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
      <button
        type="button"
        onClick={onBack}
        className="flex items-center gap-1 self-start text-xs font-bold text-[#6e6e67] transition hover:text-[#24241f]"
      >
        <ChevronLeft className="size-3.5" /> Back to staff
      </button>

      <p className="text-sm text-[#5a5a54]">
        Pay rate: <span className="font-semibold">{loadingCompensation ? "Loading…" : payLabel(compensation)}</span>
      </p>

      <div className="grid grid-cols-2 gap-3">
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Amount</span>
          <input
            name="amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            defaultValue={suggestedAmount ?? ""}
            placeholder="500"
            className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
          />
        </label>
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Currency</span>
          <input
            name="currency"
            required
            defaultValue={compensation?.currency ?? "INR"}
            placeholder="INR"
            className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
          />
        </label>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Payment method</span>
          <select
            name="paymentMethod"
            required
            defaultValue="BANK_TRANSFER"
            className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
          >
            {PAYMENT_METHODS.map((method) => (
              <option key={method} value={method}>
                {paymentMethodLabel(method)}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Date</span>
          <input
            name="occurredOn"
            type="date"
            required
            defaultValue={new Date().toISOString().slice(0, 10)}
            className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
          />
        </label>
      </div>

      <label className="flex flex-col gap-1.5">
        <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Notes (optional)</span>
        <input
          name="notes"
          placeholder="e.g. August salary, or 40 hrs @ 500/hr"
          className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
        />
      </label>

      {error && <ErrorBanner message={error} />}

      <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
        <button
          type="submit"
          disabled={saving}
          className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
        >
          {saving ? "Recording…" : "Record payment"}
        </button>
      </div>
    </form>
  );
}

export default function StaffPayModal({
  gymId,
  staff,
  initialSelected,
  onClose,
  onPaid,
}: {
  gymId: number;
  staff: PayableStaff[];
  initialSelected?: PayableStaff;
  onClose: () => void;
  onPaid: () => void;
}) {
  const [selected, setSelected] = useState<PayableStaff | null>(initialSelected ?? null);
  const [paidKey, setPaidKey] = useState<string | null>(null);

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Pay staff</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {selected ? `${selected.firstName} ${selected.lastName}` : "Who are you paying?"}
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

        {selected ? (
          <PayForm
            gymId={gymId}
            staff={selected}
            onPaid={() => {
              setPaidKey(`${selected.role}-${selected.id}`);
              setSelected(null);
              onPaid();
            }}
            onBack={() => setSelected(null)}
          />
        ) : (
          <>
            {staff.length === 0 && (
              <p className="p-8 text-center text-sm text-[#76766f]">No trainers or managers yet — add one first.</p>
            )}
            <div className="divide-y divide-[#ebebe5]">
              {staff.map((person) => {
                const key = `${person.role}-${person.id}`;
                return (
                  <button
                    key={key}
                    onClick={() => setSelected(person)}
                    className="flex w-full items-center gap-3 p-4 text-left transition hover:bg-[#fafaf6]"
                  >
                    <span className="grid size-9 shrink-0 place-items-center rounded-full bg-[#e1e1dd] text-[10px] font-bold">
                      {person.firstName[0]}
                      {person.lastName[0]}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block text-sm font-bold">
                        {person.firstName} {person.lastName}
                      </span>
                      <span className="mt-0.5 block text-xs text-[#74746d]">{person.subtitle || person.role}</span>
                    </span>
                    {paidKey === key && <span className="text-xs font-semibold text-[#4c592e]">Paid</span>}
                  </button>
                );
              })}
            </div>
          </>
        )}
      </div>
    </div>,
    document.body
  );
}
