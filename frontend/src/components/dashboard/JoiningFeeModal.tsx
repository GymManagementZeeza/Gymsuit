"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { gymToInput, updateGym, type Gym } from "@/lib/gyms";

export default function JoiningFeeModal({
  gymId,
  gym,
  onClose,
  onSaved,
}: {
  gymId: number;
  gym: Gym;
  onClose: () => void;
  onSaved: (gym: Gym) => void;
}) {
  const [amount, setAmount] = useState(gym.joiningFee != null ? String(gym.joiningFee) : "");
  const [currency, setCurrency] = useState(gym.joiningFeeCurrency ?? "INR");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    const value = Number(amount);
    if (amount === "" || Number.isNaN(value) || value < 0) {
      setError("Enter a valid joining fee amount (0 or more).");
      return;
    }

    setLoading(true);
    try {
      const saved = await updateGym(gymId, {
        ...gymToInput(gym),
        joiningFee: value,
        joiningFeeCurrency: currency.trim().toUpperCase() || "INR",
      });
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save the joining fee.");
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="w-full max-w-sm border border-[#d8d8d1] bg-white p-5">
        <div className="flex items-start justify-between">
          <div>
            <p className="ledger-label">Membership</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Joining fee</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-8 shrink-0 place-items-center transition hover:bg-[#efefe9]"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>

        <p className="mt-3 text-xs leading-relaxed text-[#72726b]">
          A one-time fee every new member pays to finish registration. Changing this only affects members added
          from now on — it doesn&apos;t retroactively charge existing members.
        </p>

        <form className="mt-5 flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-3 gap-3">
            <label className="col-span-2 flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Amount</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="1000"
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Currency</span>
              <input
                value={currency}
                onChange={(event) => setCurrency(event.target.value)}
                maxLength={3}
                className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm uppercase outline-none transition-colors focus:border-[#24241f]"
              />
            </label>
          </div>

          {error && <ErrorBanner message={error} />}

          <div className="flex items-center justify-end gap-2 pt-1">
            <button type="button" onClick={onClose} className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]">
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
            >
              {loading ? "Saving…" : "Save"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
