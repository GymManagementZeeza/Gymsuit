"use client";

/* Training Ledger page: Member home — today's reset, weekly rhythm, weight trend, and membership. */
import { useEffect, useState } from "react";
import Link from "next/link";
import { MetricCard, StatusPill } from "@/components/dashboard/ui";
import { WeightChart } from "@/components/client/WeightChart";
import MonthlyCheckInCard from "@/components/client/MonthlyCheckInCard";
import { useSession } from "@/hooks/useSession";
import { getCurrentSubscription, type MemberSubscription } from "@/lib/memberSubscriptions";
import { getMember, updateMemberWeight, type Member } from "@/lib/members";
import { listWeightHistory, type WeightLog } from "@/lib/weightLogs";
import { CalendarDays, Dumbbell, Scale, Sparkles } from "lucide-react";

function daysBetween(a: Date, b: Date) {
  const msPerDay = 24 * 60 * 60 * 1000;
  const utcA = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate());
  const utcB = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate());
  return Math.round((utcB - utcA) / msPerDay);
}

function AccessCountdown({ subscription }: { subscription: MemberSubscription | null }) {
  if (!subscription) {
    return (
      <div className="bg-[#f6f6f0]/95 p-4 text-[#24241f]">
        <p className="text-sm font-bold">No active plan</p>
        <p className="mt-1 text-xs text-[#6e6e67]">Talk to the front desk to get set up on a plan.</p>
      </div>
    );
  }

  const today = new Date();
  const start = new Date(`${subscription.currentPeriodStart}T00:00:00`);
  const end = new Date(`${subscription.currentPeriodEnd}T00:00:00`);
  const totalDays = Math.max(1, daysBetween(start, end));
  const daysLeft = daysBetween(today, end);
  const elapsedDays = Math.min(totalDays, Math.max(0, totalDays - daysLeft));
  const remainingFraction = Math.min(1, Math.max(0, 1 - elapsedDays / totalDays));
  const label = daysLeft > 0 ? `${daysLeft}d left` : daysLeft === 0 ? "Ends today" : `${Math.abs(daysLeft)}d overdue`;
  const labelTone = daysLeft <= 0 ? "text-red-700" : daysLeft <= 3 ? "text-[#8f4513]" : "text-[#4c592e]";

  return (
    <div className="bg-[#f6f6f0]/95 p-4 text-[#24241f]">
      <div className="flex items-center justify-between text-xs font-bold">
        <span>{subscription.planName}</span>
        <span className={labelTone}>{label}</span>
      </div>
      <div className="mt-2.5 h-2 w-full overflow-hidden rounded-full bg-red-200">
        <div
          className="h-full rounded-full bg-[#7fb84a] transition-all"
          style={{ width: `${remainingFraction * 100}%` }}
        />
      </div>
      <div className="mt-1.5 flex items-center justify-between text-[10px] text-[#8a8a82]">
        <span>{subscription.currentPeriodStart}</span>
        <span>{subscription.currentPeriodEnd}</span>
      </div>
    </div>
  );
}

