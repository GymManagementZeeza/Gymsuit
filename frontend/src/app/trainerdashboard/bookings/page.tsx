"use client";

/* Training Ledger page: Booking desk — real pending requests and the trainer's own upcoming sessions. */
import { useEffect, useMemo, useState } from "react";
import { Avatar, StatusPill } from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import {
  listSessionRequests,
  approveSessionRequest,
  rejectSessionRequest,
  type SessionRequest,
} from "@/lib/sessionRequests";
import { listSessions, updateSession, deleteSession, type TrainingSession } from "@/lib/sessions";
import { Check, X, Pencil } from "lucide-react";

function initialsFor(member: Member | undefined) {
  if (!member) return "?";
  return `${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase();
}

function nameFor(member: Member | undefined) {
  return member ? `${member.firstName} ${member.lastName}` : "A member";
}

function toLocalInputParts(iso: string) {
  const d = new Date(iso);
  const date = d.toISOString().slice(0, 10);
  const time = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return { date, time };
}

const tabs = ["Upcoming", "Requests", "Completed"] as const;

export default function BookingsPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const trainerId = session?.trainerId ?? null;

  const [filter, setFilter] = useState<(typeof tabs)[number]>("Requests");
  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [members, setMembers] = useState<Map<number, Member>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<number | null>(null);
  const [editingSessionId, setEditingSessionId] = useState<number | null>(null);
  const [editDate, setEditDate] = useState("");
  const [editTime, setEditTime] = useState("");

  const load = () => {
    if (!gymId) return;
    Promise.all([listSessionRequests(gymId), listSessions(gymId), listMembers(gymId)])
      .then(([requestData, sessionData, memberData]) => {
        setRequests(requestData.sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        setSessions(sessionData);
        setMembers(new Map(memberData.map((m) => [m.id, m])));
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load bookings."))
      .finally(() => setLoading(false));
  };

  useEffect(load, [gymId]);

  const pendingRequests = useMemo(
    () => requests.filter((r) => r.status === "PENDING" && r.trainerId === trainerId),
    [requests, trainerId]
  );
  const myUpcomingSessions = useMemo(
    () =>
      sessions
        .filter((s) => s.trainerId === trainerId && new Date(s.startTime) >= new Date())
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [sessions, trainerId]
  );
  const myPastSessions = useMemo(
    () =>
      sessions
        .filter((s) => s.trainerId === trainerId && new Date(s.startTime) < new Date())
        .sort((a, b) => b.startTime.localeCompare(a.startTime)),
    [sessions, trainerId]
  );

  const requestMemberId = (sessionId: number) => requests.find((r) => r.sessionId === sessionId)?.memberId;

  const handleApprove = async (requestId: number) => {
    if (!gymId) return;
    setActingId(requestId);
    setError(null);
    try {
      const updated = await approveSessionRequest(gymId, requestId);
      setRequests((prev) => prev.map((r) => (r.id === requestId ? updated : r)));
      if (updated.sessionId) {
        const [refreshedSessions] = await Promise.all([listSessions(gymId)]);
        setSessions(refreshedSessions);
      }
      setFilter("Upcoming");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not approve this request — the time may already be taken.");
    } finally {
      setActingId(null);
    }
  };

  const handleReject = async (requestId: number) => {
    if (!gymId) return;
    setActingId(requestId);
    setError(null);
    try {
      const updated = await rejectSessionRequest(gymId, requestId);
      setRequests((prev) => prev.map((r) => (r.id === requestId ? updated : r)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not reject this request.");
    } finally {
      setActingId(null);
    }
  };

  const startEdit = (s: TrainingSession) => {
    const { date, time } = toLocalInputParts(s.startTime);
    setEditingSessionId(s.id);
    setEditDate(date);
    setEditTime(time);
  };

  const saveEdit = async (s: TrainingSession) => {
    if (!gymId || !editDate || !editTime) return;
    setActingId(s.id);
    setError(null);
    try {
      const updated = await updateSession(gymId, s.id, {
        trainerId: s.trainerId,
        title: s.title,
        description: s.description ?? undefined,
        startTime: `${editDate}T${editTime}:00`,
        capacity: s.capacity ?? undefined,
      });
      setSessions((prev) => prev.map((x) => (x.id === s.id ? updated : x)));
      setEditingSessionId(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not reschedule — that time may already be taken.");
    } finally {
      setActingId(null);
    }
  };

  const handleCancelSession = async (s: TrainingSession) => {
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

  const listForTab = filter === "Upcoming" ? myUpcomingSessions : filter === "Completed" ? myPastSessions : [];

  return (
    <div className="page-enter space-y-7">
      <div className="cut-corner flex flex-col justify-between gap-6 bg-[#24241f] p-6 text-white sm:flex-row sm:items-center">
        <div>
          <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Booking desk</p>
          <p className="display-face mt-3 text-3xl leading-[1.05]">
            {loading ? "…" : myUpcomingSessions.length} session{myUpcomingSessions.length === 1 ? "" : "s"}
            <br />
            coming up.
          </p>
          <p className="mt-3 text-sm text-white/65">
            {loading ? "Loading…" : `${pendingRequests.length} request${pendingRequests.length === 1 ? "" : "s"} waiting on you.`}
          </p>
        </div>
      </div>

      {error && <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <div className="flex gap-2 border-b border-[#d8d8d1]">
        {tabs.map((tab) => (
          <button
            key={tab}
            onClick={() => setFilter(tab)}
            className={`relative -mb-px flex items-center gap-1.5 border-b-2 px-3 py-2.5 text-xs font-bold ${
              filter === tab ? "border-[#c7f36a] text-[#24241f]" : "border-transparent text-[#8a8a82] hover:text-[#24241f]"
            }`}
          >
            {tab}
            {tab === "Requests" && pendingRequests.length > 0 && (
              <span className="grid size-4 place-items-center bg-[#c7f36a] text-[9px] font-bold text-[#24241f]">
                {pendingRequests.length}
              </span>
            )}
          </button>
        ))}
      </div>

      <section className="border border-[#d8d8d1] bg-white">
        {filter === "Requests" ? (
          pendingRequests.length === 0 ? (
            <p className="p-8 text-center text-sm text-[#76766f]">No pending requests right now.</p>
          ) : (
            <div className="divide-y divide-[#ebebe5]">
              {pendingRequests.map((request) => {
                const member = members.get(request.memberId);
                return (
                  <div className="flex flex-wrap items-center gap-4 p-5" key={request.id}>
                    <Avatar initials={initialsFor(member)} tone="lime" />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-bold">{nameFor(member)}</p>
                      <p className="mt-1 text-xs text-[#777770]">
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
                    <button
                      onClick={() => handleReject(request.id)}
                      disabled={actingId === request.id}
                      className="flex items-center gap-1 border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold transition hover:border-red-600 hover:text-red-600 disabled:opacity-50"
                    >
                      <X className="size-3.5" /> Reject
                    </button>
                    <button
                      onClick={() => handleApprove(request.id)}
                      disabled={actingId === request.id}
                      className="flex items-center gap-1 bg-[#c7f36a] px-3 py-1.5 text-xs font-bold transition hover:bg-[#d8ff8a] disabled:opacity-50"
                    >
                      <Check className="size-3.5" /> {actingId === request.id ? "Approving…" : "Approve"}
                    </button>
                  </div>
                );
              })}
            </div>
          )
        ) : listForTab.length === 0 ? (
          <p className="p-8 text-center text-sm text-[#76766f]">Nothing here yet.</p>
        ) : (
          <div className="divide-y divide-[#ebebe5]">
            {listForTab.map((s) => {
              const member = members.get(requestMemberId(s.id) ?? -1);
              const isEditing = editingSessionId === s.id;
              return (
                <div className="flex flex-wrap items-center gap-4 p-5" key={s.id}>
                  <Avatar initials={initialsFor(member)} tone="peach" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-bold">{nameFor(member)}</p>
                    {isEditing ? (
                      <div className="mt-2 flex items-center gap-2">
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
                      <p className="mt-1 text-xs text-[#777770]">
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
                  {filter === "Upcoming" &&
                    (isEditing ? (
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setEditingSessionId(null)}
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
                          <Pencil className="size-3.5" /> Reschedule
                        </button>
                        <button
                          onClick={() => handleCancelSession(s)}
                          disabled={actingId === s.id}
                          className="border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold text-[#8a4636] transition hover:border-red-600 hover:text-red-600 disabled:opacity-50"
                        >
                          Cancel
                        </button>
                      </div>
                    ))}
                  {filter === "Completed" && <StatusPill label="Completed" tone="ink" />}
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
