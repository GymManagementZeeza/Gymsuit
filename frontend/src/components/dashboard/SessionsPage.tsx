"use client";

/* Sessions page: session request decisions, studio schedule, and class timetable in the FLEX design language. */
import { useEffect, useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import {
  FlexButton,
  FlexCard,
  FlexEmptyState,
  FlexIconBadge,
  FlexPageHeader,
  FlexPill,
} from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listTrainers, type Trainer } from "@/lib/trainers";
import {
  listSessionRequests,
  approveSessionRequest,
  rejectSessionRequest,
  type SessionRequest,
} from "@/lib/sessionRequests";
import { listSessions, updateSession, deleteSession, type TrainingSession } from "@/lib/sessions";
import { CalendarDays, Check, Clock3, Pencil, Plus, X } from "lucide-react";

function memberName(members: Map<number, Member>, id: number) {
  const m = members.get(id);
  return m ? `${m.firstName} ${m.lastName}` : `Member #${id}`;
}

function trainerName(trainers: Map<number, Trainer>, id: number) {
  const t = trainers.get(id);
  return t ? `${t.firstName} ${t.lastName}` : `Trainer #${id}`;
}

function initialsOf(name: string) {
  return name
    .split(" ")
    .map((p) => p[0])
    .join("")
    .toUpperCase();
}

function toLocalInputParts(iso: string) {
  const d = new Date(iso);
  const date = d.toISOString().slice(0, 10);
  const time = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return { date, time };
}

export default function SessionsPage() {
  const pathname = usePathname();
  const scheduleMode = pathname === "/dashboard/schedule";
  const classesMode = pathname === "/dashboard/classes";
  const eyebrow = scheduleMode
    ? "Training · Schedule"
    : classesMode
      ? "Training · Classes"
      : "Training · Session requests";
  const title = scheduleMode
    ? "Make every minute count."
    : classesMode
      ? "Classes that fill the room."
      : "Requests deserve a quick answer.";
  const description = scheduleMode
    ? "Every trainer's booked sessions — reschedule or cancel any of them, any time."
    : classesMode
      ? "Shape your group timetable, capacity, and the coaches who carry it."
      : "Approve a request and GymFlow turns it into a scheduled session with the member enrolled.";

  return (
    <div className="page-enter space-y-7">
      <FlexPageHeader
        title={title}
        subtitle={`${eyebrow} · ${description}`}
        actions={
          classesMode ? (
            <FlexButton>
              <Plus className="size-4" /> New class
            </FlexButton>
          ) : undefined
        }
      />
      {scheduleMode ? <SchedulePanel /> : classesMode ? <ClassesPanel /> : <RequestsPanel />}
    </div>
  );
}

