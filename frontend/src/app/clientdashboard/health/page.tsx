"use client";

/* Training Ledger page: Health — weight history, today's check-in, and the next small win. */
import { useEffect, useState } from "react";
import { StatusPill } from "@/components/dashboard/ui";
import { WeightChart } from "@/components/client/WeightChart";
import { useSession } from "@/hooks/useSession";
import { getMember, updateMemberGoal, updateMemberWeight, type Member } from "@/lib/members";
import { listWeightHistory, type WeightLog } from "@/lib/weightLogs";
import { AlertTriangle, Check, Target, TrendingDown } from "lucide-react";

function monthLabel(dateStr: string) {
  return new Date(`${dateStr}T00:00:00`).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

export default function HealthPage() {
  const session = useSession();
  const [history, setHistory] = useState<WeightLog[]>([]);
  const [logging, setLogging] = useState(false);
  const [todayInput, setTodayInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [member, setMember] = useState<Member | null>(null);
  const [editingGoal, setEditingGoal] = useState(false);
  const [goalWeightInput, setGoalWeightInput] = useState("");
  const [goalMonthInput, setGoalMonthInput] = useState("");
  const [savingGoal, setSavingGoal] = useState(false);
  const [goalError, setGoalError] = useState<string | null>(null);
  const [now] = useState(() => Date.now());

  const refreshHistory = () => {
    if (!session?.gymId || !session.memberId) return;
    listWeightHistory(session.gymId, session.memberId)
      .then(setHistory)
      .catch(() => {});
  };

  const refreshMember = () => {
    if (!session?.gymId || !session.memberId) return;
    getMember(session.gymId, session.memberId)
      .then(setMember)
      .catch(() => {});
  };

  useEffect(refreshHistory, [session?.gymId, session?.memberId]);
  useEffect(refreshMember, [session?.gymId, session?.memberId]);

  const startingLog = history[0] ?? null;
  const latestLog = history[history.length - 1] ?? null;
  const totalChange = startingLog && latestLog ? latestLog.weightKg - startingLog.weightKg : null;

  const handleSaveGoal = async () => {
    const value = parseFloat(goalWeightInput);
    if (!session?.gymId || !session.memberId || !value || value <= 0 || !goalMonthInput) return;
    setSavingGoal(true);
    setGoalError(null);
    try {
      const updated = await updateMemberGoal(session.gymId, session.memberId, value, `${goalMonthInput}-01`);
      setMember(updated);
      setEditingGoal(false);
    } catch (err) {
      setGoalError(err instanceof Error ? err.message : "Could not save your goal.");
    } finally {
      setSavingGoal(false);
    }
  };

  const currentWeight = latestLog?.weightKg ?? member?.weightKg ?? null;
  const goalDiff = member?.goalWeightKg != null && currentWeight != null ? member.goalWeightKg - currentWeight : null;

  const daysSinceLastLog = latestLog
    ? (now - new Date(latestLog.recordedAt).getTime()) / (1000 * 60 * 60 * 24)
    : Infinity;
  const needsReminder = daysSinceLastLog >= 2;

  let paceMessage = "Set a goal to see how much you need to lose or gain weekly.";
  let paceWarning = false;
  if (!needsReminder && member?.goalWeightKg != null && member.goalTargetDate && currentWeight != null) {
    const weeksRemaining =
      (new Date(`${member.goalTargetDate}T00:00:00`).getTime() - now) / (1000 * 60 * 60 * 24 * 7);
    const toLose = currentWeight - member.goalWeightKg;
    if (weeksRemaining <= 0) {
      paceMessage = "Your goal date has passed — update your goal.";
    } else if (Math.abs(toLose) < 0.05) {
      paceMessage = "You're right at your goal weight.";
    } else {
      const perWeek = toLose / weeksRemaining;
      paceMessage = `${perWeek > 0 ? "Lose" : "Gain"} ~${Math.abs(perWeek).toFixed(1)} kg/week to hit your goal.`;
      paceWarning = perWeek > 1;
    }
  }

  const handleLogToday = async () => {
    const value = parseFloat(todayInput);
    if (!session?.gymId || !session.memberId || !value || value <= 0) return;
    setSaving(true);
    setError(null);
    try {
      await updateMemberWeight(session.gymId, session.memberId, value);
      setLogging(false);
      setTodayInput("");
      refreshHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save your weight.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-enter space-y-7">
      <section className="grid gap-4 xl:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white p-5 xl:col-span-8">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="ledger-label">Weight history</p>
              <h2 className="mt-2 text-xl font-bold tracking-[-0.03em]">
                Things are moving
                <br />
                <em className="font-normal">in your direction.</em>
              </h2>
            </div>
            {totalChange !== null && (
              <StatusPill label={`${totalChange > 0 ? "+" : ""}${totalChange.toFixed(1)} kg total`} tone="orange" />
            )}
          </div>
          <div className="mt-6">
            <WeightChart history={history} />
          </div>
          <div className="mt-6 grid grid-cols-3 gap-4 border-t border-[#e7e7e1] pt-5">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Starting weight</p>
              <p className="mt-1 text-sm font-bold">{startingLog ? `${startingLog.weightKg} kg` : "—"}</p>
            </div>
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Current weight</p>
              <p className="mt-1 text-sm font-bold">{latestLog ? `${latestLog.weightKg} kg` : "—"}</p>
            </div>
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Goal weight</p>
              <p className="mt-1 text-sm font-bold">{member?.goalWeightKg != null ? `${member.goalWeightKg} kg` : "—"}</p>
            </div>
          </div>
          {logging ? (
            <div className="mt-6 flex items-center gap-2">
              <input
                type="number"
                step="0.1"
                min="0"
                value={todayInput}
                onChange={(e) => setTodayInput(e.target.value)}
                placeholder="kg"
                autoFocus
                className="w-24 border border-[#d8d8d1] bg-white px-2.5 py-2 text-sm font-bold outline-none focus:border-[#24241f]"
              />
              <button
                onClick={handleLogToday}
                disabled={saving}
                className="flex items-center gap-2 bg-[#c7f36a] px-3.5 py-2.5 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-50"
              >
                <Check className="size-4" /> {saving ? "Saving…" : "Save"}
              </button>
              <button
                onClick={() => {
                  setLogging(false);
                  setTodayInput("");
                }}
                className="text-xs font-bold text-[#8a8a82] hover:text-[#24241f]"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              onClick={() => setLogging(true)}
              className="mt-6 flex items-center gap-2 bg-[#c7f36a] px-3.5 py-2.5 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
            >
              + Log today&apos;s weight
            </button>
          )}
          {error && <p className="mt-2 text-xs text-red-700">{error}</p>}
        </div>

        <aside className="cut-corner bg-[#e6eaf6] p-5 xl:col-span-4">
          <p className="ledger-label">Goal</p>
          <div className="mx-auto mt-4 grid size-14 place-items-center rounded-full bg-white text-[#395cbd]">
            <Target className="size-6" />
          </div>

          {editingGoal ? (
            <div className="mt-4 space-y-3">
              <div>
                <label className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#596276]">Target weight (kg)</label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={goalWeightInput}
                  onChange={(e) => setGoalWeightInput(e.target.value)}
                  placeholder="kg"
                  autoFocus
                  className="mt-1 w-full border border-[#bec7da] bg-white px-2.5 py-2 text-sm font-bold outline-none focus:border-[#395cbd]"
                />
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#596276]">By month</label>
                <input
                  type="month"
                  value={goalMonthInput}
                  onChange={(e) => setGoalMonthInput(e.target.value)}
                  className="mt-1 w-full border border-[#bec7da] bg-white px-2.5 py-2 text-sm font-bold outline-none focus:border-[#395cbd]"
                />
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={handleSaveGoal}
                  disabled={savingGoal}
                  className="flex items-center gap-1.5 bg-[#24241f] px-3.5 py-2 text-xs font-bold text-white transition hover:bg-[#46463e] disabled:opacity-50"
                >
                  <Check className="size-3.5" /> {savingGoal ? "Saving…" : "Save goal"}
                </button>
                <button
                  onClick={() => setEditingGoal(false)}
                  className="text-xs font-bold text-[#596276] hover:text-[#24241f]"
                >
                  Cancel
                </button>
              </div>
              {goalError && <p className="text-xs text-red-700">{goalError}</p>}
            </div>
          ) : member?.goalWeightKg != null && member.goalTargetDate ? (
            <>
              <h3 className="mt-4 text-center text-lg font-bold tracking-[-0.02em]">
                {goalDiff !== null && Math.abs(goalDiff) > 0.05
                  ? `${goalDiff < 0 ? "Lose" : "Gain"} ${Math.abs(goalDiff).toFixed(1)} kg`
                  : "Goal reached"}
              </h3>
              <p className="mt-2 text-center text-xs leading-relaxed text-[#596276]">
                Target: <b>{member.goalWeightKg} kg</b> by <b>{monthLabel(member.goalTargetDate)}</b>
              </p>
              <button
                onClick={() => {
                  setGoalWeightInput(String(member.goalWeightKg));
                  setGoalMonthInput(member.goalTargetDate!.slice(0, 7));
                  setEditingGoal(true);
                }}
                className="mx-auto mt-5 flex items-center gap-1 text-xs font-bold underline decoration-[#395cbd] decoration-2 underline-offset-4"
              >
                Update goal →
              </button>
            </>
          ) : (
            <>
              <h3 className="mt-4 text-center text-lg font-bold tracking-[-0.02em]">No goal set yet</h3>
              <p className="mt-2 text-center text-xs leading-relaxed text-[#596276]">
                Pick a target weight and the month you want to hit it by.
              </p>
              <button
                onClick={() => {
                  setGoalWeightInput("");
                  setGoalMonthInput("");
                  setEditingGoal(true);
                }}
                className="mx-auto mt-5 flex items-center gap-1 text-xs font-bold underline decoration-[#395cbd] decoration-2 underline-offset-4"
              >
                Set a goal →
              </button>
            </>
          )}
        </aside>
      </section>

      <section className="cut-corner flex flex-col gap-4 bg-[#24241f] p-6 text-white sm:flex-row sm:items-center">
        <div className="flex items-center gap-4">
          {needsReminder ? (
            <AlertTriangle className="size-8 shrink-0 text-[#f3b45a]" />
          ) : (
            <TrendingDown className="size-8 shrink-0 text-[#c7f36a]" />
          )}
          <div>
            <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">
              {needsReminder ? "Weight check-in" : "Weekly pace"}
            </p>
            <p className="display-face mt-2 text-xl leading-[1.1] sm:text-2xl">
              {needsReminder ? "Forgot to record weight? Record now." : paceMessage}
            </p>
            {paceWarning && (
              <p className="mt-2 flex items-center gap-1.5 text-xs font-bold text-[#f3b45a]">
                <AlertTriangle className="size-3.5" /> That&apos;s more than 1 kg/week — not a healthy pace.
              </p>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
