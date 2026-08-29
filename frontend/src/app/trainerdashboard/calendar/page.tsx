"use client";

/* Training Ledger page: Week calendar — the coach's real sessions and pending requests, laid out by hour. */
import { useEffect, useMemo, useState } from "react";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listSessions, type TrainingSession } from "@/lib/sessions";
import { listSessionRequests, type SessionRequest } from "@/lib/sessionRequests";
import { ChevronLeft, ChevronRight } from "lucide-react";

const HOURS = Array.from({ length: 15 }, (_, i) => i + 7); // 07:00 .. 21:00
const DAY_LABELS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

function startOfWeek(date: Date) {
  const d = new Date(date);
  const day = (d.getDay() + 6) % 7; // Monday = 0
  d.setDate(d.getDate() - day);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, days: number) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function sameDay(a: Date, b: Date) {
  return a.toDateString() === b.toDateString();
}

export default function CalendarPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const trainerId = session?.trainerId ?? null;

  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [members, setMembers] = useState<Map<number, Member>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!gymId) return;
    Promise.all([listSessions(gymId), listSessionRequests(gymId), listMembers(gymId)])
      .then(([sessionData, requestData, memberData]) => {
        setSessions(sessionData);
        setRequests(requestData);
        setMembers(new Map(memberData.map((m) => [m.id, m])));
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your calendar."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart]);

  const memberIdForSession = useMemo(() => {
    const map = new Map<number, number>();
    for (const r of requests) if (r.sessionId) map.set(r.sessionId, r.memberId);
    return map;
  }, [requests]);

  const mySessions = useMemo(() => sessions.filter((s) => s.trainerId === trainerId), [sessions, trainerId]);
  const myPendingRequests = useMemo(
    () => requests.filter((r) => r.trainerId === trainerId && r.status === "PENDING"),
    [requests, trainerId]
  );

  type CalendarEvent = { key: string; label: string; time: string; tone: string; textTone: string };

  const eventsByCell = useMemo(() => {
    const map = new Map<string, CalendarEvent[]>();
    const place = (dayIdx: number, hour: number, event: CalendarEvent) => {
      const key = `${dayIdx}-${hour}`;
      const list = map.get(key) ?? [];
      list.push(event);
      map.set(key, list);
    };

    for (const s of mySessions) {
      const start = new Date(s.startTime);
      const dayIdx = weekDays.findIndex((d) => sameDay(d, start));
      if (dayIdx === -1) continue;
      const member = members.get(memberIdForSession.get(s.id) ?? -1);
      place(dayIdx, start.getHours(), {
        key: `s-${s.id}`,
        label: member ? `${member.firstName} ${member.lastName[0] ?? ""}.` : s.title,
        time: start.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
        tone: "bg-[#d7e4fd]",
        textTone: "text-[#24329e]",
      });
    }

    for (const r of myPendingRequests) {
      const start = new Date(r.requestedStartTime);
      const dayIdx = weekDays.findIndex((d) => sameDay(d, start));
      if (dayIdx === -1) continue;
      const member = members.get(r.memberId);
      place(dayIdx, start.getHours(), {
        key: `r-${r.id}`,
        label: member ? `${member.firstName} ${member.lastName[0] ?? ""}.` : "Requested",
        time: start.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
        tone: "bg-[#f4cfbd]",
        textTone: "text-[#8a4a2e]",
      });
    }

    return map;
  }, [mySessions, myPendingRequests, weekDays, members, memberIdForSession]);

  const weekLabel = `${weekDays[0].toLocaleDateString(undefined, { month: "short", day: "numeric" })} – ${weekDays[6].toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}`;
  const today = new Date();

  return (
    <div className="page-enter space-y-7">
      {error && <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[#e7e7e1] p-5">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setWeekStart((w) => addDays(w, -7))}
              className="grid size-8 place-items-center border border-[#d9d9d2] transition hover:bg-[#f7f7f2]"
              aria-label="Previous week"
            >
              <ChevronLeft className="size-4" />
            </button>
            <b className="text-sm font-bold">{loading ? "Loading…" : weekLabel}</b>
            <button
              onClick={() => setWeekStart((w) => addDays(w, 7))}
              className="grid size-8 place-items-center border border-[#d9d9d2] transition hover:bg-[#f7f7f2]"
              aria-label="Next week"
            >
              <ChevronRight className="size-4" />
            </button>
          </div>
          <button
            onClick={() => setWeekStart(startOfWeek(new Date()))}
            className="border border-[#d8d8d1] px-3 py-2 text-xs font-bold transition hover:border-[#24241f]"
          >
            This week
          </button>
        </div>

        <div className="grid min-w-[820px] grid-cols-[76px_repeat(7,minmax(104px,1fr))] overflow-x-auto border-b border-[#e7e7e1] bg-[#fafaf6] text-center">
          <div className="border-r border-[#e7e7e1] p-3" />
          {weekDays.map((day, index) => (
            <div
              key={day.toISOString()}
              className={`border-r border-[#e7e7e1] p-3 text-xs font-bold last:border-r-0 ${sameDay(day, today) ? "bg-[#eaf6ca]" : ""}`}
            >
              {DAY_LABELS[index]}
              <span className="ml-1 text-[#8b8b84]">{day.getDate()}</span>
            </div>
          ))}
        </div>

        <div className="divide-y divide-[#ededE7]">
          {HOURS.map((hour) => (
            <div className="grid min-w-[820px] grid-cols-[76px_repeat(7,minmax(104px,1fr))]" key={hour}>
              <div className="mono border-r border-[#e7e7e1] p-4 text-[10px] text-[#71716a]">
                {String(hour).padStart(2, "0")}:00
              </div>
              {weekDays.map((day, dayIdx) => {
                const cellEvents = eventsByCell.get(`${dayIdx}-${hour}`) ?? [];
                return (
                  <div className="space-y-1 border-r border-[#f1f1eb] p-2 last:border-r-0" key={day.toISOString()}>
                    {cellEvents.map((event) => (
                      <div className={`p-2 text-left ${event.tone}`} key={event.key}>
                        <p className={`truncate text-xs font-bold ${event.textTone}`}>{event.label}</p>
                        <p className="mono mt-0.5 text-[10px] text-[#5c5c55]">{event.time}</p>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </section>

      <section className="flex flex-wrap items-center gap-4 text-xs font-bold text-[#6e6e67]">
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-[#d7e4fd]" /> Confirmed
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2 rounded-full bg-[#f4cfbd]" /> Pending request
        </span>
      </section>
    </div>
  );
}
