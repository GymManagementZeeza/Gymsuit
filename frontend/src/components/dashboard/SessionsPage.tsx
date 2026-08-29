"use client";

/* Training Ledger page: Training schedule workflow emphasizes request decisions and studio timing. */
import { useEffect, useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, StatusPill, TableAction } from "@/components/dashboard/ui";
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
  const title = scheduleMode
    ? "Make every minute count."
    : classesMode
      ? "Classes that fill the room."
      : "Requests deserve a quick answer.";

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow={scheduleMode ? "Training · Schedule" : classesMode ? "Training · Classes" : "Training · Session requests"}
        title={title}
        description={
          scheduleMode
            ? "Every trainer's booked sessions — reschedule or cancel any of them, any time."
            : classesMode
              ? "Shape your group timetable, capacity, and the coaches who carry it."
              : "Approve a request and GymFlow turns it into a scheduled session with the member enrolled."
        }
        actions={classesMode ? <ActionButton icon={<Plus className="size-4" />}>New class</ActionButton> : undefined}
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
      <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
        <div className="flex items-center justify-between border-b border-[#e6e6e0] p-5">
          <div>
            <p className="ledger-label">Awaiting decision</p>
            <h2 className="mt-2 text-xl font-bold">
              {loading ? "Loading…" : `${pending.length} request${pending.length === 1 ? "" : "s"} need a response`}
            </h2>
          </div>
          <StatusPill label={`${requests.length} total`} tone="blue" />
        </div>

        {error && <div className="p-4"><div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div></div>}

        {!loading && pending.length === 0 && (
          <p className="p-8 text-center text-sm text-[#76766f]">No pending requests right now.</p>
        )}

        <div className="divide-y divide-[#e9e9e3]">
          {pending.map((request) => (
            <article className="p-5" key={request.id}>
              <div className="flex flex-col justify-between gap-4 sm:flex-row">
                <div className="flex gap-3">
                  <div className="grid size-10 place-items-center rounded-full bg-[#f3cdbb] text-[10px] font-bold">
                    {initialsOf(memberName(members, request.memberId))}
                  </div>
                  <div>
                    <p className="text-sm font-bold">
                      {memberName(members, request.memberId)} <span className="font-normal text-[#777770]">requested a session</span>
                    </p>
                    <p className="mt-1 text-xs text-[#6e6e67]">
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
                  <TableAction onClick={() => handleReject(request.id)} disabled={actingId === request.id}>
                    <span className="flex items-center gap-1">
                      <X className="size-3.5" /> Reject
                    </span>
                  </TableAction>
                  <button
                    onClick={() => handleApprove(request.id)}
                    disabled={actingId === request.id}
                    className="flex items-center gap-1 bg-[#c7f36a] px-3 py-1.5 text-[11px] font-bold transition hover:bg-[#d8ff8a] disabled:opacity-50"
                  >
                    <Check className="size-3.5" /> {actingId === request.id ? "Approving…" : "Approve"}
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
      <aside className="cut-corner bg-[#ffded2] p-5 xl:col-span-4">
        <p className="ledger-label !text-[#8b4936] before:!bg-[#f07848]">Autopilot off</p>
        <p className="display-face mt-5 text-3xl leading-[1.05]">Every request keeps a human decision.</p>
        <p className="mt-4 text-xs leading-relaxed text-[#875442]">
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
    <section className="border border-[#d8d8d1] bg-white">
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[#e7e7e1] p-5">
        <div>
          <p className="ledger-label">Upcoming</p>
          <p className="text-sm font-bold">
            {loading ? "Loading…" : `${upcoming.length} session${upcoming.length === 1 ? "" : "s"} scheduled`}
          </p>
        </div>
        <StatusPill label="Owner & manager view" tone="ink" />
      </div>

      {error && <div className="p-4"><div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div></div>}

      {!loading && upcoming.length === 0 && (
        <p className="p-8 text-center text-sm text-[#76766f]">No sessions scheduled yet.</p>
      )}

      <div className="divide-y divide-[#ededE7]">
        {upcoming.map((s) => {
          const isEditing = editingId === s.id;
          const memberId = memberForSession.get(s.id);
          return (
            <div className="flex flex-wrap items-center gap-4 p-4" key={s.id}>
              <div className="w-[140px] shrink-0">
                {isEditing ? (
                  <div className="flex flex-col gap-1.5">
                    <input
                      type="date"
                      value={editDate}
                      onChange={(e) => setEditDate(e.target.value)}
                      className="h-8 border border-[#d8d8d1] bg-white px-2 text-xs outline-none focus:border-[#24241f]"
                    />
                    <input
                      type="time"
                      value={editTime}
                      onChange={(e) => setEditTime(e.target.value)}
                      className="h-8 border border-[#d8d8d1] bg-white px-2 text-xs outline-none focus:border-[#24241f]"
                    />
                  </div>
                ) : (
                  <p className="mono text-xs text-[#71716a]">
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
              <div className="min-w-0 flex-1 border-l-4 border-[#c7f36a] bg-[#f4f4ef] px-3 py-2">
                <p className="text-sm font-bold">{s.title}</p>
                <p className="mt-1 text-[11px] text-[#71716a]">
                  {isEditing ? (
                    <select
                      value={editTrainerId ?? ""}
                      onChange={(e) => setEditTrainerId(Number(e.target.value))}
                      className="h-7 border border-[#d8d8d1] bg-white px-1.5 text-[11px] outline-none focus:border-[#24241f]"
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
                  <button
                    onClick={() => setEditingId(null)}
                    className="border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold transition hover:border-[#24241f]"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => saveEdit(s)}
                    disabled={actingId === s.id}
                    className="bg-[#c7f36a] px-3 py-1.5 text-xs font-bold transition hover:bg-[#d8ff8a] disabled:opacity-50"
                  >
                    {actingId === s.id ? "Saving…" : "Save"}
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => startEdit(s)}
                    className="flex items-center gap-1 border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold transition hover:border-[#24241f]"
                  >
                    <Pencil className="size-3.5" /> Rearrange
                  </button>
                  <button
                    onClick={() => handleDelete(s)}
                    disabled={actingId === s.id}
                    className="border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold text-[#8a4636] transition hover:border-red-600 hover:text-red-600 disabled:opacity-50"
                  >
                    Cancel
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
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
      <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
        <div className="border-b border-[#e7e7e1] p-5">
          <p className="ledger-label">Timetable</p>
          <h2 className="mt-2 text-xl font-bold">6 repeating classes</h2>
        </div>
        <div className="divide-y divide-[#e9e9e3]">
          {classes.map((item) => (
            <div className="flex items-center gap-4 p-5" key={item.name}>
              <div className="grid size-11 place-items-center bg-[#24241f] text-[#c7f36a]">
                <CalendarDays className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-bold">{item.name}</p>
                <p className="mt-1 text-xs text-[#71716a]">
                  {item.schedule} · {item.coach}
                </p>
              </div>
              <div className="hidden text-right sm:block">
                <p className="text-sm font-bold">{item.capacity}</p>
                <p className="mt-1 text-[10px] uppercase tracking-[0.1em] text-[#777770]">enrolled</p>
              </div>
              <StatusPill label={item.status} tone={item.status === "Waitlist" ? "orange" : "lime"} />
            </div>
          ))}
        </div>
      </div>
      <aside className="bg-[#dce6ff] p-5 xl:col-span-4">
        <Clock3 className="size-5 text-[#3457a7]" />
        <p className="display-face mt-5 text-3xl leading-[1.05]">Capacity has a rhythm.</p>
        <p className="mt-4 text-xs leading-relaxed text-[#566792]">
          Two classes will hit capacity this week. Move a trainer or open an extra block before waitlists build.
        </p>
        <button className="mt-6 text-xs font-bold underline decoration-[#3457a7] decoration-2 underline-offset-4">See capacity forecast</button>
      </aside>
    </section>
  );
}