function WeightCard({
  gymId,
  memberId,
  onRecorded,
}: {
  gymId: number;
  memberId: number;
  onRecorded: () => void;
}) {
  const [weight, setWeight] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [input, setInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMember(gymId, memberId)
      .then((m) => setWeight(m.weightKg))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [gymId, memberId]);

  const handleSave = async () => {
    const value = parseFloat(input);
    if (!value || value <= 0) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateMemberWeight(gymId, memberId, value);
      setWeight(updated.weightKg);
      setEditing(false);
      setInput("");
      onRecorded();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save your weight.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <article className="border border-[#c7f36a] bg-[#fcfcf5] p-4 text-[#24241f]">
      <div className="flex items-center justify-between">
        <p className="ledger-label">My current weight</p>
        <Scale className="size-5" />
      </div>

      {loading ? (
        <p className="mt-3 text-sm text-[#8a8a82]">Loading…</p>
      ) : editing ? (
        <div className="mt-3 flex items-center gap-2">
          <input
            type="number"
            step="0.1"
            min="0"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="kg"
            autoFocus
            className="w-20 border border-[#d8d8d1] bg-white px-2 py-1.5 text-sm font-bold outline-none focus:border-[#24241f]"
          />
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="bg-[#24241f] px-3 py-1.5 text-xs font-bold text-white transition hover:bg-[#46463e] disabled:opacity-50"
          >
            {saving ? "Saving…" : "Save"}
          </button>
          <button
            type="button"
            onClick={() => {
              setEditing(false);
              setInput("");
            }}
            className="text-xs font-bold text-[#8a8a82] hover:text-[#24241f]"
          >
            Cancel
          </button>
        </div>
      ) : weight !== null ? (
        <>
          <p className="display-face mt-3 text-2xl leading-none">{weight} kg</p>
          <button
            type="button"
            onClick={() => {
              setInput(String(weight));
              setEditing(true);
            }}
            className="mt-2.5 text-xs font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4"
          >
            Update weight
          </button>
        </>
      ) : (
        <button
          type="button"
          onClick={() => setEditing(true)}
          className="mt-3 bg-[#24241f] px-3.5 py-2.5 text-sm font-bold text-white transition hover:bg-[#46463e]"
        >
          Record weight
        </button>
      )}

      {error && <p className="mt-2 text-xs text-red-700">{error}</p>}
    </article>
  );
}

