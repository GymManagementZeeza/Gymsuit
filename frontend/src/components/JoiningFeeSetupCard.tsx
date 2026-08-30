"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Wallet } from "lucide-react";
import AuthShell from "@/components/AuthShell";
import ErrorBanner from "@/components/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { dashboardPathForRole } from "@/lib/auth";
import { getGym, gymToInput, updateGym, type Gym } from "@/lib/gyms";

export default function JoiningFeeSetupCard() {
  const router = useRouter();
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [gym, setGym] = useState<Gym | null>(null);
  const [loadingGym, setLoadingGym] = useState(true);
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("INR");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!gymId) return;
    getGym(gymId)
      .then((data) => {
        setGym(data);
        if (data.joiningFee != null) setAmount(String(data.joiningFee));
        if (data.joiningFeeCurrency) setCurrency(data.joiningFeeCurrency);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your gym."))
      .finally(() => setLoadingGym(false));
  }, [gymId]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!gymId || !gym) return;
    setError(null);

    const value = Number(amount);
    if (amount === "" || Number.isNaN(value) || value < 0) {
      setError("Enter a joining fee amount (0 or more) to continue.");
      return;
    }

    setSaving(true);
    try {
      await updateGym(gymId, { ...gymToInput(gym), joiningFee: value, joiningFeeCurrency: currency.trim().toUpperCase() || "INR" });
      if (session) router.push(dashboardPathForRole(session.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save the joining fee.");
      setSaving(false);
    }
  };

  return (
    <AuthShell
      eyebrow="One last step"
      headline="Set your joining fee."
      description="A one-time fee every new member pays to finish registration. Enter 0 if you don't want to charge one — you can change this anytime in Settings."
      panelWidth="max-w-[480px]"
    >
      <form className="mt-9 flex flex-col gap-5" onSubmit={handleSubmit}>
        <div className="flex items-center gap-3 rounded-xl border border-ink/15 bg-white px-4 py-3.5">
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-saffron/10 text-saffron">
            <Wallet className="h-4 w-4" />
          </span>
          <p className="text-xs text-ink/55">
            Charged the next time you add a member — they can&apos;t finish registration until it&apos;s paid.
          </p>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <label className="col-span-2 flex flex-col gap-1.5">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Amount</span>
            <input
              type="number"
              min="0"
              step="0.01"
              required
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              placeholder="1000"
              disabled={loadingGym}
              className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium text-ink outline-none transition-colors placeholder:text-ink/35 focus:border-saffron disabled:opacity-60"
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-bold uppercase tracking-[0.13em] text-ink/50">Currency</span>
            <input
              value={currency}
              onChange={(event) => setCurrency(event.target.value)}
              maxLength={3}
              disabled={loadingGym}
              className="w-full rounded-xl border border-ink/15 bg-white px-4 py-3 text-sm font-medium uppercase text-ink outline-none transition-colors focus:border-saffron disabled:opacity-60"
            />
          </label>
        </div>

        {error && <ErrorBanner message={error} />}

        <button
          type="submit"
          disabled={saving || loadingGym || !gymId}
          className="button-saffron mt-1 w-full justify-center py-3.5 text-base disabled:opacity-60"
        >
          {saving ? "Saving…" : "Continue to dashboard"} <ArrowRight className="h-4 w-4" />
        </button>
      </form>
    </AuthShell>
  );
}
