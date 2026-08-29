"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { checkIn, checkOut, listCheckInHistory, type CheckIn } from "@/lib/checkins";

function dateKey(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// ~13 weeks (3 months), starting on a Sunday so weeks line up cleanly, through today.
function last3MonthsDays() {
  const today = new Date();
  const start = new Date(today);
  start.setDate(start.getDate() - 89);
  start.setDate(start.getDate() - start.getDay());

  const days: Date[] = [];
  const cursor = new Date(start);
  while (cursor <= today) {
    days.push(new Date(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return days;
}

function minutesForDay(history: CheckIn[], day: Date) {
  const key = dateKey(day);
  let minutes = 0;
  for (const entry of history) {
    const start = new Date(entry.checkInTime);
    if (dateKey(start) !== key) continue;
    const end = entry.checkOutTime ? new Date(entry.checkOutTime) : new Date();
    minutes += Math.max(0, (end.getTime() - start.getTime()) / 60000);
  }
  return minutes;
}

// GitHub-activity-style intensity: nothing, a light visit, or a solid session.
function intensityClass(minutes: number) {
  if (minutes <= 0) return "bg-[#ececE5]";
  if (minutes < 60) return "bg-[#c7f36a]/50";
  return "bg-[#5f8d1f]";
}

export default function MonthlyCheckInCard({ gymId, memberId }: { gymId: number; memberId: number }) {
  const [history, setHistory] = useState<CheckIn[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirmMode, setConfirmMode] = useState<"in" | "out" | null>(null);
  const [confirmTime, setConfirmTime] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const refresh = () => {
    listCheckInHistory(gymId, memberId)
      .then(setHistory)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your check-ins."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gymId, memberId]);

  const activeCheckIn = history.find((c) => c.checkOutTime === null) ?? null;
  const days = last3MonthsDays();
  const activeDays = days.filter((d) => minutesForDay(history, d) > 0).length;
  const todayKey = dateKey(new Date());
  const weekCount = Math.ceil(days.length / 7);

  const openConfirm = (mode: "in" | "out") => {
    setConfirmTime(new Date().toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" }));
    setConfirmMode(mode);
  };

  const handleConfirm = async () => {
    setSubmitting(true);
    setError(null);
    try {
      if (confirmMode === "in") {
        await checkIn(gymId, memberId);
      } else {
        await checkOut(gymId, memberId);
      }
      setConfirmMode(null);
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not record that.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="border border-[#d8d8d1] bg-white p-5 lg:col-span-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="ledger-label">Last 3 months</p>
          <h3 className="mt-2 text-lg font-bold tracking-[-0.02em]">
            {loading ? "Loading your check-ins…" : `${activeDays} of ${days.length} days at the gym.`}
          </h3>
        </div>
        <button
          type="button"
          onClick={() => openConfirm(activeCheckIn ? "out" : "in")}
          disabled={loading}
          className={`shrink-0 px-3.5 py-2.5 text-xs font-bold transition disabled:opacity-50 ${
            activeCheckIn
              ? "border border-[#d8d8d1] text-[#24241f] hover:border-[#24241f]"
              : "bg-[#c7f36a] text-[#24241f] hover:bg-[#d8ff8a]"
          }`}
        >
          {activeCheckIn ? "Check out" : "Check in"}
        </button>
      </div>

      <div className="mt-6">
        <div
          className="grid w-full gap-1"
          style={{
            gridTemplateColumns: `repeat(${weekCount}, 1fr)`,
            gridTemplateRows: "repeat(7, 1fr)",
            gridAutoFlow: "column",
          }}
        >
          {days.map((day) => {
            const minutes = minutesForDay(history, day);
            const isToday = dateKey(day) === todayKey;
            return (
              <span
                key={day.toISOString()}
                title={`${day.toLocaleDateString(undefined, { month: "short", day: "numeric" })} · ${
                  minutes > 0 ? `${Math.round(minutes)} min` : "No visit"
                }`}
                className={`aspect-square rounded-sm ${intensityClass(minutes)} ${
                  isToday ? "ring-2 ring-[#24241f] ring-offset-1" : ""
                }`}
              />
            );
          })}
        </div>
      </div>

      <div className="mt-4 flex items-center gap-3 text-[10px] text-[#8a8a82]">
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-[#ececE5]" /> None
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-[#c7f36a]/50" /> Under 1h
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-sm bg-[#5f8d1f]" /> 1h+
        </span>
      </div>

      {error && (
        <div className="mt-4">
          <ErrorBanner message={error} />
        </div>
      )}

      <Link href="/clientdashboard/health" className="mt-5 inline-flex items-center gap-1 text-xs font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">
        See your activity →
      </Link>

      {confirmMode && (
        <ConfirmDialog
          title={confirmMode === "in" ? "Are you at the gym?" : "Check out?"}
          description={
            confirmMode === "in"
              ? `It's ${confirmTime}. Confirm you're checking in now.`
              : `It's ${confirmTime}. Confirm you're checking out now.`
          }
          confirmLabel={submitting ? "Saving…" : confirmMode === "in" ? "Yes, I'm here" : "Yes, check out"}
          cancelLabel="No"
          onConfirm={handleConfirm}
          onCancel={() => setConfirmMode(null)}
          disabled={submitting}
        />
      )}
    </div>
  );
}
