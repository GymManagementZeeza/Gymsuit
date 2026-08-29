"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import type { Member } from "@/lib/members";
import { recordTransaction, type GymTransactionRecord, type TransactionDirection } from "@/lib/transactions";
import type { PaymentMethod } from "@/lib/memberPayments";

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CASH", label: "Cash" },
  { value: "UPI", label: "UPI" },
  { value: "BANK_TRANSFER", label: "Bank transfer" },
  { value: "OTHER", label: "Other" },
];

export default function RecordTransactionModal({
  gymId,
  members,
  onClose,
  onSaved,
}: {
  gymId: number;
  members: Member[];
  onClose: () => void;
  onSaved: (transaction: GymTransactionRecord) => void;
}) {
  const [direction, setDirection] = useState<TransactionDirection>("EXPENSE");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const data = new FormData(event.currentTarget);
    const memberId = data.get("memberId") as string;

    try {
      const saved = await recordTransaction(gymId, {
        direction,
        description: data.get("description") as string,
        amount: Number(data.get("amount")),
        currency: (data.get("currency") as string) || "INR",
        paymentMethod: data.get("paymentMethod") as PaymentMethod,
        memberId: memberId ? Number(memberId) : undefined,
        occurredOn: data.get("occurredOn") as string,
        notes: (data.get("notes") as string) || undefined,
      });
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not record this transaction.");
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Payments</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Record a payment</h2>
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

        <form className="flex flex-col gap-5 p-5" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setDirection("EXPENSE")}
              className={`h-10 border text-sm font-bold transition ${
                direction === "EXPENSE" ? "border-[#24241f] bg-[#24241f] text-white" : "border-[#d8d8d1] bg-white"
              }`}
            >
              Expense
            </button>
            <button
              type="button"
              onClick={() => setDirection("INCOME")}
              className={`h-10 border text-sm font-bold transition ${
                direction === "INCOME" ? "border-[#24241f] bg-[#24241f] text-white" : "border-[#d8d8d1] bg-white"
              }`}
            >
              Income
            </button>
          </div>
          <p className="-mt-3 text-xs text-[#76766f]">
            {direction === "EXPENSE"
              ? "Money the gym paid out — rent, repairs, supplies."
              : "Money the gym received outside membership plans — parking, a protein shake, a day pass."}
          </p>

          <label className="flex flex-col gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Description</span>
            <input
              name="description"
              required
              placeholder={direction === "EXPENSE" ? "Plumbing repair" : "Parking fee"}
              className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
            />
          </label>

          <div className="grid grid-cols-3 gap-3">
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Amount</span>
              <input
                name="amount"
                type="number"
                min="0.01"
                step="0.01"
                required
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Currency</span>
              <input
                name="currency"
                required
                defaultValue="INR"
                maxLength={3}
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm uppercase outline-none transition-colors focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Method</span>
              <select
                name="paymentMethod"
                defaultValue="CASH"
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
              >
                {PAYMENT_METHODS.map((m) => (
                  <option key={m.value} value={m.value}>
                    {m.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
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
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
                Member (optional)
              </span>
              <select
                name="memberId"
                defaultValue=""
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
              >
                <option value="">None</option>
                {members.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.firstName} {m.lastName}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="flex flex-col gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Notes (optional)</span>
            <input
              name="notes"
              placeholder="Any extra context"
              className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
            />
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
              type="submit"
              disabled={loading}
              className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
            >
              {loading ? "Saving…" : "Record payment"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