function RequestsPanel() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [members, setMembers] = useState<Map<number, Member>>(new Map());
  const [trainers, setTrainers] = useState<Map<number, Trainer>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<number | null>(null);

  useEffect(() => {
    if (!gymId) return;
    Promise.all([listSessionRequests(gymId), listMembers(gymId), listTrainers(gymId)])
      .then(([requestData, memberData, trainerData]) => {
        setRequests(requestData.sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        setMembers(new Map(memberData.map((m) => [m.id, m])));
        setTrainers(new Map(trainerData.map((t) => [t.id, t])));
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load session requests."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const pending = requests.filter((r) => r.status === "PENDING");

  const handleApprove = async (id: number) => {
    if (!gymId) return;
    setActingId(id);
    setError(null);
    try {
      const updated = await approveSessionRequest(gymId, id);
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not approve — the trainer may already be booked then.");
    } finally {
      setActingId(null);
    }
  };

  const handleReject = async (id: number) => {
    if (!gymId) return;
    setActingId(id);
    setError(null);
    try {
      const updated = await rejectSessionRequest(gymId, id);
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not reject this request.");
    } finally {
      setActingId(null);
    }
  };

  return (
    <section className="grid gap-4 xl:grid-cols-12">
      <FlexCard className="xl:col-span-8">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Awaiting decision</p>
            <h2 className="mt-2 text-xl font-black tracking-tight text-stone-900">
              {loading ? "Loading…" : `${pending.length} request${pending.length === 1 ? "" : "s"} need a response`}
            </h2>
          </div>
          <FlexPill tone="stone" className="uppercase">{`${requests.length} total`}</FlexPill>
        </div>

        {error && (
          <div className="mt-4 rounded-2xl bg-[#ffe3e3] px-4 py-3 text-sm font-medium text-[#7f1d1d]">{error}</div>
        )}

        {!loading && pending.length === 0 && (
          <div className="mt-4">
            <FlexEmptyState title="No pending requests right now." />
          </div>
        )}

        <div className="mt-2 divide-y divide-stone-100">
          {pending.map((request) => {
            const acting = actingId === request.id;
            return (
              <article className="py-4" key={request.id}>
                <div className="flex flex-col justify-between gap-4 sm:flex-row">
                  <div className="flex gap-3">
                    <FlexIconBadge className="size-10 rounded-full text-[10px] font-bold">
                      {initialsOf(memberName(members, request.memberId))}
                    </FlexIconBadge>
                    <div>
                      <p className="text-sm font-bold text-stone-900">
                        {memberName(members, request.memberId)}{" "}
                        <span className="font-normal text-stone-500">requested a session</span>
                      </p>
                      <p className="mt-1 text-xs text-stone-500">
                        with <span className="font-semibold">{trainerName(trainers, request.trainerId)}</span> ·{" "}
                        {new Date(request.requestedStartTime).toLocaleString(undefined, {
                          weekday: "short",
                          month: "short",
                          day: "numeric",
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                        {request.notes && ` · ${request.notes}`}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <FlexButton
                      variant="ghost"
                      onClick={acting ? undefined : () => handleReject(request.id)}
                      className={`px-3.5 py-2 text-xs ${acting ? "pointer-events-none opacity-40" : ""}`}
                    >
                      <X className="size-3.5" /> Reject
                    </FlexButton>
                    <FlexButton
                      variant="lime"
                      onClick={acting ? undefined : () => handleApprove(request.id)}
                      className={`px-3.5 py-2 text-xs ${acting ? "pointer-events-none opacity-50" : ""}`}
                    >
                      <Check className="size-3.5" /> {acting ? "Approving…" : "Approve"}
                    </FlexButton>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </FlexCard>
      <aside className="rounded-[1.75rem] bg-[#fdf1dc] p-5 shadow-[0_2px_16px_rgba(160,120,30,0.10)] xl:col-span-4">
        <FlexPill tone="amber" className="uppercase">
          Autopilot off
        </FlexPill>
        <p className="mt-4 text-lg font-black tracking-tight text-stone-900">
          Every request keeps a human decision.
        </p>
        <p className="mt-3 text-xs font-medium leading-relaxed text-stone-600">
          When approved, GymFlow checks the trainer is actually free before reserving the time and enrolling the
          member. Nothing books around a trainer without their sign-off.
        </p>
      </aside>
    </section>
  );
}

function SchedulePanel() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [members, setMembers] = useState<Map<number, Member>>(new Map());
  const [trainers, setTrainers] = useState<Map<number, Trainer>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDate, setEditDate] = useState("");
  const [editTime, setEditTime] = useState("");
  const [editTrainerId, setEditTrainerId] = useState<number | null>(null);

  useEffect(() => {
    if (!gymId) return;
    Promise.all([listSessions(gymId), listSessionRequests(gymId), listMembers(gymId), listTrainers(gymId)])
      .then(([sessionData, requestData, memberData, trainerData]) => {
        setSessions(sessionData.sort((a, b) => a.startTime.localeCompare(b.startTime)));
        setRequests(requestData);
        setMembers(new Map(memberData.map((m) => [m.id, m])));
        setTrainers(new Map(trainerData.map((t) => [t.id, t])));
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load the schedule."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const memberForSession = useMemo(() => {
    const map = new Map<number, number>();
    for (const r of requests) {
      if (r.sessionId) map.set(r.sessionId, r.memberId);
    }
    return map;
  }, [requests]);

  const upcoming = sessions.filter((s) => new Date(s.startTime) >= new Date());

  const startEdit = (s: TrainingSession) => {
    const { date, time } = toLocalInputParts(s.startTime);
    setEditingId(s.id);
    setEditDate(date);
    setEditTime(time);
    setEditTrainerId(s.trainerId);
  };

  const saveEdit = async (s: TrainingSession) => {
    if (!gymId || !editDate || !editTime || !editTrainerId) return;
    setActingId(s.id);
    setError(null);
    try {
      const updated = await updateSession(gymId, s.id, {
        trainerId: editTrainerId,
        title: s.title,
        description: s.description ?? undefined,
        startTime: `${editDate}T${editTime}:00`,
        capacity: s.capacity ?? undefined,
      });
      setSessions((prev) => prev.map((x) => (x.id === s.id ? updated : x)).sort((a, b) => a.startTime.localeCompare(b.startTime)));
      setEditingId(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not rearrange — that trainer may already be booked then.");
    } finally {
      setActingId(null);
    }
  };

  const handleDelete = async (s: TrainingSession) => {
    if (!gymId) return;
    setActingId(s.id);
    setError(null);
    try {
      await deleteSession(gymId, s.id);
      setSessions((prev) => prev.filter((x) => x.id !== s.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not cancel this session.");
    } finally {
      setActingId(null);
    }
  };

  return (
    <FlexCard>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Upcoming</p>
          <p className="mt-1 text-sm font-bold text-stone-900">
            {loading ? "Loading…" : `${upcoming.length} session${upcoming.length === 1 ? "" : "s"} scheduled`}
          </p>
        </div>
        <FlexPill tone="stone" className="uppercase">
          Owner & manager view
        </FlexPill>
      </div>

      {error && (
        <div className="mt-4 rounded-2xl bg-[#ffe3e3] px-4 py-3 text-sm font-medium text-[#7f1d1d]">{error}</div>
      )}

      {!loading && upcoming.length === 0 && (
        <div className="mt-4">
          <FlexEmptyState title="No sessions scheduled yet." />
        </div>
      )}

      <div className="mt-2 divide-y divide-stone-100">
        {upcoming.map((s) => {
          const isEditing = editingId === s.id;
          const acting = actingId === s.id;
          const memberId = memberForSession.get(s.id);
          return (
            <div className="flex flex-wrap items-center gap-4 py-4" key={s.id}>
              <div className="w-[140px] shrink-0">
                {isEditing ? (
                  <div className="flex flex-col gap-1.5">
                    <input
                      type="date"
                      value={editDate}
                      onChange={(e) => setEditDate(e.target.value)}
                      className="h-9 rounded-xl border border-stone-200 bg-white px-2 text-xs outline-none focus:border-stone-900"
                    />
                    <input
                      type="time"
                      value={editTime}
                      onChange={(e) => setEditTime(e.target.value)}
                      className="h-9 rounded-xl border border-stone-200 bg-white px-2 text-xs outline-none focus:border-stone-900"
                    />
                  </div>
                ) : (
                  <p className="font-mono text-xs text-stone-500">
                    {new Date(s.startTime).toLocaleString(undefined, {
                      weekday: "short",
                      month: "short",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                )}
              </div>
              <div className="min-w-0 flex-1 rounded-r-2xl border-l-4 border-[#c7f36a] bg-stone-100/70 px-3 py-2">
                <p className="text-sm font-bold text-stone-900">{s.title}</p>
                <p className="mt-1 text-[11px] text-stone-500">
                  {isEditing ? (
                    <select
                      value={editTrainerId ?? ""}
                      onChange={(e) => setEditTrainerId(Number(e.target.value))}
                      className="h-8 rounded-xl border border-stone-200 bg-white px-1.5 text-[11px] outline-none focus:border-stone-900"
                    >
                      {Array.from(trainers.values()).map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.firstName} {t.lastName}
                        </option>
                      ))}
                    </select>
                  ) : (
                    trainerName(trainers, s.trainerId)
                  )}
                  {memberId !== undefined && ` · ${memberName(members, memberId)}`}
                </p>
              </div>
              {isEditing ? (
                <div className="flex items-center gap-2">
                  <FlexButton
                    variant="ghost"
                    onClick={() => setEditingId(null)}
                    className="px-3.5 py-2 text-xs"
                  >
                    Cancel
                  </FlexButton>
                  <FlexButton
                    variant="lime"
                    onClick={() => saveEdit(s)}
                    className={`px-3.5 py-2 text-xs ${acting ? "pointer-events-none opacity-50" : ""}`}
                  >
                    {acting ? "Saving…" : "Save"}
                  </FlexButton>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <FlexButton variant="ghost" onClick={() => startEdit(s)} className="px-3.5 py-2 text-xs">
                    <Pencil className="size-3.5" /> Rearrange
                  </FlexButton>
                  <FlexButton
                    variant="ghost"
                    onClick={acting ? undefined : () => handleDelete(s)}
                    className={`px-3.5 py-2 text-xs text-[#7f1d1d] ${acting ? "pointer-events-none opacity-50" : ""}`}
                  >
                    Cancel
                  </FlexButton>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </FlexCard>
  );
}

function ClassesPanel() {
  const classes = [
    { name: "Strength foundations", schedule: "Mon / Wed / Fri · 08:00", coach: "Alex Morgan", capacity: "18 / 20", status: "2 spots" },
    { name: "Mobility & recovery", schedule: "Tue / Thu · 10:00", coach: "Naomi Brooks", capacity: "10 / 12", status: "2 spots" },
    { name: "Metcon circuit", schedule: "Mon / Wed · 17:30", coach: "Alex Morgan", capacity: "20 / 20", status: "Waitlist" },
  ];
  return (
    <section className="grid gap-4 xl:grid-cols-12">
      <FlexCard className="xl:col-span-8">
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Timetable</p>
        <h2 className="mt-2 text-xl font-black tracking-tight text-stone-900">6 repeating classes</h2>
        <div className="mt-2 divide-y divide-stone-100">
          {classes.map((item) => (
            <div className="flex items-center gap-4 py-4" key={item.name}>
              <FlexIconBadge tone="lime" className="size-11">
                <CalendarDays className="size-5" />
              </FlexIconBadge>
              <div className="min-w-0 flex-1">
                <p className="font-bold text-stone-900">{item.name}</p>
                <p className="mt-1 text-xs text-stone-500">
                  {item.schedule} · {item.coach}
                </p>
              </div>
              <div className="hidden text-right sm:block">
                <p className="text-sm font-bold text-stone-900">{item.capacity}</p>
                <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.1em] text-stone-400">enrolled</p>
              </div>
              <FlexPill tone={item.status === "Waitlist" ? "amber" : "lime"} className="uppercase">
                {item.status}
              </FlexPill>
            </div>
          ))}
        </div>
      </FlexCard>
      <aside className="rounded-[1.75rem] bg-[#eef2ff] p-5 shadow-[0_2px_16px_rgba(20,20,16,0.06)] xl:col-span-4">
        <FlexIconBadge>
          <Clock3 className="size-5" />
        </FlexIconBadge>
        <p className="mt-4 text-lg font-black tracking-tight text-stone-900">Capacity has a rhythm.</p>
        <p className="mt-3 text-xs font-medium leading-relaxed text-stone-600">
          Two classes will hit capacity this week. Move a trainer or open an extra block before waitlists build.
        </p>
        <button className="mt-5 text-xs font-bold text-stone-900 underline decoration-[#c7f36a] decoration-2 underline-offset-4">
          See capacity forecast
        </button>
      </aside>
    </section>
  );
}
