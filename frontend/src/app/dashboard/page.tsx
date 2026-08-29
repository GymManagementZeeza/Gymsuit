"use client";

/* Training Ledger page: Daily command view with warm paper canvas and lime operational signals. */
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, StatusPill } from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { getCurrentUser } from "@/lib/users";
import { listMembers } from "@/lib/members";
import { listRecentActivity, type ActivityType, type GymActivity } from "@/lib/activity";
import { listGymPayments } from "@/lib/memberPayments";
import { listActiveCheckIns, listCheckInsSince, type CheckIn } from "@/lib/checkins";
import { listTransactions } from "@/lib/transactions";
import {
  Ban,
  Banknote,
  ArrowUpRight,
  Bell,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Dumbbell,
  MoreHorizontal,
  Plus,
  Receipt,
  Tags,
  TrendingDown,
  UserMinus,
  UserPlus,
} from "lucide-react";

const ACTIVITY_STYLES: Record<ActivityType, { icon: typeof UserPlus; tone: string }> = {
  MEMBER_JOINED: { icon: UserPlus, tone: "bg-[#cce0ce] text-[#24241f]" },
  MEMBER_REMOVED: { icon: UserMinus, tone: "bg-[#f2c7b5] text-[#24241f]" },
  PAYMENT_RECEIVED: { icon: Banknote, tone: "bg-[#ead7a1] text-[#24241f]" },
  PLAN_ASSIGNED: { icon: Tags, tone: "bg-[#c4d8fb] text-[#24241f]" },
  PLAN_CHANGED: { icon: Tags, tone: "bg-[#c4d8fb] text-[#24241f]" },
  SUBSCRIPTION_CANCELLED: { icon: Ban, tone: "bg-[#f2c7b5] text-[#24241f]" },
  INCOME_RECORDED: { icon: Receipt, tone: "bg-[#ead7a1] text-[#24241f]" },
  EXPENSE_RECORDED: { icon: TrendingDown, tone: "bg-[#f2c7b5] text-[#24241f]" },
  NOTIFICATION_SENT: { icon: Bell, tone: "bg-[#c4d8fb] text-[#24241f]" },
};

function relativeTime(iso: string) {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes} min${minutes === 1 ? "" : "s"} ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days} day${days === 1 ? "" : "s"} ago`;
  return new Date(iso).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function last12MonthKeys() {
  const now = new Date();
  const keys: string[] = [];
  for (let i = 11; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    keys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
  }
  return keys;
}

function monthAbbrev(monthKey: string) {
  return new Date(`${monthKey}-01T00:00:00`).toLocaleDateString(undefined, { month: "short" }).toUpperCase();
}

function bucketCheckInsByHour(checkIns: CheckIn[]) {
  const counts = new Array(24).fill(0) as number[];
  for (const entry of checkIns) {
    counts[new Date(entry.checkInTime).getHours()] += 1;
  }
  return counts;
}

function hourLabel(hour: number) {
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  return `${displayHour}${hour < 12 ? "AM" : "PM"}`;
}

/** Catmull-Rom → cubic bezier smoothing, for an Apple Health-style curved line. */
function smoothPath(points: { x: number; y: number }[]) {
  if (points.length < 2) return "";
  let d = `M${points[0].x.toFixed(1)},${points[0].y.toFixed(1)}`;
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i - 1] ?? points[i];
    const p1 = points[i];
    const p2 = points[i + 1];
    const p3 = points[i + 2] ?? p2;
    const c1x = p1.x + (p2.x - p0.x) / 6;
    const c1y = p1.y + (p2.y - p0.y) / 6;
    const c2x = p2.x - (p3.x - p1.x) / 6;
    const c2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C${c1x.toFixed(1)},${c1y.toFixed(1)} ${c2x.toFixed(1)},${c2y.toFixed(1)} ${p2.x.toFixed(1)},${p2.y.toFixed(1)}`;
  }
  return d;
}

