"use client";

/* Client progress — one member's synced health data from their mobile app (read-only). Shared by the trainer and nutritionist dashboards. */
import { useEffect, useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSession } from "@/hooks/useSession";
import {
  getClientProgress,
  getClientIntakePhotoUrl,
  type ClientProgress,
  type ProgressDay,
} from "@/lib/trainerProgress";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Dumbbell,
  Flame,
  Footprints,
  HeartPulse,
  Moon,
  RotateCcw,
  Route,
  Scale,
  UtensilsCrossed,
  X,
} from "lucide-react";

type Range = "D" | "W" | "M" | "3M" | "Y";
const RANGES: { key: Range; label: string; days: number }[] = [
  { key: "D", label: "D", days: 1 },
  { key: "W", label: "W", days: 7 },
  { key: "M", label: "M", days: 30 },
  { key: "3M", label: "3M", days: 90 },
  { key: "Y", label: "Y", days: 365 },
];

function stripTime(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}
function iso(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
function addDays(d: Date, n: number) {
  const next = new Date(d);
  next.setDate(next.getDate() + n);
  return next;
}
function fmt(d: Date) {
  return d.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}
function fmtDay(isoDate: string) {
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, {
    weekday: "short",
    day: "numeric",
    month: "short",
  });
}
const num = (v: number | null | undefined) => (v == null ? "—" : Math.round(v).toLocaleString());

type Metric = "steps" | "burned" | "intake" | "heart" | "distance" | "sleep" | "weight" | "workouts";