export default function ClientHome() {
  const session = useSession();
  const [subscription, setSubscription] = useState<MemberSubscription | null>(null);
  const [loadingSubscription, setLoadingSubscription] = useState(true);
  const [weightHistory, setWeightHistory] = useState<WeightLog[]>([]);
  const [member, setMember] = useState<Member | null>(null);

  useEffect(() => {
    if (!session?.gymId || !session.memberId) return;
    getCurrentSubscription(session.gymId, session.memberId)
      .then(setSubscription)
      .catch(() => {})
      .finally(() => setLoadingSubscription(false));
  }, [session?.gymId, session?.memberId]);

  const refreshWeightHistory = () => {
    if (!session?.gymId || !session.memberId) return;
    listWeightHistory(session.gymId, session.memberId)
      .then(setWeightHistory)
      .catch(() => {});
  };

  useEffect(refreshWeightHistory, [session?.gymId, session?.memberId]);

  useEffect(() => {
    if (!session?.gymId || !session.memberId) return;
    getMember(session.gymId, session.memberId)
      .then(setMember)
      .catch(() => {});
  }, [session?.gymId, session?.memberId]);

  const daysLeft = subscription
    ? daysBetween(new Date(), new Date(`${subscription.currentPeriodEnd}T00:00:00`))
    : null;

  const latestLog = weightHistory[weightHistory.length - 1] ?? null;
  const monthStartLog = weightHistory.find((log) => {
    const recorded = new Date(log.recordedAt);
    const now = new Date();
    return recorded.getFullYear() === now.getFullYear() && recorded.getMonth() === now.getMonth();
  });
  const monthChange = monthStartLog && latestLog ? latestLog.weightKg - monthStartLog.weightKg : null;

  return (
    <div className="page-enter space-y-7">
      <section className="grid gap-4 lg:grid-cols-12">
        <div className="cut-corner relative overflow-hidden bg-[#24241f] p-6 text-white lg:col-span-7">
          <div className="absolute inset-0 dot-field opacity-[0.12]" aria-hidden="true" />
          <div
            className="absolute -right-16 -top-16 h-56 w-56 rounded-full border-[30px] border-[#c7f36a]/20"
            aria-hidden="true"
          />
          <div className="relative flex h-full flex-col justify-between gap-8">
            <div>
              <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Your gym access</p>
              <p className="display-face mt-3 text-4xl leading-[0.98]">
                {loadingSubscription
                  ? "Checking your access…"
                  : daysLeft === null
                    ? "No active plan"
                    : daysLeft > 0
                      ? (
                        <>
                          {daysLeft} day{daysLeft === 1 ? "" : "s"} left
                          <br />
                          on your plan.
                        </>
                      )
                      : (
                        <>
                          Time to
                          <br />
                          renew your plan.
                        </>
                      )}
              </p>
            </div>
            {!loadingSubscription && <AccessCountdown subscription={subscription} />}
          </div>
        </div>

        {session?.gymId && session.memberId && (
          <MonthlyCheckInCard gymId={session.gymId} memberId={session.memberId} />
        )}
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        {session?.gymId && session.memberId ? (
          <WeightCard gymId={session.gymId} memberId={session.memberId} onRecorded={refreshWeightHistory} />
        ) : (
          <MetricCard label="My current weight" value="—" detail="Loading…" tone="lime" icon={<Scale className="size-5" />} />
        )}
        <MetricCard label="Classes this month" value="9" detail="2 more than February" tone="paper" icon={<CalendarDays className="size-5" />} />
        <MetricCard label="Trainer sessions" value="3" detail="Next session · Sat 09:30" tone="blue" icon={<Dumbbell className="size-5" />} />
      </section>

      <section className="grid gap-4 lg:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white p-5 lg:col-span-8">
          <div className="flex items-center justify-between">
            <div>
              <p className="ledger-label">Weight trend</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Progress, not perfection.</h2>
            </div>
            <Link href="/clientdashboard/health" className="flex items-center gap-1 text-sm font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">
              My health →
            </Link>
          </div>
          <div className="mt-6">
            <WeightChart history={weightHistory} />
          </div>
          <div className="mt-6 grid grid-cols-3 gap-4 border-t border-[#e7e7e1] pt-5">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Goal</p>
              <p className="mt-1 text-sm font-bold">
                {member?.goalWeightKg != null ? (
                  `${member.goalWeightKg} kg`
                ) : (
                  <Link href="/clientdashboard/health" className="underline decoration-[#c7f36a] decoration-2 underline-offset-4">
                    Set a goal
                  </Link>
                )}
              </p>
            </div>
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">This month</p>
              <p className={`mt-1 text-sm font-bold ${monthChange !== null && monthChange < 0 ? "text-[#4c592e]" : monthChange !== null && monthChange > 0 ? "text-[#8f4513]" : ""}`}>
                {monthChange === null ? "—" : `${monthChange > 0 ? "+" : ""}${monthChange.toFixed(1)} kg`}
              </p>
            </div>
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Last log</p>
              <p className="mt-1 text-sm font-bold">
                {latestLog ? new Date(latestLog.recordedAt).toLocaleDateString(undefined, { month: "short", day: "numeric" }) : "—"}
              </p>
            </div>
          </div>
        </div>

        <div className="cut-corner flex flex-col justify-between bg-[#c7f36a] p-5 text-[#24241f] lg:col-span-4">
          <div>
            <StatusPill label="Membership active" tone="ink" />
            <h3 className="display-face mt-4 text-2xl leading-[1.1]">
              North Loop
              <br />
              Unlimited
            </h3>
            <p className="mt-3 text-xs leading-relaxed text-[#4c592e]">
              Your gym membership renews on <b>01 April 2025</b>.
            </p>
          </div>
          <div>
            <div className="mt-6 flex items-center justify-between border-t border-[#24241f]/15 pt-4 text-xs font-bold">
              <span>$69</span>
              <Sparkles className="size-4" />
              <span>APR 01</span>
            </div>
            <Link
              href="/clientdashboard/payments"
              className="mt-4 flex items-center justify-center gap-2 bg-[#24241f] px-3.5 py-2.5 text-sm font-bold text-white transition hover:bg-[#46463e]"
            >
              View membership
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