function greetingForHour(hour: number) {
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

function currentMonthValue() {
  return new Date().toISOString().slice(0, 7);
}

function monthLabel(monthValue: string) {
  return new Date(`${monthValue}-01T00:00:00`).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function shiftMonth(monthValue: string, delta: number) {
  const date = new Date(`${monthValue}-01T00:00:00`);
  date.setMonth(date.getMonth() + delta);
  return date.toISOString().slice(0, 7);
}

const POPOVER_WIDTH = 224;

function MonthPicker({ value, onChange }: { value: string; onChange: (month: string) => void }) {
  const [open, setOpen] = useState(false);
  const [coords, setCoords] = useState<{ top: number; left: number } | null>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const openPicker = () => {
    const rect = buttonRef.current?.getBoundingClientRect();
    if (rect) {
      setCoords({ top: rect.bottom + 8, left: rect.right - POPOVER_WIDTH });
    }
    setOpen(true);
  };

  useEffect(() => {
    if (!open) return;
    const handleClick = (event: MouseEvent) => {
      const target = event.target as Node;
      if (
        buttonRef.current &&
        !buttonRef.current.contains(target) &&
        popoverRef.current &&
        !popoverRef.current.contains(target)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        onClick={() => (open ? setOpen(false) : openPicker())}
        className="hidden items-center gap-2 border border-[#d8d8d1] bg-white px-3.5 py-2.5 text-sm font-semibold transition hover:border-[#24241f] sm:flex"
      >
        <CalendarDays className="size-4" /> {monthLabel(value)}
      </button>
      {open &&
        coords &&
        createPortal(
          <div
            ref={popoverRef}
            style={{ position: "fixed", top: coords.top, left: coords.left, width: POPOVER_WIDTH }}
            className="z-50 border border-[#d8d8d1] bg-white p-3 shadow-lg"
          >
            <div className="flex items-center justify-between">
              <button
                type="button"
                onClick={() => onChange(shiftMonth(value, -1))}
                className="grid size-8 place-items-center transition hover:bg-[#f1f1eb]"
                aria-label="Previous month"
              >
                <ChevronLeft className="size-4" />
              </button>
              <p className="text-sm font-bold">{monthLabel(value)}</p>
              <button
                type="button"
                onClick={() => onChange(shiftMonth(value, 1))}
                className="grid size-8 place-items-center transition hover:bg-[#f1f1eb]"
                aria-label="Next month"
              >
                <ChevronRight className="size-4" />
              </button>
            </div>
            <button
              type="button"
              onClick={() => {
                onChange(currentMonthValue());
                setOpen(false);
              }}
              className="mt-3 w-full border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold transition hover:border-[#24241f]"
            >
              This month
            </button>
          </div>,
          document.body
        )}
    </>
  );
}

export default function DashboardOverview() {
  const router = useRouter();
  const session = useSession();
  const [firstName, setFirstName] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState(currentMonthValue());
  const [newMembersCount, setNewMembersCount] = useState<number | null>(null);
  const [activities, setActivities] = useState<GymActivity[]>([]);
  const [activitiesLoading, setActivitiesLoading] = useState(true);
  const [activitiesError, setActivitiesError] = useState<string | null>(null);
  const [monthlyRevenue, setMonthlyRevenue] = useState<{ month: string; total: number }[] | null>(null);
  const [revenueCurrency, setRevenueCurrency] = useState("INR");
  const [checkedInCount, setCheckedInCount] = useState<number | null>(null);
  const [totalMembersCount, setTotalMembersCount] = useState<number | null>(null);
  const [hourlyCheckIns, setHourlyCheckIns] = useState<number[] | null>(null);
  const [monthlyExpenses, setMonthlyExpenses] = useState<number | null>(null);
  const [expensesCurrency, setExpensesCurrency] = useState("INR");

  useEffect(() => {
    if (!session) return;
    getCurrentUser()
      .then((user) => setFirstName(user.displayName.split(" ")[0]))
      .catch(() => {});
  }, [session]);

  useEffect(() => {
    if (!session?.gymId) return;
    listMembers(session.gymId)
      .then((members) => {
        setNewMembersCount(members.filter((m) => m.joinDate.startsWith(selectedMonth)).length);
        setTotalMembersCount(members.length);
      })
      .catch(() => {});
  }, [session?.gymId, selectedMonth]);

  useEffect(() => {
    if (!session?.gymId) return;
    listActiveCheckIns(session.gymId)
      .then((checkIns) => setCheckedInCount(checkIns.length))
      .catch(() => {});
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    const since = new Date();
    since.setDate(since.getDate() - 7);
    listCheckInsSince(session.gymId, since)
      .then((checkIns) => setHourlyCheckIns(bucketCheckInsByHour(checkIns)))
      .catch(() => {});
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    listRecentActivity(session.gymId)
      .then((entries) => setActivities(entries.slice(0, 6)))
      .catch((err) => setActivitiesError(err instanceof Error ? err.message : "Could not load activity."))
      .finally(() => setActivitiesLoading(false));
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    listGymPayments(session.gymId)
      .then((payments) => {
        const months = last12MonthKeys();
        const totals = new Map(months.map((m) => [m, 0]));
        let currency = "INR";
        for (const payment of payments) {
          if (payment.status !== "SUCCEEDED") continue;
          const dateStr = payment.paidAt ?? payment.createdAt;
          const key = dateStr.slice(0, 7);
          if (totals.has(key)) {
            totals.set(key, (totals.get(key) ?? 0) + Number(payment.amount));
          }
          currency = payment.currency;
        }
        setMonthlyRevenue(months.map((month) => ({ month, total: totals.get(month) ?? 0 })));
        setRevenueCurrency(currency);
      })
      .catch(() => {});
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    listTransactions(session.gymId)
      .then((transactions) => {
        let total = 0;
        let currency = "INR";
        for (const tx of transactions) {
          if (tx.direction !== "EXPENSE" || !tx.occurredOn.startsWith(selectedMonth)) continue;
          total += Number(tx.amount);
          currency = tx.currency;
        }
        setMonthlyExpenses(total);
        setExpensesCurrency(currency);
      })
      .catch(() => {});
  }, [session?.gymId, selectedMonth]);

  const todayLabel = new Date().toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long" });

  const currentMonthRevenue = monthlyRevenue?.[monthlyRevenue.length - 1]?.total ?? 0;
  const previousMonthRevenue = monthlyRevenue?.[monthlyRevenue.length - 2]?.total ?? 0;
  const revenueChangePct =
    previousMonthRevenue > 0
      ? ((currentMonthRevenue - previousMonthRevenue) / previousMonthRevenue) * 100
      : currentMonthRevenue > 0
        ? 100
        : 0;
  const maxMonthlyRevenue = monthlyRevenue ? Math.max(1, ...monthlyRevenue.map((m) => m.total)) : 1;

  const maxHourlyCheckIns = hourlyCheckIns ? Math.max(1, ...hourlyCheckIns) : 1;
  const peakHour = hourlyCheckIns
    ? hourlyCheckIns.reduce((best, count, hour) => (count > hourlyCheckIns[best] ? hour : best), 0)
    : null;
  const peakHourCount = peakHour !== null ? hourlyCheckIns![peakHour] : 0;
  const CHART_WIDTH = 640;
  const CHART_HEIGHT = 140;
  const CHART_PAD_TOP = 16;
  const plotHeight = CHART_HEIGHT - CHART_PAD_TOP;
  const chartPoints = (hourlyCheckIns ?? new Array(24).fill(0)).map((count, hour) => ({
    hour,
    x: (hour / 23) * CHART_WIDTH,
    y: CHART_PAD_TOP + plotHeight - (count / maxHourlyCheckIns) * plotHeight,
    count,
  }));
  const chartLinePath = smoothPath(chartPoints);
  const chartAreaPath = `${chartLinePath} L${CHART_WIDTH},${CHART_HEIGHT} L0,${CHART_HEIGHT} Z`;

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow={todayLabel}
        title={
          <>
            {greetingForHour(new Date().getHours())},
            <br />
            <em className="font-normal">{firstName ? `${firstName}.` : "…"}</em>
          </>
        }
        description="Run today's floor with nothing missed."
        actions={
          <>
            <ActionButton icon={<Plus className="size-4" />} onClick={() => router.push("/dashboard/members")}>
              Add member
            </ActionButton>
            <MonthPicker value={selectedMonth} onChange={setSelectedMonth} />
          </>
        }
      />

      <section className="grid gap-4 lg:grid-cols-12">
        <div className="cut-corner relative min-h-[292px] overflow-hidden bg-[#25251f] p-6 text-white lg:col-span-7">
          <Image
            src="/gymflow-texture.jpg"
            alt=""
            fill
            className="object-cover opacity-[0.14] mix-blend-screen"
          />
          <div className="relative flex h-full flex-col justify-between">
            <div className="flex items-start justify-between">
              <div>
                <p className="ledger-label !text-[#bdbdb2] before:!bg-[#c7f36a]">Membership revenue</p>
                <div className="mt-4 flex items-end gap-3">
                  <p className="display-face text-5xl leading-none">
                    {monthlyRevenue === null ? "…" : `${revenueCurrency} ${currentMonthRevenue.toFixed(0)}`}
                  </p>
                  {monthlyRevenue !== null && (
                    <span className="mb-1 flex items-center gap-1 rounded-full bg-[#c7f36a] px-2 py-1 text-[10px] font-black text-[#24241f]">
                      <ArrowUpRight className={`size-3 ${revenueChangePct < 0 ? "rotate-180" : ""}`} />
                      {Math.abs(revenueChangePct).toFixed(1)}%
                    </span>
                  )}
                </div>
                <p className="mt-2 text-sm text-[#bdbdb2]">
                  {monthlyRevenue === null
                    ? "Loading…"
                    : `vs. ${revenueCurrency} ${previousMonthRevenue.toFixed(0)} this time last month`}
                </p>
              </div>
              <button className="grid size-9 place-items-center border border-white/20 text-white transition hover:bg-white/10" aria-label="More revenue options">
                <MoreHorizontal className="size-5" />
              </button>
            </div>
            <div className="mt-8">
              <div className="flex h-24 items-end gap-2" aria-label="Monthly revenue chart">
                {(monthlyRevenue ?? last12MonthKeys().map((month) => ({ month, total: 0 }))).map((entry, index, arr) => (
                  <div key={entry.month} className="group flex flex-1 items-end self-stretch">
                    <div
                      className={`w-full transition-all duration-300 group-hover:opacity-80 ${index === arr.length - 1 ? "bg-[#c7f36a]" : "bg-white/20"}`}
                      style={{ height: `${(entry.total / maxMonthlyRevenue) * 100}%` }}
                    />
                  </div>
                ))}
              </div>
              <div className="mono mt-3 flex justify-between text-[10px] text-[#a6a69d]">
                {last12MonthKeys().map((month) => (
                  <span key={month}>{monthAbbrev(month)}</span>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-5">
          <div className="grid h-full grid-cols-2 gap-4">
            <MetricCard
              label="New members"
              value={newMembersCount === null ? "…" : String(newMembersCount)}
              detail={`joined in ${monthLabel(selectedMonth)}`}
              tone="lime"
              icon={<UserPlus className="size-5" />}
            />
            <MetricCard label="Session requests" value="08" detail="3 need review" tone="paper" icon={<CalendarDays className="size-5" />} />
            <MetricCard
              label="On the floor now"
              value={checkedInCount === null ? "…" : String(checkedInCount)}
              detail="currently checked in"
              tone="paper"
              icon={<Dumbbell className="size-5" />}
            />
            <MetricCard
              label="Expenses"
              value={monthlyExpenses === null ? "…" : `${expensesCurrency} ${monthlyExpenses.toFixed(0)}`}
              detail={`spent in ${monthLabel(selectedMonth)}`}
              tone="orange"
              icon={<Clock3 className="size-5" />}
            />
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white xl:col-span-7">
          <div className="flex items-center justify-between border-b border-[#e5e5de] px-5 py-4">
            <div>
              <p className="ledger-label">Last 7 days</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Peak hours on the floor</h2>
            </div>
            {hourlyCheckIns !== null && peakHour !== null && peakHourCount > 0 && (
              <span className="flex items-center gap-1.5 bg-[#f1f8de] px-2.5 py-1.5 text-xs font-bold text-[#4c592e]">
                <span className="size-1.5 rounded-full bg-[#8bb92f]" />
                Busiest at {hourLabel(peakHour)}
              </span>
            )}
          </div>
          <div className="px-5 py-6">
            {hourlyCheckIns === null ? (
              <div className="flex h-[140px] items-center justify-center text-sm text-[#8a8a82]">Loading…</div>
            ) : peakHourCount === 0 ? (
              <div className="flex h-[140px] items-center justify-center text-sm text-[#8a8a82]">
                No check-ins in the last 7 days yet.
              </div>
            ) : (
              <svg viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} className="w-full overflow-visible" aria-label="Check-ins by hour of day over the last 7 days">
                <defs>
                  <linearGradient id="peakHoursFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#c7f36a" stopOpacity="0.55" />
                    <stop offset="100%" stopColor="#c7f36a" stopOpacity="0" />
                  </linearGradient>
                </defs>
                <path d={chartAreaPath} fill="url(#peakHoursFill)" />
                <path d={chartLinePath} fill="none" stroke="#24241f" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
                {chartPoints.map((p) =>
                  p.hour === peakHour ? (
                    <g key={p.hour}>
                      <line x1={p.x} y1={p.y} x2={p.x} y2={CHART_HEIGHT} stroke="#24241f" strokeOpacity={0.18} strokeWidth={1} strokeDasharray="3 3" />
                      <circle cx={p.x} cy={p.y} r={7} fill="#c7f36a" fillOpacity={0.35} />
                      <circle cx={p.x} cy={p.y} r={4} fill="#24241f" stroke="white" strokeWidth={1.5} />
                    </g>
                  ) : null
                )}
              </svg>
            )}
            <div className="mono mt-3 flex justify-between text-[10px] text-[#8a8a82]">
              {[0, 4, 8, 12, 16, 20].map((hour) => (
                <span key={hour}>{hourLabel(hour)}</span>
              ))}
            </div>
          </div>
        </div>

        <div className="relative min-h-[368px] overflow-hidden bg-[#24241f] p-5 text-white xl:col-span-5">
          <Image src="/gymflow-floor.jpg" alt="Training floor at North Loop gym" fill className="object-cover opacity-55" />
          <div className="absolute inset-0 bg-gradient-to-t from-[#161614] via-[#161614]/30 to-transparent" />
          <div className="relative flex h-full flex-col justify-between">
            <div className="flex items-start justify-between">
              <div>
                <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Live floor</p>
                <p className="display-face mt-3 text-3xl">
                  It&apos;s a good
                  <br />
                  kind of busy.
                </p>
              </div>
              <StatusPill label="Open" tone="lime" />
            </div>
            <div>
              <div className="flex items-end justify-between">
                <div>
                  <p className="text-4xl font-bold tracking-[-0.06em]">
                    {checkedInCount === null ? "…" : checkedInCount}
                    {totalMembersCount !== null && (
                      <span className="text-lg font-normal text-white/60"> / {totalMembersCount}</span>
                    )}
                  </p>
                  <p className="mt-1 text-xs text-white/70">members currently in the gym</p>
                </div>
                <Link href="/dashboard/live-floor" className="flex items-center gap-1 border border-white/30 px-3 py-2 text-xs font-bold backdrop-blur-sm transition hover:bg-white hover:text-[#24241f]">
                  View live floor <ArrowUpRight className="size-3" />
                </Link>
              </div>
              <div className="mt-5 h-1 bg-white/25">
                <div
                  className="h-full bg-[#c7f36a]"
                  style={{
                    width: `${
                      checkedInCount !== null && totalMembersCount
                        ? Math.min(100, (checkedInCount / totalMembersCount) * 100)
                        : 0
                    }%`,
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white lg:col-span-8">
          <div className="flex items-center justify-between border-b border-[#e5e5de] px-5 py-4">
            <div>
              <p className="ledger-label">Live activity</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Recent activity</h2>
            </div>
          </div>

          {activitiesError && (
            <p className="p-4 text-sm text-red-600">{activitiesError}</p>
          )}

          {!activitiesLoading && !activitiesError && activities.length === 0 && (
            <p className="p-8 text-center text-sm text-[#76766f]">Nothing has happened yet — activity will show up here.</p>
          )}

          <div className="grid divide-y divide-[#ebebe5] sm:grid-cols-2 sm:divide-x sm:divide-y-0">
            {activitiesLoading &&
              [0, 1].map((i) => (
                <div className="flex items-center gap-3 p-4" key={i}>
                  <div className="size-9 shrink-0 animate-pulse rounded-full bg-[#ededE7]" />
                  <div className="h-3 w-2/3 animate-pulse rounded bg-[#ededE7]" />
                </div>
              ))}
            {activities.map((entry) => {
              const style = ACTIVITY_STYLES[entry.type];
              const Icon = style.icon;
              return (
                <div className="flex items-center gap-3 p-4" key={entry.id}>
                  <div className={`grid size-9 shrink-0 place-items-center rounded-full ${style.tone}`}>
                    <Icon className="size-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold">{entry.message}</p>
                    <p className="mono mt-1 text-[10px] text-[#8a8a82]">{relativeTime(entry.createdAt).toUpperCase()}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
        <div className="cut-corner flex flex-col justify-between bg-[#c7f36a] p-5 text-[#24241f] lg:col-span-4">
          <div>
            <p className="ledger-label !text-[#4f5b32] before:!bg-[#24241f]">Need attention</p>
            <p className="display-face mt-4 text-3xl leading-[1.05]">Three session requests are waiting.</p>
          </div>
          <Link href="/dashboard/session-requests" className="mt-6 flex w-fit items-center gap-2 bg-[#24241f] px-3.5 py-2.5 text-sm font-bold text-white transition hover:bg-[#46463e]">
            Review requests <Check className="size-4" />
          </Link>
        </div>
      </section>
    </div>
  );
}