export default function ClientProgressView({
  backHref,
  backLabel,
  gymId: gymIdProp,
  memberId: memberIdProp,
  loginPath,
}: {
  backHref: string;
  backLabel: string;
  /** The member's gym. Defaults to the viewer's own gym (trainers); standalone nutritionists have none. */
  gymId?: number;
  /** Defaults to the `[id]` route param. */
  memberId?: number;
  loginPath?: string;
}) {
  const params = useParams();
  const memberId = memberIdProp ?? Number(params.id);
  const session = useSession(loginPath);
  const gymId = gymIdProp ?? session?.gymId ?? null;

  const [range, setRange] = useState<Range>("D");
  const [anchor, setAnchor] = useState<Date>(() => stripTime(new Date()));
  const [data, setData] = useState<ClientProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openMetric, setOpenMetric] = useState<Metric | null>(null);
  const [photoUrls, setPhotoUrls] = useState<Record<string, string>>({});

  // Load meal photos only while the intake popup is open; revoke blob URLs on close.
  useEffect(() => {
    if (openMetric !== "intake" || !gymId || !Number.isFinite(memberId) || !data) return;
    let cancelled = false;
    const urls: Record<string, string> = {};
    const load = async () => {
      for (const day of data.days) {
        for (const entry of day.intakeEntries ?? []) {
          if (!entry.hasPhoto) continue;
          const url = await getClientIntakePhotoUrl(gymId, memberId, entry.externalId).catch(() => null);
          if (url) urls[entry.externalId] = url;
        }
      }
      if (!cancelled) setPhotoUrls(urls);
    };
    load();
    return () => {
      cancelled = true;
      Object.values(urls).forEach((u) => URL.revokeObjectURL(u));
      setPhotoUrls({});
    };
  }, [openMetric, data, gymId, memberId]);

  const rangeDays = RANGES.find((r) => r.key === range)?.days ?? 1;
  const from = useMemo(() => addDays(anchor, -(rangeDays - 1)), [anchor, rangeDays]);
  const today = stripTime(new Date());
  const isToday = anchor.getTime() === today.getTime();

  useEffect(() => {
    if (!gymId || !Number.isFinite(memberId)) return;
    setLoading(true);
    getClientProgress(gymId, memberId, iso(from), iso(anchor))
      .then((res) => {
        setData(res);
        setError(null);
      })
      .catch(() => setError("Could not load this client's progress."))
      .finally(() => setLoading(false));
  }, [gymId, memberId, from, anchor]);

  const shift = (dir: -1 | 1) => {
    const next = addDays(anchor, dir * rangeDays);
    setAnchor(next > today ? today : stripTime(next));
  };

  const days = data?.days ?? [];
  const sum = (pick: (d: ProgressDay) => number | null) =>
    days.reduce((acc, d) => acc + (pick(d) ?? 0), 0);
  const avg = (pick: (d: ProgressDay) => number | null) => {
    const values = days.map(pick).filter((v): v is number => v != null);
    return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
  };
  const steps = sum((d) => d.steps);
  const burned = sum((d) => d.activeCalories);
  const intake = sum((d) => d.intakeCalories);
  const distanceKm = sum((d) => (d.distanceMeters ?? 0) / 1000);
  const sleepAvg = avg((d) => d.sleepDurationMinutes);
  const restingAvg = avg((d) => d.restingHeartRateBpm);
  const latestWeight = [...days].reverse().find((d) => d.weightKg != null)?.weightKg ?? null;
  const workoutCount = data?.workouts.length ?? 0;
  const workoutCalories = sum((d) => d.workoutCalories);
  const hasIntake = days.some((d) => d.intakeCalories != null);

  const rangeLabel = (() => {
    if (isToday) {
      return { D: "Today", W: "This week", M: "This month", "3M": "Last 3 months", Y: "This year" }[range];
    }
    return range === "D" ? fmt(anchor) : `${fmt(from)} – ${fmt(anchor)}`;
  })();

  const metricMeta: Record<
    Metric,
    { icon: ReactNode; chip: string; bar: string }
  > = {
    steps: { icon: <Footprints size={20} />, chip: "bg-sky-100 text-sky-700", bar: "#0284c7" },
    burned: { icon: <Flame size={20} />, chip: "bg-orange-100 text-orange-700", bar: "#ea580c" },
    intake: { icon: <UtensilsCrossed size={20} />, chip: "bg-emerald-100 text-emerald-700", bar: "#059669" },
    heart: { icon: <HeartPulse size={20} />, chip: "bg-rose-100 text-rose-700", bar: "#e11d48" },
    distance: { icon: <Route size={20} />, chip: "bg-teal-100 text-teal-700", bar: "#0d9488" },
    sleep: { icon: <Moon size={20} />, chip: "bg-indigo-100 text-indigo-700", bar: "#6366f1" },
    weight: { icon: <Scale size={20} />, chip: "bg-violet-100 text-violet-700", bar: "#7c3aed" },
    workouts: { icon: <Dumbbell size={20} />, chip: "bg-amber-100 text-amber-700", bar: "#d97706" },
  };

  const card = (
    metric: Metric,
    title: string,
    value: string,
    sub: string,
  ) => (
    <button
      key={metric}
      type="button"
      onClick={() => setOpenMetric(metric)}
      className="rounded-2xl border border-[#e8e8e2] bg-white p-4 text-left shadow-sm transition hover:border-[#0b5a8a] hover:shadow"
    >
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold uppercase tracking-wide text-[#6b6b64]">{title}</p>
        <span className={`rounded-full p-2 ${metricMeta[metric].chip}`}>{metricMeta[metric].icon}</span>
      </div>
      <p className="mt-2 text-2xl font-bold text-[#1a1a17]">{value}</p>
      <p className="mt-1 text-xs text-[#6b6b64]">{sub} · tap for details</p>
    </button>
  );

  /* Trend graph: bars per day (weekly buckets for long ranges), no chart
     library — plain SVG so the frontend gains no new dependency. */
  const [chartMetric, setChartMetric] = useState<"steps" | "burned" | "intake" | "heart">("steps");
  const chartValue = (d: ProgressDay) =>
    chartMetric === "steps"
      ? d.steps
      : chartMetric === "burned"
        ? d.activeCalories
        : chartMetric === "intake"
          ? d.intakeCalories
          : d.restingHeartRateBpm;
  const chartBuckets = useMemo(() => {
    if (days.length <= 31) {
      return days.map((d) => ({ label: fmtDay(d.date), value: chartValue(d) }));
    }
    const buckets: { label: string; value: number | null; count: number }[] = [];
    days.forEach((d, i) => {
      const v = chartValue(d);
      if (i % 7 === 0) {
        buckets.push({ label: fmtDay(d.date), value: v, count: v != null ? 1 : 0 });
      } else {
        const b = buckets[buckets.length - 1];
        if (v != null) {
          b.value = chartMetric === "heart" ? (b.value ?? 0) + v : (b.value ?? 0) + v;
          b.count += 1;
        }
      }
    });
    return buckets.map((b) => ({
      label: b.label,
      value: b.value != null && chartMetric === "heart" && b.count > 0 ? b.value / b.count : b.value,
    }));
  }, [days, chartMetric]);
  const chartMax = Math.max(1, ...chartBuckets.map((b) => b.value ?? 0));
  const chartAvg =
    chartBuckets.filter((b) => b.value != null).reduce((a, b) => a + (b.value ?? 0), 0) /
    Math.max(1, chartBuckets.filter((b) => b.value != null).length);

  const dayRow = (d: ProgressDay, text: string) => (
    <div key={d.date} className="flex items-center justify-between border-b border-[#f0f0ec] py-2 text-sm last:border-0">
      <span className="text-[#6b6b64]">{fmtDay(d.date)}</span>
      <span className="font-semibold text-[#1a1a17]">{text}</span>
    </div>
  );

  const popupBody = () => {
    if (!data) return null;
    if (days.length === 0) {
      return <p className="text-sm text-[#6b6b64]">No data for this period.</p>;
    }
    switch (openMetric) {
      case "steps":
        return <div>{days.map((d) => dayRow(d, `${num(d.steps)} steps`))}</div>;
      case "burned":
        return (
          <div>
            {days.map((d) =>
              dayRow(d, `${num(d.activeCalories)} kcal active${d.totalCalories != null ? ` · ${num(d.totalCalories)} kcal total` : ""}`),
            )}
          </div>
        );
      case "intake":
        return hasIntake ? (
          <div>
            <p className="mb-3 text-sm text-[#6b6b64]">
              Total {num(intake)} kcal in this period.
            </p>
            {days.filter((d) => (d.intakeEntries ?? []).length > 0).map((d) => (
              <div key={d.date} className="mb-4">
                <p className="text-xs font-bold uppercase tracking-wide text-[#6b6b64]">{fmtDay(d.date)}</p>
                {(d.intakeEntries ?? []).map((entry) => (
                  <div key={entry.externalId} className="flex items-center gap-3 border-b border-[#f0f0ec] py-2 last:border-0">
                    {entry.hasPhoto && photoUrls[entry.externalId] ? (
                      <img
                        src={photoUrls[entry.externalId]}
                        alt={entry.dishName ?? "Meal photo"}
                        className="h-16 w-16 shrink-0 rounded-lg object-cover"
                      />
                    ) : (
                      <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg bg-[#f1f1f1] text-xs text-[#9a9a93]">
                        No photo
                      </span>
                    )}
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-[#1a1a17]">{entry.dishName ?? "Meal"}</p>
                      <p className="text-xs text-[#6b6b64]">
                        {entry.meal ?? ""} · {num(entry.calories)} kcal
                        {entry.proteinG != null
                          ? ` · P ${Math.round(entry.proteinG)}g · C ${Math.round(entry.carbsG ?? 0)}g · F ${Math.round(entry.fatG ?? 0)}g`
                          : ""}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-[#6b6b64]">
            No intake has synced for this period yet. The client logs food in the app&apos;s Food tab —
            by photo or manually — and logged meals (with their photos, when taken) appear here once
            the app syncs.
          </p>
        );
      case "heart":
        return (
          <div>
            {days.map((d) =>
              dayRow(
                d,
                `Resting ${num(d.restingHeartRateBpm)} · latest ${num(d.latestHeartRateBpm)} · ${num(d.minHeartRateBpm)}–${num(d.maxHeartRateBpm)} bpm`,
              ),
            )}
          </div>
        );
      case "distance":
        return <div>{days.map((d) => dayRow(d, `${((d.distanceMeters ?? 0) / 1000).toFixed(2)} km`))}</div>;
      case "sleep":
        return (
          <div>
            {days.map((d) =>
              dayRow(
                d,
                d.sleepDurationMinutes != null
                  ? `${(d.sleepDurationMinutes / 60).toFixed(1)} h`
                  : "No sleep data",
              ),
            )}
          </div>
        );
      case "weight":
        return <div>{days.map((d) => dayRow(d, d.weightKg != null ? `${d.weightKg.toFixed(1)} kg` : "—"))}</div>;
      case "workouts":
        return data.workouts.length ? (
          <div>
            {data.workouts.map((w, i) => (
              <div key={i} className="border-b border-[#f0f0ec] py-2 text-sm last:border-0">
                <p className="font-semibold text-[#1a1a17]">{w.title}</p>
                <p className="text-[#6b6b64]">
                  {new Date(w.startTime).toLocaleDateString(undefined, { day: "numeric", month: "short" })}
                  {w.durationMinutes != null ? ` · ${w.durationMinutes} min` : ""}
                  {w.caloriesBurned != null ? ` · ${Math.round(w.caloriesBurned)} kcal` : ""}
                  {w.setCount != null ? ` · ${w.setCount} sets` : ""}
                  {w.totalReps != null ? ` · ${w.totalReps} reps` : ""}
                  {w.totalVolumeKg != null ? ` · ${Math.round(w.totalVolumeKg)} kg volume` : ""}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-[#6b6b64]">No workouts logged in this period.</p>
        );
      default:
        return null;
    }
  };

  const popupTitles: Record<Metric, string> = {
    steps: "Steps — day by day",
    burned: "Calories burned — day by day",
    intake: "Calorie intake — day by day",
    heart: "Heart rate — day by day",
    distance: "Distance — day by day",
    sleep: "Sleep — day by day",
    weight: "Weight — day by day",
    workouts: "Workouts in this period",
  };

  const notUsingApp = data && (!data.hasAppAccount || !data.hasAnyRecords);

  return (
    <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6">
      <Link href={backHref} className="inline-flex items-center gap-1 text-sm font-semibold text-[#0b5a8a]">
        <ArrowLeft size={16} /> {backLabel}
      </Link>

      <h1 className="mt-4 text-2xl font-bold text-[#1a1a17]">
        {data ? `${data.firstName} ${data.lastName}` : "Client progress"}
      </h1>
      {data?.email && <p className="text-sm text-[#6b6b64]">{data.email}</p>}

      {/* Date toggle, as in the reference: D / W / M / 3M / Y pills */}
      <div className="mt-6 flex flex-wrap gap-2">
        {RANGES.map((r) => (
          <button
            key={r.key}
            type="button"
            onClick={() => setRange(r.key)}
            className={`rounded-full px-6 py-2.5 text-sm font-semibold transition ${
              range === r.key ? "bg-[#0b5a8a] text-white" : "bg-[#f1f1f1] text-[#1a1a17] hover:bg-[#e6e6e6]"
            }`}
          >
            {r.label}
          </button>
        ))}
      </div>

      {/* Anchor row: label + prev / next / reset */}
      <div className="mt-4 flex items-center justify-between">
        <p className="text-xl font-semibold text-[#1a1a17]">{rangeLabel}</p>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => shift(-1)}
            aria-label="Previous period"
            className="rounded-full bg-[#f1f1f1] p-3 transition hover:bg-[#e6e6e6]"
          >
            <ChevronLeft size={18} />
          </button>
          <button
            type="button"
            onClick={() => shift(1)}
            disabled={isToday}
            aria-label="Next period"
            className="rounded-full bg-[#f1f1f1] p-3 transition hover:bg-[#e6e6e6] disabled:opacity-40"
          >
            <ChevronRight size={18} />
          </button>
          <button
            type="button"
            onClick={() => setAnchor(today)}
            aria-label="Back to today"
            className="rounded-full bg-[#f1f1f1] p-3 transition hover:bg-[#e6e6e6]"
          >
            <RotateCcw size={18} />
          </button>
        </div>
      </div>

      {loading && <p className="mt-8 text-sm text-[#6b6b64]">Loading progress…</p>}
      {error && <p className="mt-8 rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{error}</p>}

      {!loading && notUsingApp && (
        <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          <p className="font-bold">⚠ This client is not using the mobile app.</p>
          <p className="mt-1">
            No health data has synced for them yet. Ask them to sign in to the GymSuit app on their
            phone and allow health access — their steps, heart rate, calories and workouts will
            appear here once the app syncs.
          </p>
        </div>
      )}

      {!loading && data && !notUsingApp && days.length === 0 && (
        <p className="mt-6 rounded-xl bg-[#f7f7f2] px-4 py-3 text-sm text-[#6b6b64]">
          No data for this period — try a longer range or step back with the arrows.
        </p>
      )}

      {!loading && data && !notUsingApp && chartBuckets.length > 0 && (
        <div className="mt-6 rounded-2xl border border-[#e8e8e2] bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-sm font-bold text-[#1a1a17]">Trend · {rangeLabel}</h2>
            <div className="flex flex-wrap gap-1.5">
              {(["steps", "burned", "intake", "heart"] as const).map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setChartMetric(m)}
                  className={`flex items-center gap-1 rounded-full px-3 py-1.5 text-xs font-semibold transition ${
                    chartMetric === m ? "bg-[#0b5a8a] text-white" : "bg-[#f1f1f1] text-[#1a1a17] hover:bg-[#e6e6e6]"
                  }`}
                >
                  <span className="[&>svg]:size-3.5">{metricMeta[m].icon}</span>
                  {m === "steps" ? "Steps" : m === "burned" ? "Burned" : m === "intake" ? "Intake" : "Heart rate"}
                </button>
              ))}
            </div>
          </div>
          {days.length <= 1 ? (
            <p className="mt-4 text-sm text-[#6b6b64]">
              One day only — switch to W, M or 3M above to see a trend over time.
            </p>
          ) : (
            <svg viewBox="0 0 600 190" className="mt-3 w-full" role="img" aria-label="Daily trend chart">
              {chartBuckets.map((b, i) => {
                const w = 600 / chartBuckets.length;
                const h = b.value != null ? (b.value / chartMax) * 150 : 0;
                const avgY = 170 - (chartAvg / chartMax) * 150;
                return (
                  <g key={i}>
                    <rect
                      x={i * w + Math.max(1, w * 0.15)}
                      y={170 - h}
                      width={Math.max(2, w * 0.7)}
                      height={h}
                      rx={3}
                      fill={metricMeta[chartMetric].bar}
                      opacity={b.value == null ? 0.25 : 1}
                    >
                      <title>
                        {b.label}: {b.value != null ? num(b.value) : "no data"}
                        {chartMetric === "heart" ? " bpm" : chartMetric === "steps" ? " steps" : " kcal"}
                      </title>
                    </rect>
                    {i === 0 && (
                      <line x1={0} y1={avgY} x2={600} y2={avgY} stroke="#9ca3af" strokeDasharray="4 4" strokeWidth={1} />
                    )}
                  </g>
                );
              })}
              <line x1={0} y1={170} x2={600} y2={170} stroke="#e5e5e0" strokeWidth={1} />
            </svg>
          )}
          <p className="mt-1 text-xs text-[#6b6b64]">
            {days.length > 31 ? "Weekly totals (weekly average for heart rate)" : "Daily values"} · dashed line is the
            average · hover a bar for its exact value.
          </p>
        </div>
      )}

      {!loading && data && (
        <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {card("steps", "Steps", num(steps), range === "D" ? "Today" : `${num(steps / Math.max(days.length, 1))} / day avg`)}
          {card("burned", "Calories burned", `${num(burned)} kcal`, "Active calories")}
          {card(
            "intake",
            "Calorie intake",
            hasIntake ? `${num(intake)} kcal` : "Not synced yet",
            hasIntake ? "From food log" : "Food log still on phone",
          )}
          {card(
            "heart",
            "Heart rate",
            restingAvg != null ? `${Math.round(restingAvg)} bpm` : "—",
            "Avg resting",
          )}
          {card("distance", "Distance", `${distanceKm.toFixed(1)} km`, "Total")}
          {card(
            "sleep",
            "Sleep",
            sleepAvg != null ? `${(sleepAvg / 60).toFixed(1)} h` : "—",
            "Avg per night",
          )}
          {card("weight", "Weight", latestWeight != null ? `${latestWeight.toFixed(1)} kg` : "—", "Latest recorded")}
          {card("workouts", "Workouts", `${workoutCount}`, `${num(workoutCalories)} kcal in workouts`)}
        </div>
      )}

      {openMetric && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 sm:items-center" onClick={() => setOpenMetric(null)}>
          <div
            className="max-h-[80vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-5 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-bold text-[#1a1a17]">
                <span className={`rounded-full p-2 ${metricMeta[openMetric].chip}`}>{metricMeta[openMetric].icon}</span>
                {popupTitles[openMetric]}
              </h2>
              <button
                type="button"
                onClick={() => setOpenMetric(null)}
                aria-label="Close"
                className="rounded-full bg-[#f1f1f1] p-2 hover:bg-[#e6e6e6]"
              >
                <X size={16} />
              </button>
            </div>
            <p className="mt-1 text-sm text-[#6b6b64]">{rangeLabel}</p>
            <div className="mt-4">{popupBody()}</div>
          </div>
        </div>
      )}
    </div>
  );
}
