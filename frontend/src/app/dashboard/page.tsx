"use client";

/* Dashboard overview: light-mode command cards — admissions, collection, check-in rush, renewals. */
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import { intlLocale, type Locale } from "@/lib/i18n/dictionaries";
import { useSession } from "@/hooks/useSession";
import { getCurrentUser } from "@/lib/users";
import { getGym } from "@/lib/gyms";
import { listMembers, type Member } from "@/lib/members";
import { listCheckInsSince } from "@/lib/checkins";
import { listGymPayments } from "@/lib/memberPayments";
import { listCurrentSubscriptions, type MemberSubscription } from "@/lib/memberSubscriptions";
import { BUILD_INFO } from "@/generated/build-info";
import {
  AlertTriangle,
  IndianRupee,
  MessageCircle,
  UsersRound,
  Zap,
} from "lucide-react";

function last7MonthKeys() {
  const now = new Date();
  const keys: string[] = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    keys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
  }
  return keys;
}

function monthAbbrev(monthKey: string, locale: Locale) {
  return new Date(`${monthKey}-01T00:00:00`).toLocaleDateString(intlLocale(locale), { month: "short" });
}

function formatINRCompact(amount: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(amount);
}

function daysUntil(dateStr: string | null): number | null {
  if (!dateStr) return null;
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return Math.ceil((new Date(`${dateStr}T00:00:00`).getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

type StatVariant = "white" | "lime" | "red";

function StatCard({
  label,
  value,
  detail,
  icon,
  href,
  variant,
}: {
  label: string;
  value: string;
  detail: string;
  icon: React.ReactNode;
  href: string;
  variant: StatVariant;
}) {
  const styles: Record<StatVariant, string> = {
    white: "bg-white text-stone-900 shadow-[0_2px_16px_rgba(20,20,16,0.06)]",
    lime: "bg-gradient-to-br from-[#d3f34f] to-[#a4d614] text-[#1c2b06] shadow-[0_2px_16px_rgba(120,160,20,0.25)]",
    red: "bg-[#ffe3e3] text-[#7f1d1d] shadow-[0_2px_16px_rgba(160,30,30,0.10)]",
  };
  return (
    <Link
      href={href}
      aria-label={`${label} — ${value}`}
      className={`flex min-h-[148px] flex-col justify-between rounded-[1.75rem] p-5 transition hover:-translate-y-0.5 ${styles[variant]}`}
    >
      <div className="flex items-start justify-between gap-2">
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] opacity-70">{label}</p>
        <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-black/[0.07]">{icon}</span>
      </div>
      <div>
        <p className="text-4xl font-black tracking-tight">{value}</p>
        <p className="mt-1 text-xs font-semibold opacity-70">{detail}</p>
      </div>
    </Link>
  );
}

export default function DashboardOverview() {
  const session = useSession();
  const { t, locale, setLocale } = useLanguage();
  const [gymName, setGymName] = useState<string | null>(null);
  const [gymCity, setGymCity] = useState<string | null>(null);
  const [initials, setInitials] = useState("…");
  const [members, setMembers] = useState<Member[]>([]);
  const [subscriptionsByMember, setSubscriptionsByMember] = useState<Record<number, MemberSubscription>>({});
  const [dataLoading, setDataLoading] = useState(true);
  const [monthlyRevenue, setMonthlyRevenue] = useState<{ month: string; total: number }[] | null>(null);
  const [monthlyCollection, setMonthlyCollection] = useState<number | null>(null);
  const [checkinHours, setCheckinHours] = useState<number[] | null>(null);

  useEffect(() => {
    if (!session) return;
    getCurrentUser()
      .then((user) => {
        const parts = user.displayName.split(/\s+/).filter(Boolean).slice(0, 2);
        setInitials(parts.map((p) => p[0]?.toUpperCase()).join("") || "…");
      })
      .catch(() => {});
  }, [session]);

  useEffect(() => {
    if (!session?.gymId) return;
    getGym(session.gymId)
      .then((gym) => {
        setGymName(gym.name);
        setGymCity(gym.city);
      })
      .catch(() => {});
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    Promise.all([listMembers(session.gymId), listCurrentSubscriptions(session.gymId)])
      .then(([memberList, subList]) => {
        setMembers(memberList);
        const subMap: Record<number, MemberSubscription> = {};
        for (const sub of subList) {
          subMap[sub.memberId] = sub;
        }
        setSubscriptionsByMember(subMap);
      })
      .catch(() => {})
      .finally(() => setDataLoading(false));
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    listGymPayments(session.gymId)
      .then((payments) => {
        const months = last7MonthKeys();
        const totals = new Map(months.map((m) => [m, 0]));
        const thisMonth = new Date().toISOString().slice(0, 7);
        let collection = 0;
        for (const payment of payments) {
          if (payment.status !== "SUCCEEDED") continue;
          const dateStr = payment.paidAt ?? payment.createdAt;
          const key = dateStr.slice(0, 7);
          if (totals.has(key)) {
            totals.set(key, (totals.get(key) ?? 0) + Number(payment.amount));
          }
          if (key === thisMonth) {
            collection += Number(payment.amount);
          }
        }
        setMonthlyRevenue(months.map((month) => ({ month, total: totals.get(month) ?? 0 })));
        setMonthlyCollection(collection);
      })
      .catch(() => {});
  }, [session?.gymId]);

  useEffect(() => {
    if (!session?.gymId) return;
    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);
    listCheckInsSince(session.gymId, startOfToday)
      .then((checkins) => {
        const buckets = new Array(15).fill(0) as number[];
        for (const checkin of checkins) {
          const hour = new Date(checkin.checkInTime).getHours();
          if (hour >= 6 && hour <= 20) {
            buckets[hour - 6] += 1;
          }
        }
        setCheckinHours(buckets);
      })
      .catch(() => {});
  }, [session?.gymId]);

  const activeMembers = useMemo(
    () => members.filter((m) => subscriptionsByMember[m.id]?.status === "ACTIVE").length,
    [members, subscriptionsByMember]
  );

  const pendingFeesMembers = useMemo(
    () =>
      members.filter((m) => {
        const sub = subscriptionsByMember[m.id];
        if (!sub || sub.status !== "ACTIVE") return true;
        const remaining = daysUntil(sub.currentPeriodEnd);
        return remaining !== null && remaining < 0;
      }),
    [members, subscriptionsByMember]
  );

  const expiringSoon = useMemo(
    () =>
      members.filter((m) => {
        const remaining = daysUntil(subscriptionsByMember[m.id]?.currentPeriodEnd ?? null);
        return remaining !== null && remaining >= 0 && remaining <= 7;
      }),
    [members, subscriptionsByMember]
  );

  const totalCheckinsToday = checkinHours ? checkinHours.reduce((a, b) => a + b, 0) : 0;
  const maxCheckinBucket = checkinHours ? Math.max(1, ...checkinHours) : 1;
  const maxRevenue = monthlyRevenue ? Math.max(1, ...monthlyRevenue.map((m) => m.total)) : 1;
  const hours = Array.from({ length: 15 }, (_, i) => i + 6);

  return (
    <div className="page-enter mx-auto w-full max-w-xl space-y-4 pb-4">
      {/* Compact header like the reference: gym name, language pill, avatar */}
      <div className="flex items-center justify-between pt-1">
        <div className="min-w-0">
          <h1 className="truncate text-xl font-black tracking-tight text-stone-900">
            {gymName ?? "…"}
          </h1>
          {gymCity && (
            <p className="mono mt-0.5 text-[10px] uppercase tracking-[0.18em] text-stone-500">
              {gymCity}
            </p>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            onClick={() => setLocale(locale === "en" ? "ml" : "en")}
            className="rounded-full bg-[#c7f36a] px-4 py-2 text-sm font-bold text-stone-900 transition hover:bg-[#d8ff8a]"
          >
            {locale === "en" ? "മലയാളം" : "English"}
          </button>
          <span className="grid size-10 place-items-center rounded-full bg-[#4ade80] text-xs font-black text-stone-900">
            {initials}
          </span>
        </div>
      </div>

      {/* Stat cards */}
      <section className="grid grid-cols-2 gap-4" aria-label={t.dashboard.totalAdmission}>
        <StatCard
          label={t.dashboard.totalAdmission}
          value={dataLoading ? "…" : String(members.length)}
          detail={t.dashboard.activeCount(activeMembers)}
          icon={<UsersRound className="size-5" />}
          href="/dashboard/members"
          variant="white"
        />
        <StatCard
          label={t.dashboard.activeMembers}
          value={dataLoading ? "…" : String(activeMembers)}
          detail={t.dashboard.fullPowerMembers}
          icon={<Zap className="size-5" />}
          href="/dashboard/members"
          variant="lime"
        />
        <StatCard
          label={t.dashboard.monthlyCollection}
          value={monthlyCollection === null ? "…" : formatINRCompact(monthlyCollection)}
          detail={t.dashboard.thisMonth}
          icon={<IndianRupee className="size-5" />}
          href="/dashboard/payments"
          variant="white"
        />
        <StatCard
          label={t.dashboard.pendingFees}
          value={dataLoading ? "…" : String(pendingFeesMembers.length)}
          detail={t.dashboard.feesBakki}
          icon={<AlertTriangle className="size-5" />}
          href="/dashboard/members"
          variant="red"
        />
      </section>

      {/* Today's rush — check-in flow */}
      <section className="rounded-[1.75rem] bg-white p-5 shadow-[0_2px_16px_rgba(20,20,16,0.06)]">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-black tracking-tight text-stone-900">{t.dashboard.todaysRush}</h2>
            <p className="mono mt-1 text-[11px] text-stone-500">
              {t.dashboard.rushHours} • {t.dashboard.totalToday(totalCheckinsToday)}
            </p>
          </div>
          <span className="shrink-0 rounded-full border border-green-500 px-3 py-1 text-[10px] font-black tracking-[0.08em] text-green-600">
            {t.dashboard.live}
          </span>
        </div>
        <div className="mt-5 flex h-36 items-end gap-1" aria-label={t.dashboard.checkinChartLabel}>
          {(checkinHours ?? new Array(15).fill(0)).map((count, i) => (
            <div key={hours[i]} className="flex flex-1 items-end self-stretch">
              <div
                className="w-full rounded-full bg-gradient-to-t from-[#4ade80] to-[#a3e635]"
                style={{ height: `${Math.max(count > 0 ? 8 : 2, (count / maxCheckinBucket) * 100)}%` }}
              />
            </div>
          ))}
        </div>
        <div className="mono mt-2 flex text-[9px] text-stone-400">
          {hours.map((h) => (
            <span key={h} className="flex-1 text-center">
              {h}h
            </span>
          ))}
        </div>
      </section>

      {/* Expiry warning */}
      <section className="rounded-[1.75rem] bg-[#fdf1dc] p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <span className="grid size-10 shrink-0 place-items-center rounded-2xl bg-[#f5a623] text-white">
              <AlertTriangle className="size-5" />
            </span>
            <h2 className="text-lg font-black leading-snug tracking-tight text-stone-900">
              {t.dashboard.expiryWarning}
            </h2>
          </div>
          <span className="shrink-0 rounded-full border border-amber-700/30 px-3 py-1 text-center text-[10px] font-black text-amber-800">
            {t.dashboard.expiringMembers(expiringSoon.length)}
          </span>
        </div>
        <p className="mt-3 text-sm font-medium text-amber-900/80">{t.dashboard.expiryDetail}</p>
        <div className="mt-4 rounded-2xl bg-white/70 p-4">
          {expiringSoon.length === 0 ? (
            <p className="text-sm font-semibold text-stone-600">{t.dashboard.noExpiryWeek}</p>
          ) : (
            <ul className="divide-y divide-amber-900/10">
              {expiringSoon.slice(0, 5).map((member) => (
                <li key={member.id} className="flex items-center justify-between gap-3 py-2.5 first:pt-0 last:pb-0">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-bold text-stone-900">
                      {member.firstName} {member.lastName}
                    </p>
                    <p className="text-[11px] text-stone-500">
                      {t.dashboard.countdownDaysLeft(daysUntil(subscriptionsByMember[member.id]?.currentPeriodEnd ?? null) ?? 0)}
                    </p>
                  </div>
                  {member.phone && (
                    <a
                      href={`https://wa.me/${member.phone.replace(/\D/g, "")}?text=${encodeURIComponent(t.dashboard.whatsappMessage(member.firstName))}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex shrink-0 items-center gap-1 rounded-full bg-[#25d366] px-3 py-1.5 text-[11px] font-bold text-white"
                    >
                      <MessageCircle className="size-3.5" /> {t.dashboard.whatsapp}
                    </a>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      {/* Monthly revenue */}
      <section className="rounded-[1.75rem] bg-white p-5 shadow-[0_2px_16px_rgba(20,20,16,0.06)]">
        <div className="flex items-baseline justify-between">
          <h2 className="text-lg font-black tracking-tight text-stone-900">{t.dashboard.monthlyRevenueTitle}</h2>
          <p className="mono text-[11px] text-stone-500">{t.dashboard.last7Months}</p>
        </div>
        <div className="mt-5 flex h-36 items-end gap-2" aria-label={t.dashboard.revenueChartLabel}>
          {(monthlyRevenue ?? last7MonthKeys().map((month) => ({ month, total: 0 }))).map((entry) => (
            <div key={entry.month} className="flex flex-1 items-end self-stretch">
              <div
                className="w-full rounded-t-xl bg-stone-300"
                style={{ height: `${Math.max(entry.total > 0 ? 8 : 2, (entry.total / maxRevenue) * 100)}%` }}
              />
            </div>
          ))}
        </div>
        <div className="mono mt-2 flex text-[10px] text-stone-400">
          {last7MonthKeys().map((month) => (
            <span key={month} className="flex-1 text-center">
              {monthAbbrev(month, locale)}
            </span>
          ))}
        </div>
      </section>

      <footer className="pt-2 text-center">
        <p className="text-[11px] text-stone-400">
          GYMSUIT v{BUILD_INFO.version} • {gymName ?? ""}
        </p>
        <p className="mt-1 text-[11px] text-stone-400">Made for Kerala gyms</p>
      </footer>
    </div>
  );
}
