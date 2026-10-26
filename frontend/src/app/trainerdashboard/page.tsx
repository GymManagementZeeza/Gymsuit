"use client";

/* Training Ledger page: Trainer command view — the coach's real day, load, and client moments. */

import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Avatar, MetricCard, StatusPill } from "@/components/dashboard/ui";
import { ArrowUpRight, CalendarDays, Dumbbell, Users } from "lucide-react";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listSessions, type TrainingSession } from "@/lib/sessions";
import { listSessionRequests, type SessionRequest } from "@/lib/sessionRequests";

function startOfWeek(date: Date) {
  const d = new Date(date);
  const day = (d.getDay() + 6) % 7; // Monday = 0
  d.setDate(d.getDate() - day);
  d.setHours(0, 0, 0, 0);
  return d;
}

function sameDay(a: Date, b: Date) {
  return a.toDateString() === b.toDateString();
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

export default function TrainerOverview() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const trainerId = session?.trainerId ?? null;

  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!gymId) return;
    Promise.all([listSessions(gymId), listSessionRequests(gymId), listMembers(gymId)])
      .then(([sessionData, requestData, memberData]) => {
        setSessions(sessionData);
        setRequests(requestData);
        setMembers(memberData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your overview."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const mySessions = useMemo(() => sessions.filter((s) => s.trainerId === trainerId), [sessions, trainerId]);
  const myClients = useMemo(() => members.filter((m) => m.trainerId === trainerId), [members, trainerId]);

  const memberIdForSession = useMemo(() => {
    const map = new Map<number, number>();
    for (const r of requests) if (r.sessionId) map.set(r.sessionId, r.memberId);
    return map;
  }, [requests]);

  const memberForSession = (s: TrainingSession) => members.find((m) => m.id === memberIdForSession.get(s.id));

  const sessionName = (s: TrainingSession) => {
    const member = memberForSession(s);
    return member ? `${member.firstName} ${member.lastName}` : s.title;
  };

  const sessionInitials = (s: TrainingSession) => {
    const member = memberForSession(s);
    if (!member) return "?";
    return `${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase();
  };

  const now = new Date();
  const upcomingSessions = useMemo(
    () =>
      mySessions
        .filter((s) => new Date(s.startTime) >= now)
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [mySessions]
  );
  const nextSession = upcomingSessions[0] ?? null;

  const todaySessions = useMemo(
    () =>
      mySessions
        .filter((s) => sameDay(new Date(s.startTime), now))
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [mySessions]
  );

  const weekStart = startOfWeek(now);
  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart);
    d.setDate(d.getDate() + i);
    return d;
  }), [weekStart]);
  const weekLabels = ["M", "T", "W", "T", "F", "S", "S"];
  const weekCounts = useMemo(
    () =>
      weekDays.map(
        (day) => mySessions.filter((s) => sameDay(new Date(s.startTime), day)).length
      ),
    [mySessions, weekDays]
  );
  const weekTotal = weekCounts.reduce((sum, count) => sum + count, 0);
  const weekMax = Math.max(0, ...weekCounts);

  const completedSessions = useMemo(
    () => mySessions.filter((s) => new Date(s.startTime) < now).length,
    [mySessions]
  );

  const upcomingByMember = useMemo(() => {
    const map = new Map<number, TrainingSession>();
    for (const s of upcomingSessions) {
      const memberId = memberIdForSession.get(s.id);
      if (memberId === undefined) continue;
      const existing = map.get(memberId);
      if (!existing || new Date(s.startTime) < new Date(existing.startTime)) map.set(memberId, s);
    }
    return map;
  }, [upcomingSessions, memberIdForSession]);

  const clientsWithSessionThisWeek = useMemo(() => {
    const in7Days = new Date();
    in7Days.setDate(in7Days.getDate() + 7);
    return myClients.filter((m) => {
      const next = upcomingByMember.get(m.id);
      return next && new Date(next.startTime) <= in7Days;
    }).length;
  }, [myClients, upcomingByMember]);

  const pendingRequests = useMemo(
    () => requests.filter((r) => r.trainerId === trainerId && r.status === "PENDING"),
    [requests, trainerId]
  );

  const clientsWithoutUpcoming = useMemo(
    () => myClients.filter((m) => !upcomingByMember.has(m.id)),
    [myClients, upcomingByMember]
  );

  return (
    <div className="page-enter space-y-7">
      {error && <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <section className="grid gap-4 lg:grid-cols-12">
        <div className="cut-corner relative min-h-[300px] overflow-hidden bg-[#24241f] text-white lg:col-span-7">
          <Image
            src="/studio-compass-coach.webp"
            alt="Trainer at work"
            fill
            className="object-cover opacity-70"
            sizes="(min-width: 1024px) 620px, 100vw"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#161614] via-[#161614]/50 to-transparent" />
          <div className="relative flex h-full flex-col justify-end p-6">
            <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Your next session</p>
            <p className="display-face mt-3 text-4xl leading-[0.95]">
              Progress is
              <br />
              personal.
            </p>
            <div className="mt-6 flex items-center gap-3 bg-[#f6f6f0]/95 p-3 text-[#24241f]">
              {loading ? (
                <p className="text-sm text-[#6e6e67]">Loading…</p>
              ) : nextSession ? (
                <>
                  <Avatar initials={sessionInitials(nextSession)} tone="peach" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-bold">{sessionName(nextSession)}</p>
                    <p className="text-xs text-[#6e6e67]">
                      {formatTime(nextSession.startTime)} · {nextSession.title}
                    </p>
                  </div>
                  <Link href="/trainerdashboard/bookings" className="flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
                    Open session <ArrowUpRight className="size-3" />
                  </Link>
                </>
              ) : (
                <>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-bold">No upcoming sessions</p>
                    <p className="text-xs text-[#6e6e67]">New requests will show up on the booking desk.</p>
                  </div>
                  <Link href="/trainerdashboard/bookings" className="flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
                    Open bookings <ArrowUpRight className="size-3" />
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="border border-[#d8d8d1] bg-white p-5 lg:col-span-5">
          <div className="flex items-start justify-between">
            <div>
              <p className="ledger-label">This week</p>
              <h3 className="mt-2 text-lg font-bold tracking-[-0.02em]">Training load</h3>
            </div>
            <span className="mono text-xs font-bold text-[#777770]">
              {loading ? "…" : `${weekTotal} session${weekTotal === 1 ? "" : "s"}`}
            </span>
          </div>
          <div className="mt-6 flex h-28 items-end gap-3">
            {weekCounts.map((count, index) => (
              <div key={index} className="flex flex-1 items-end self-stretch">
                <div
                  className={`w-full transition-all duration-300 ${count > 0 ? "bg-[#c7f36a]" : "bg-[#ececE5]"}`}
                  style={{ height: `${weekMax > 0 ? (count / weekMax) * 100 : 0}%` }}
                />
              </div>
            ))}
          </div>
          <div className="mt-2 flex gap-3 text-[10px] font-bold text-[#8a8a82]">
            {weekLabels.map((day, index) => (
              <span key={index} className="flex-1 text-center">
                {day}
              </span>
            ))}
          </div>
          <p className="mt-5 text-xs text-[#6e6e67]">
            {loading ? (
              "Loading…"
            ) : (
              <>
                <em className="not-italic font-bold text-[#24241f]">{completedSessions}</em> sessions completed so far
              </>
            )}
          </p>
          <Link href="/trainerdashboard/calendar" className="mt-3 inline-flex items-center gap-1 text-xs font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">
            Open calendar →
          </Link>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        <MetricCard label="Sessions completed" value={loading ? "…" : String(completedSessions)} detail={`${weekTotal} sessions this week`} tone="paper" icon={<Dumbbell className="size-5" />} />
        <MetricCard label="Active clients" value={loading ? "…" : String(myClients.length)} detail={`${clientsWithSessionThisWeek} booked this week`} tone="orange" icon={<Users className="size-5" />} />
      </section>

      <section className="grid gap-4 lg:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white lg:col-span-8">
          <div className="flex items-center justify-between border-b border-[#e5e5de] px-5 py-4">
            <div>
              <p className="ledger-label">Today&apos;s flow</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
                {loading ? "Loading…" : todaySessions.length === 0 ? "Nothing scheduled today." : `${todaySessions.length} session${todaySessions.length === 1 ? "" : "s"} today.`}
              </h2>
            </div>
            <Link href="/trainerdashboard/calendar" className="flex items-center gap-1 text-sm font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">
              Full day <CalendarDays className="size-4" />
            </Link>
          </div>
          {loading ? (
            <p className="p-8 text-center text-sm text-[#76766f]">Loading…</p>
          ) : todaySessions.length === 0 ? (
            <p className="p-8 text-center text-sm text-[#76766f]">Nothing here yet.</p>
          ) : (
            <div className="divide-y divide-[#ebebe5]">
              {todaySessions.map((s) => {
                const isNext = nextSession?.id === s.id;
                return (
                  <div className="flex items-center gap-4 px-5 py-4" key={s.id}>
                    <div className="w-[56px] shrink-0">
                      <p className="mono text-sm font-bold">{formatTime(s.startTime)}</p>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-bold tracking-[-0.015em]">{sessionName(s)}</p>
                      <p className="mt-1 text-xs text-[#777770]">{s.title}</p>
                    </div>
                    {isNext ? <StatusPill label="Next up" tone="orange" /> : <ArrowUpRight className="size-4 text-[#a5a59c]" />}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <aside className="cut-corner bg-[#c7f36a] p-5 text-[#24241f] lg:col-span-4">
          <p className="ledger-label !text-[#4f5b32] before:!bg-[#24241f]">Client moment</p>
          {loading ? (
            <p className="display-face mt-4 text-2xl leading-[1.1]">Loading…</p>
          ) : pendingRequests.length > 0 ? (
            <>
              <p className="display-face mt-4 text-2xl leading-[1.1]">
                {pendingRequests.length} request{pendingRequests.length === 1 ? "" : "s"} waiting on you.
              </p>
              <p className="mt-3 text-xs leading-relaxed text-[#4c592e]">
                Approve or reject them from the booking desk.
              </p>
              <div className="mt-6 flex items-center gap-2">
                <Link href="/trainerdashboard/bookings" className="ml-auto flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
                  Open bookings <ArrowUpRight className="size-3" />
                </Link>
              </div>
            </>
          ) : clientsWithoutUpcoming.length > 0 ? (
            <>
              <p className="display-face mt-4 text-2xl leading-[1.1]">
                {clientsWithoutUpcoming.length} of {myClients.length} client{myClients.length === 1 ? "" : "s"} have nothing booked.
              </p>
              <p className="mt-3 text-xs leading-relaxed text-[#4c592e]">
                They could use a little nudge.
              </p>
              <div className="mt-6 flex items-center gap-2">
                {clientsWithoutUpcoming.slice(0, 2).map((client) => (
                  <Avatar
                    key={client.id}
                    initials={`${client.firstName[0] ?? ""}${client.lastName[0] ?? ""}`.toUpperCase()}
                    tone="peach"
                    size="sm"
                  />
                ))}
                <Link href="/trainerdashboard/clients" className="ml-auto flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
                  Open clients <ArrowUpRight className="size-3" />
                </Link>
              </div>
            </>
          ) : (
            <>
              <p className="display-face mt-4 text-2xl leading-[1.1]">Everyone is booked in.</p>
              <p className="mt-3 text-xs leading-relaxed text-[#4c592e]">
                No pending requests, and every client has a session coming up.
              </p>
              <div className="mt-6 flex items-center gap-2">
                <Link href="/trainerdashboard/clients" className="ml-auto flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
                  Open clients <ArrowUpRight className="size-3" />
                </Link>
              </div>
            </>
          )}
        </aside>
      </section>
    </div>
  );
}
