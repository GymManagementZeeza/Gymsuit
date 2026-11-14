"use client";

/* Nutritionist → client meal recommendations: latest plan, a short history, and the send form. */
import { useEffect, useState } from "react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { listMealPlans, sendMealPlan, type MealPlan } from "@/lib/nutritionist";
import { CheckCircle2, ChevronDown, Send, UtensilsCrossed, X } from "lucide-react";

const MEALS = [
  { key: "breakfast", label: "Breakfast", icon: "🍳", placeholder: "e.g. Oats with banana and nuts, 1 boiled egg, black coffee" },
  { key: "lunch", label: "Lunch", icon: "🥗", placeholder: "e.g. 1 cup rice, dal, grilled chicken, salad" },
  { key: "eveningSnack", label: "Evening snack", icon: "🍎", placeholder: "e.g. Apple and a handful of almonds, green tea" },
  { key: "dinner", label: "Dinner", icon: "🍲", placeholder: "e.g. 2 chapatis, vegetable curry, curd" },
] as const;

type MealKey = (typeof MEALS)[number]["key"];
type Draft = Record<MealKey, string> & { date: string; note: string };

const fieldClass =
  "w-full border border-[#d8d8d1] bg-white px-3 py-2 text-sm outline-none transition-colors focus:border-[#24241f]";

function localIsoDate(d = new Date()) {
  const tz = d.getTimezoneOffset() * 60000;
  return new Date(d.getTime() - tz).toISOString().slice(0, 10);
}

function dayLabel(iso: string) {
  const today = localIsoDate();
  if (iso === today) return "Today";
  const tomorrow = localIsoDate(new Date(Date.now() + 86400000));
  if (iso === tomorrow) return "Tomorrow";
  return new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, { weekday: "short", day: "numeric", month: "short" });
}

function emptyDraft(date = localIsoDate()): Draft {
  return { date, breakfast: "", lunch: "", eveningSnack: "", dinner: "", note: "" };
}

