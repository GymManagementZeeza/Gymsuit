"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { X, IndianRupee } from "lucide-react";
import ErrorBanner from "@/components/ErrorBanner";
import { gymToInput, updateGym, type Gym } from "@/lib/gyms";

export default function UpiIdModal({
  gymId,
  gym,
  onClose,
  onSaved,
  onSkip,
}: {
  gymId: number;
  gym: Gym;
  onClose: () => void;
  onSaved: (gym: Gym) => void;
  /** When provided, the secondary button reads "Add later" instead of "Cancel" — for the onboarding flow. */
  onSkip?: () => void;
}) {
  const [upiId, setUpiId] = useState(gym.upiId ?? "");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const saved = await updateGym(gymId, { ...gymToInput(gym), upiId: upiId.trim() || undefined });
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save the UPI ID.");
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="w-full max-w-sm rounded-2xl border border-ink/15 bg-white p-5">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-saffron/10 text-saffron">
              <IndianRupee className="h-4.5 w-4.5" />
            </span>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Payments</p>
              <h2 className="font-display text-lg font-bold tracking-[-0.02em] text-ink">Your UPI ID</h2>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid h-8 w-8 shrink-0 place-items-center rounded-lg transition-colors hover:bg-sand"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <p className="mt-4 text-sm leading-6 text-ink/60">
          Used to generate a UPI QR code for one-time charges, like a new member&apos;s joining fee.
        </p>

        <form className="mt-5 flex flex-col gap-4" onSubmit={handleSubmit}>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">UPI ID</span>
            <input
              value={upiId}
              onChange={(e) => setUpiId(e.target.value)}
              placeholder="yourgym@upi"
              className="h-10 rounded-xl border border-ink/15 bg-white px-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron"
            />
          </label>

          {error && <ErrorBanner message={error} />}

          <div className="flex items-center justify-end gap-2 pt-1">
            {onSkip ? (
              <button type="button" onClick={onSkip} className="button-quiet px-4 py-2 text-xs">
                Add later
              </button>
            ) : (
              <button type="button" onClick={onClose} className="button-quiet px-4 py-2 text-xs">
                Cancel
              </button>
            )}
            <button type="submit" disabled={loading} className="button-saffron px-4 py-2 text-xs disabled:opacity-60">
              {loading ? "Saving…" : "Save"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
