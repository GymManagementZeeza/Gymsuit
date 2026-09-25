"use client";

/* Training Ledger page: Daily command view with warm paper canvas and lime operational signals. */
import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, StatusPill } from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { getCurrentUser } from "@/lib/users";
import { listMembers, type Member } from "@/lib/members";
import { listRecentActivity, type ActivityType, type GymActivity } from "@/lib/activity";
import { listGymPayments } from "@/lib/memberPayments";
import { listActiveCheckIns, type CheckIn } from "@/lib/checkins";
import { listCurrentSubscriptions, type MemberSubscription } from "@/lib/memberSubscriptions";
import { listTransactions } from "@/lib/transactions";
import {
  AlertTriangle,
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
  Mail,
  MessageCircle,
  MoreHorizontal,
  Phone,
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

function formatPaymentCountdown(periodEnd: string | null) {
  if (!periodEnd) {
    return { text: "No active plan", detail: "No upcoming payment", tone: "neutral" as const };
  }
  const end = new Date(`${periodEnd}T00:00:00`);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const diffMs = end.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays < 0) {
    const overdue = Math.abs(diffDays);
    return {
      text: `${overdue} day${overdue === 1 ? "" : "s"} overdue`,
      detail: `Payment was due on ${periodEnd}`,
      tone: "overdue" as const,
    };
  }
  if (diffDays === 0) {
    return {
      text: "Payment due today",
      detail: `Due today (${periodEnd})`,
      tone: "urgent" as const,
    };
  }
  return {
    text: `${diffDays} day${diffDays === 1 ? "" : "s"} left for next payment`,
    detail: `Due on ${periodEnd}`,
    tone: diffDays <= 7 ? ("urgent" as const) : ("normal" as const),
  };
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
  const [members, setMembers] = useState<Member[]>([]);
  const [subscriptionsByMember, setSubscriptionsByMember] = useState<Record<number, MemberSubscription>>({});
  const [membersLoading, setMembersLoading] = useState(true);
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
    setMembersLoading(true);
    Promise.all([
      listMembers(session.gymId),
      listCurrentSubscriptions(session.gymId),
    ])
      .then(([memberList, subList]) => {
        setMembers(memberList);
        setNewMembersCount(memberList.filter((m) => m.joinDate.startsWith(selectedMonth)).length);
        setTotalMembersCount(memberList.length);

        const subMap: Record<number, MemberSubscription> = {};
        for (const sub of subList) {
          subMap[sub.memberId] = sub;
        }
        setSubscriptionsByMember(subMap);
      })
      .catch(() => {})
      .finally(() => setMembersLoading(false));
  }, [session?.gymId, selectedMonth]);

  useEffect(() => {
    if (!session?.gymId) return;
    listActiveCheckIns(session.gymId)
      .then((checkIns) => setCheckedInCount(checkIns.length))
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

  const sortedMembers = useMemo(() => {
    return [...members].sort((a, b) => {
      const subA = subscriptionsByMember[a.id];
      const subB = subscriptionsByMember[b.id];

      const expiryA = subA?.currentPeriodEnd ?? null;
      const expiryB = subB?.currentPeriodEnd ?? null;

      if (expiryA && expiryB) {
        return expiryA.localeCompare(expiryB);
      }
      if (expiryA) return -1;
      if (expiryB) return 1;
      return `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`);
    });
  }, [members, subscriptionsByMember]);

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
        <div className="flex flex-col justify-between border border-[#d8d8d1] bg-white xl:col-span-7">
          <div className="flex items-center justify-between border-b border-[#e5e5de] px-5 py-4">
            <div>
              <p className="ledger-label">Member roster</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Members by expiry date</h2>
            </div>
            <Link href="/dashboard/members" className="text-xs font-bold text-[#24241f] hover:underline">
              View all
            </Link>
          </div>
          <div className="max-h-[380px] divide-y divide-[#ebebe5] overflow-y-auto">
            {membersLoading ? (
              <div className="flex h-[200px] items-center justify-center text-sm text-[#8a8a82]">Loading members…</div>
            ) : sortedMembers.length === 0 ? (
              <div className="flex h-[200px] items-center justify-center text-sm text-[#8a8a82]">No members found.</div>
            ) : (
              sortedMembers.slice(0, 10).map((member, index) => {
                const sub = subscriptionsByMember[member.id];
                const countdown = formatPaymentCountdown(sub?.currentPeriodEnd ?? null);
                const hasNoActivePlan = !sub || sub.status !== "ACTIVE" || countdown.tone === "overdue";

                return (
                  <div key={member.id} className={`flex flex-wrap items-center justify-between gap-3 p-4 transition ${hasNoActivePlan ? "bg-red-50/40 hover:bg-red-50/70" : "hover:bg-[#fafaf6]"}`}>
                    <div className="flex min-w-0 flex-1 items-center gap-3">
                      <div className="relative">
                        <span
                          className={`grid size-9 shrink-0 place-items-center rounded-full text-[10px] font-bold ${
                            index % 2 ? "bg-[#d7e4fd]" : "bg-[#f4cfbd]"
                          }`}
                        >
                          {member.firstName[0]}
                          {member.lastName[0]}
                        </span>
                        {hasNoActivePlan && (
                          <span className="absolute -bottom-1 -right-1 grid size-4 place-items-center rounded-full bg-red-600 text-white shadow" title="No active plan — member may have stopped coming">
                            <AlertTriangle className="size-2.5" />
                          </span>
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <p className="truncate text-sm font-bold">
                            {member.firstName} {member.lastName}
                          </p>
                          {hasNoActivePlan && (
                            <span className="flex items-center gap-1 rounded bg-red-100 px-1.5 py-0.5 text-[9px] font-bold text-red-700">
                              <AlertTriangle className="size-2.5 text-red-600" />
                              No active plan
                            </span>
                          )}
                        </div>
                        <p className="truncate text-xs text-[#74746d]">
                          {sub?.planName ?? "Inactive member"}
                        </p>
                      </div>
                    </div>
                    <div className="min-w-0 text-right">
                      <p
                        className={`text-xs font-bold ${
                          hasNoActivePlan
                            ? "text-[#c84b31]"
                            : "text-[#24241f]"
                        }`}
                      >
                        {countdown.text}
                      </p>
                      <span className="mono text-[10px] text-[#8a8a82] uppercase">
                        {countdown.detail}
                      </span>
                      {hasNoActivePlan && (
                        <div className="mt-1.5 flex flex-wrap items-center justify-end gap-1.5">
                          {member.phone && (
                            <a
                              href={`tel:${member.phone}`}
                              className="flex items-center gap-1 rounded border border-red-300 bg-white px-2 py-0.5 text-[10px] font-bold text-red-700 shadow-sm transition hover:bg-red-50"
                              title={`Call ${member.firstName} (${member.phone})`}
                            >
                              <Phone className="size-3 text-red-600" /> Call
                            </a>
                          )}
                          {member.phone && (
                            <a
                              href={`https://wa.me/${member.phone.replace(/\D/g, "")}?text=${encodeURIComponent(`Hi ${member.firstName}, we missed seeing you at the gym! Let us know if you need help renewing your membership.`)}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex items-center gap-1 rounded border border-emerald-300 bg-white px-2 py-0.5 text-[10px] font-bold text-emerald-700 shadow-sm transition hover:bg-emerald-50"
                              title={`WhatsApp ${member.firstName} (${member.phone})`}
                            >
                              <MessageCircle className="size-3 text-emerald-600" /> WhatsApp
                            </a>
                          )}
                          {member.email && (
                            <a
                              href={`mailto:${member.email}?subject=We%20miss%20you%20at%20the%20gym!`}
                              className="flex items-center gap-1 rounded border border-red-300 bg-white px-2 py-0.5 text-[10px] font-bold text-red-700 shadow-sm transition hover:bg-red-50"
                              title={`Email ${member.firstName} (${member.email})`}
                            >
                              <Mail className="size-3 text-red-600" /> Email
                            </a>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            )}
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
              <div className="flex items-end justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-4xl font-bold tracking-[-0.06em]">
                    {checkedInCount === null ? "…" : checkedInCount}
                    {totalMembersCount !== null && (
                      <span className="text-lg font-normal text-white/60"> / {totalMembersCount}</span>
                    )}
                  </p>
                  <p className="mt-1 text-xs text-white/70">members currently in the gym</p>
                </div>
                <Link href="/dashboard/live-floor" className="flex shrink-0 items-center gap-1 border border-white/30 px-3 py-2 text-xs font-bold backdrop-blur-sm transition hover:bg-white hover:text-[#24241f]">
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