export default function MealPlanPanel({ memberId, clientName }: { memberId: number; clientName: string }) {
  const [plans, setPlans] = useState<MealPlan[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<Draft>(() => emptyDraft());
  const [sending, setSending] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [sentFor, setSentFor] = useState<string | null>(null);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(memberId)) return;
    listMealPlans(memberId)
      .then(setPlans)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load meal plans."))
      .finally(() => setLoaded(true));
  }, [memberId]);

  const latest = plans[0] ?? null;

  const openForm = (date = localIsoDate()) => {
    // Editing a day that already has a plan starts from what was sent.
    const existing = plans.find((p) => p.date === date);
    setDraft(
      existing
        ? {
            date,
            breakfast: existing.breakfast ?? "",
            lunch: existing.lunch ?? "",
            eveningSnack: existing.eveningSnack ?? "",
            dinner: existing.dinner ?? "",
            note: existing.note ?? "",
          }
        : emptyDraft(date),
    );
    setFormError(null);
    setOpen(true);
  };

  const changeDate = (date: string) => {
    const existing = plans.find((p) => p.date === date);
    setDraft((d) =>
      existing
        ? {
            date,
            breakfast: existing.breakfast ?? "",
            lunch: existing.lunch ?? "",
            eveningSnack: existing.eveningSnack ?? "",
            dinner: existing.dinner ?? "",
            note: existing.note ?? "",
          }
        : { ...d, date },
    );
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (sending) return;
    if (MEALS.every((m) => !draft[m.key].trim())) {
      setFormError("Add a recommendation for at least one meal.");
      return;
    }
    setFormError(null);
    setSending(true);
    try {
      const saved = await sendMealPlan(memberId, {
        date: draft.date,
        breakfast: draft.breakfast.trim() || undefined,
        lunch: draft.lunch.trim() || undefined,
        eveningSnack: draft.eveningSnack.trim() || undefined,
        dinner: draft.dinner.trim() || undefined,
        note: draft.note.trim() || undefined,
      });
      setPlans((prev) =>
        [saved, ...prev.filter((p) => p.date !== saved.date)].sort((a, b) => b.date.localeCompare(a.date)),
      );
      setSentFor(dayLabel(saved.date));
      setOpen(false);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Could not send the meal plan.");
    } finally {
      setSending(false);
    }
  };

  return (
    <section className="mt-6 border border-[#e1e1da] bg-white">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e1e1da] p-4">
        <div className="flex items-center gap-3">
          <span className="grid size-10 place-items-center bg-[#f4fde3] text-[#25251f]">
            <UtensilsCrossed className="size-5" />
          </span>
          <div>
            <p className="ledger-label">Meal plan</p>
            <p className="mt-0.5 text-sm text-[#76766f]">
              Breakfast, lunch, evening snack and dinner — {clientName || "your client"} sees it in the GymSuit app.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => openForm()}
          className="flex h-10 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
        >
          <Send className="size-4" /> Send meal plan
        </button>
      </div>

      {sentFor && (
        <p className="flex items-center gap-2 border-b border-[#e1e1da] bg-[#f4fde3] px-4 py-3 text-sm text-[#24241f]">
          <CheckCircle2 className="size-4 shrink-0 text-[#3d7a52]" />
          Sent the plan for {sentFor.toLowerCase()}. {clientName || "Your client"} got an email and an app notification.
          <button type="button" onClick={() => setSentFor(null)} className="ml-auto text-[#76766f] hover:text-[#24241f]" aria-label="Dismiss">
            <X className="size-4" />
          </button>
        </p>
      )}

      {error && (
        <div className="p-4">
          <ErrorBanner message={error} />
        </div>
      )}

      {!loaded && <div className="m-4 h-16 animate-pulse bg-[#efefe9]" />}

      {loaded && !error && !latest && (
        <p className="p-4 text-sm text-[#76766f]">No meal plans sent yet.</p>
      )}

      {latest && (
        <div className="p-4">
          <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
            <p className="text-sm font-bold">Latest · {dayLabel(latest.date)}</p>
            <button type="button" onClick={() => openForm(latest.date)} className="text-xs font-bold text-[#0b5a8a] hover:underline">
              Edit &amp; resend
            </button>
          </div>
          <PlanGrid plan={latest} />
          {latest.note && <p className="mt-3 border-l-2 border-[#c7f36a] pl-3 text-sm text-[#4b4b44]">{latest.note}</p>}

          {plans.length > 1 && (
            <div className="mt-4 border-t border-[#efefe9] pt-3">
              <button
                type="button"
                onClick={() => setShowHistory((v) => !v)}
                className="flex items-center gap-1 text-xs font-bold text-[#76766f] hover:text-[#24241f]"
                aria-expanded={showHistory}
              >
                <ChevronDown className={`size-4 transition-transform ${showHistory ? "rotate-180" : ""}`} />
                Earlier plans ({plans.length - 1})
              </button>
              {showHistory && (
                <div className="mt-3 flex flex-col gap-4">
                  {plans.slice(1).map((plan) => (
                    <div key={plan.id}>
                      <p className="mb-2 text-xs font-bold text-[#76766f]">{dayLabel(plan.date)}</p>
                      <PlanGrid plan={plan} compact />
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {open && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-[#141410]/50 sm:items-center sm:p-4">
          <form
            onSubmit={submit}
            className="flex max-h-[92vh] w-full max-w-lg flex-col border border-[#d8d8d1] bg-white"
            role="dialog"
            aria-modal="true"
            aria-label="Send meal plan"
          >
            <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
              <div>
                <p className="ledger-label">Meal plan</p>
                <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">For {clientName || "your client"}</h2>
              </div>
              <button type="button" onClick={() => setOpen(false)} className="grid size-8 place-items-center transition hover:bg-[#efefe9]" aria-label="Close">
                <X className="size-4" />
              </button>
            </div>

            <div className="flex flex-col gap-4 overflow-y-auto p-5">
              <label className="flex flex-col gap-1.5">
                <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Day</span>
                <input
                  type="date"
                  required
                  min={localIsoDate()}
                  value={draft.date}
                  onChange={(e) => changeDate(e.target.value)}
                  className={`${fieldClass} h-10`}
                />
              </label>
              {MEALS.map((meal) => (
                <label key={meal.key} className="flex flex-col gap-1.5">
                  <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
                    <span className="mr-1 text-sm">{meal.icon}</span> {meal.label}
                  </span>
                  <textarea
                    rows={2}
                    maxLength={2000}
                    value={draft[meal.key]}
                    onChange={(e) => setDraft((d) => ({ ...d, [meal.key]: e.target.value }))}
                    placeholder={meal.placeholder}
                    className={`${fieldClass} resize-y`}
                  />
                </label>
              ))}
              <label className="flex flex-col gap-1.5">
                <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Note (optional)</span>
                <textarea
                  rows={2}
                  maxLength={2000}
                  value={draft.note}
                  onChange={(e) => setDraft((d) => ({ ...d, note: e.target.value }))}
                  placeholder="e.g. Drink 3 litres of water. Keep portions moderate at dinner."
                  className={`${fieldClass} resize-y`}
                />
              </label>
              <p className="text-[11px] leading-5 text-[#76766f]">
                {clientName || "Your client"} gets an email and an app notification, and sees the plan on the Food tab in the
                GymSuit app. Sending again for the same day replaces that day&apos;s plan.
              </p>
              {formError && <ErrorBanner message={formError} />}
            </div>

            <div className="flex justify-end gap-2 border-t border-[#e5e5de] p-4">
              <button type="button" onClick={() => setOpen(false)} className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]">
                Cancel
              </button>
              <button
                type="submit"
                disabled={sending}
                className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
              >
                <Send className="size-3.5" /> {sending ? "Sending…" : "Send to client"}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

function PlanGrid({ plan, compact = false }: { plan: MealPlan; compact?: boolean }) {
  return (
    <div className={`grid gap-2 ${compact ? "sm:grid-cols-4" : "sm:grid-cols-2"}`}>
      {MEALS.map((meal) => {
        const text = plan[meal.key];
        return (
          <div key={meal.key} className="border border-[#efefe9] bg-[#fafaf6] p-3">
            <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
              <span className="mr-1 text-sm">{meal.icon}</span> {meal.label}
            </p>
            <p className={`mt-1 whitespace-pre-line text-sm ${text ? "text-[#24241f]" : "text-[#a3a39a]"}`}>{text || "—"}</p>
          </div>
        );
      })}
    </div>
  );
}
