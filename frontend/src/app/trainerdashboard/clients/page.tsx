"use client";

/* Training Ledger page: Client desk — the trainer's actual assigned members and their real bookings. */
import { useEffect, useMemo, useState } from "react";
import { Avatar } from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { listMembers, assignMemberTrainer, type Member } from "@/lib/members";
import { listSessions, type TrainingSession } from "@/lib/sessions";
import { listSessionRequests, type SessionRequest } from "@/lib/sessionRequests";
import { sendTodayWorkout, type SuggestedExerciseInput } from "@/lib/trainerWorkouts";
import { ArrowUpRight, Plus, Search, X } from "lucide-react";

const AVATAR_TONES = ["peach", "sky", "mint", "lavender"] as const;

function initialsOf(member: Member) {
  return `${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase();
}

function formatWhen(iso: string) {
  const date = new Date(iso);
  const today = new Date();
  const isToday = date.toDateString() === today.toDateString();
  const time = date.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  return isToday
    ? `Today · ${time}`
    : `${date.toLocaleDateString(undefined, { weekday: "short" })} · ${time}`;
}

export default function ClientsPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const trainerId = session?.trainerId ?? null;

  const [members, setMembers] = useState<Member[]>([]);
  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [addOpen, setAddOpen] = useState(false);
  const [addingId, setAddingId] = useState<number | null>(null);

  // Send today's workout modal
  const [workoutMember, setWorkoutMember] = useState<Member | null>(null);
  const [workoutRows, setWorkoutRows] = useState([{ exerciseName: "", sets: "3", reps: "10", weightKg: "0" }]);
  const [workoutMessage, setWorkoutMessage] = useState("");
  const [workoutSending, setWorkoutSending] = useState(false);
  const [workoutSent, setWorkoutSent] = useState(false);
  const [workoutError, setWorkoutError] = useState<string | null>(null);

  const openWorkoutModal = (member: Member) => {
    setWorkoutMember(member);
    setWorkoutRows([{ exerciseName: "", sets: "3", reps: "10", weightKg: "0" }]);
    setWorkoutMessage("");
    setWorkoutSent(false);
    setWorkoutError(null);
  };

  const handleSendWorkout = async () => {
    if (!gymId || !trainerId || !workoutMember) return;
    const exercises: SuggestedExerciseInput[] = [];
    for (const row of workoutRows) {
      const name = row.exerciseName.trim();
      const sets = Number(row.sets);
      const reps = Number(row.reps);
      const weightKg = Number(row.weightKg);
      if (!name || !Number.isFinite(sets) || sets < 1 || !Number.isFinite(reps) || reps < 1 || !Number.isFinite(weightKg) || weightKg < 0) {
        setWorkoutError("Each exercise needs a name, sets and reps of at least 1, and a weight of 0 or more.");
        return;
      }
      exercises.push({ exerciseName: name, sets, reps, weightKg });
    }
    if (exercises.length === 0) {
      setWorkoutError("Add at least one exercise.");
      return;
    }
    setWorkoutSending(true);
    setWorkoutError(null);
    try {
      await sendTodayWorkout(gymId, trainerId, workoutMember.id, {
        message: workoutMessage.trim() || undefined,
        exercises,
      });
      setWorkoutSent(true);
    } catch (err) {
      setWorkoutError(err instanceof Error ? err.message : "Could not send the workout.");
    } finally {
      setWorkoutSending(false);
    }
  };

  const load = () => {
    if (!gymId) return;
    Promise.all([listMembers(gymId), listSessions(gymId), listSessionRequests(gymId)])
      .then(([memberData, sessionData, requestData]) => {
        setMembers(memberData);
        setSessions(sessionData);
        setRequests(requestData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your clients."))
      .finally(() => setLoading(false));
  };

  useEffect(load, [gymId]);

  const myClients = useMemo(() => members.filter((m) => m.trainerId === trainerId), [members, trainerId]);
  const unassignedMembers = useMemo(() => members.filter((m) => m.trainerId === null), [members]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return myClients;
    return myClients.filter((m) => `${m.firstName} ${m.lastName} ${m.email ?? ""} ${m.phone}`.toLowerCase().includes(q));
  }, [myClients, query]);

  const memberIdForSession = useMemo(() => {
    const map = new Map<number, number>();
    for (const r of requests) {
      if (r.sessionId) map.set(r.sessionId, r.memberId);
    }
    return map;
  }, [requests]);

  const upcomingSessionsByMember = useMemo(() => {
    const map = new Map<number, TrainingSession>();
    const now = new Date();
    for (const s of sessions) {
      if (s.trainerId !== trainerId) continue;
      if (new Date(s.startTime) < now) continue;
      const memberId = memberIdForSession.get(s.id);
      if (memberId === undefined) continue;
      const existing = map.get(memberId);
      if (!existing || new Date(s.startTime) < new Date(existing.startTime)) {
        map.set(memberId, s);
      }
    }
    return map;
  }, [sessions, trainerId, memberIdForSession]);

  const sessionCountByMember = useMemo(() => {
    const counts = new Map<number, number>();
    for (const s of sessions) {
      if (s.trainerId !== trainerId) continue;
      const memberId = memberIdForSession.get(s.id);
      if (memberId === undefined) continue;
      counts.set(memberId, (counts.get(memberId) ?? 0) + 1);
    }
    return counts;
  }, [sessions, trainerId, memberIdForSession]);

  const clientsWithUpcomingThisWeek = useMemo(() => {
    const in7Days = new Date();
    in7Days.setDate(in7Days.getDate() + 7);
    return myClients.filter((m) => {
      const next = upcomingSessionsByMember.get(m.id);
      return next && new Date(next.startTime) <= in7Days;
    }).length;
  }, [myClients, upcomingSessionsByMember]);

  const handleAddClient = async (memberId: number) => {
    if (!gymId || !trainerId) return;
    setAddingId(memberId);
    setError(null);
    try {
      const updated = await assignMemberTrainer(gymId, memberId, trainerId);
      setMembers((prev) => prev.map((m) => (m.id === memberId ? updated : m)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not add this client.");
    } finally {
      setAddingId(null);
    }
  };

  return (
    <div className="page-enter space-y-7">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <label className="flex h-10 max-w-xs flex-1 items-center gap-2 border border-[#d8d8d1] bg-white px-3 text-[#777770]">
          <Search className="size-4" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full bg-transparent text-sm outline-none placeholder:text-[#9b9b94]"
            placeholder="Find a client"
          />
        </label>
        <button
          onClick={() => setAddOpen(true)}
          className="flex h-10 items-center gap-2 bg-[#c7f36a] px-3.5 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
        >
          <Plus className="size-4" /> Add client
        </button>
      </div>

      {error && <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <section className="grid gap-4 xl:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
          <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
            <div>
              <p className="ledger-label">Client directory</p>
              <h2 className="mt-2 text-xl font-bold tracking-[-0.03em]">
                {loading ? "Loading…" : `${myClients.length} ${myClients.length === 1 ? "person" : "people"} in your care`}
              </h2>
            </div>
          </div>

          {!loading && filtered.length === 0 && (
            <p className="p-8 text-center text-sm text-[#76766f]">
              {myClients.length === 0 ? "No clients yet — add one to get started." : "No clients match your search."}
            </p>
          )}

          <div className="divide-y divide-[#ebebe5]">
            {filtered.map((client, index) => {
              const next = upcomingSessionsByMember.get(client.id);
              const sessionCount = sessionCountByMember.get(client.id) ?? 0;
              return (
                <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center" key={client.id}>
                  <div className="flex min-w-0 items-center gap-3 sm:w-56">
                    <Avatar initials={initialsOf(client)} tone={AVATAR_TONES[index % AVATAR_TONES.length]} />
                    <div className="min-w-0">
                      <p className="truncate text-sm font-bold">
                        {client.firstName} {client.lastName}
                      </p>
                      <p className="mt-0.5 truncate text-[11px] text-[#74746d]">
                        {sessionCount} session{sessionCount === 1 ? "" : "s"} · {client.phone}
                      </p>
                    </div>
                  </div>
                  <div className="flex-1" />
                  <div className="sm:w-36">
                    <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Next</p>
                    <p className="mt-1 text-sm font-bold">{next ? formatWhen(next.startTime) : "Not scheduled"}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => openWorkoutModal(client)}
                    className="flex items-center gap-1 bg-[#c7f36a] px-3 py-1.5 text-xs font-bold transition hover:bg-[#d8ff8a]"
                  >
                    Send workout
                  </button>
                </div>
              );
            })}
          </div>
        </div>

        <aside className="cut-corner bg-[#e6eaf6] p-5 xl:col-span-4">
          <p className="ledger-label">This week</p>
          <div className="mx-auto mt-6 grid size-28 place-items-center rounded-full border-[10px] border-[#395cbd]/25">
            <span className="display-face text-3xl text-[#24241f]">{loading ? "…" : clientsWithUpcomingThisWeek}</span>
          </div>
          <h3 className="mt-6 text-center text-lg font-bold tracking-[-0.02em]">
            {loading
              ? "Loading…"
              : myClients.length === 0
                ? "No clients yet."
                : `${clientsWithUpcomingThisWeek} of ${myClients.length} clients have a session booked this week.`}
          </h3>
        </aside>
      </section>

      {addOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
          <div className="max-h-[80vh] w-full max-w-md overflow-y-auto border border-[#d8d8d1] bg-white">
            <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
              <div>
                <p className="ledger-label">Unassigned members</p>
                <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Add a client</h2>
              </div>
              <button
                type="button"
                onClick={() => setAddOpen(false)}
                className="grid size-8 place-items-center transition hover:bg-[#efefe9]"
                aria-label="Close"
              >
                <X className="size-4" />
              </button>
            </div>
            {unassignedMembers.length === 0 ? (
              <p className="p-8 text-center text-sm text-[#76766f]">
                Every member already has a trainer. Members can also pick you as their trainer themselves.
              </p>
            ) : (
              <div className="divide-y divide-[#ebebe5]">
                {unassignedMembers.map((m) => (
                  <div className="flex items-center gap-3 p-4" key={m.id}>
                    <Avatar initials={initialsOf(m)} tone="peach" size="sm" />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-bold">
                        {m.firstName} {m.lastName}
                      </p>
                      <p className="text-xs text-[#74746d]">{m.phone}</p>
                    </div>
                    <button
                      onClick={() => handleAddClient(m.id)}
                      disabled={addingId === m.id}
                      className="flex items-center gap-1 bg-[#c7f36a] px-3 py-1.5 text-xs font-bold transition hover:bg-[#d8ff8a] disabled:opacity-50"
                    >
                      {addingId === m.id ? "Adding…" : "Add"}
                      <ArrowUpRight className="size-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
      {workoutMember && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
          <div className="max-h-[85vh] w-full max-w-lg overflow-y-auto border border-[#d8d8d1] bg-white">
            <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
              <div>
                <p className="ledger-label">Today&apos;s workout</p>
                <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
                  Send workout to {workoutMember.firstName} {workoutMember.lastName}
                </h2>
                <p className="mt-1 text-xs text-[#76766f]">
                  They get a push notification in the GymSuit app and an email with this workout.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setWorkoutMember(null)}
                className="grid size-8 shrink-0 place-items-center transition hover:bg-[#efefe9]"
                aria-label="Close"
              >
                <X className="size-4" />
              </button>
            </div>

            {workoutSent ? (
              <div className="p-8 text-center">
                <p className="text-sm font-bold">Sent!</p>
                <p className="mt-2 text-sm text-[#76766f]">
                  {workoutMember.firstName} will see today&apos;s workout in the app and got an email too.
                </p>
                <button
                  type="button"
                  onClick={() => setWorkoutMember(null)}
                  className="mt-6 bg-[#c7f36a] px-4 py-2 text-sm font-bold transition hover:bg-[#d8ff8a]"
                >
                  Done
                </button>
              </div>
            ) : (
              <div className="space-y-4 p-5">
                <div className="grid grid-cols-[1fr_64px_64px_80px_32px] items-center gap-2 text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">
                  <span>Exercise</span>
                  <span>Sets</span>
                  <span>Reps</span>
                  <span>Weight (kg)</span>
                  <span />
                </div>
                {workoutRows.map((row, i) => (
                  <div key={i} className="grid grid-cols-[1fr_64px_64px_80px_32px] items-center gap-2">
                    <input
                      value={row.exerciseName}
                      onChange={(e) =>
                        setWorkoutRows((prev) => prev.map((r, j) => (j === i ? { ...r, exerciseName: e.target.value } : r)))
                      }
                      placeholder="e.g. Barbell bench press"
                      className="h-10 border border-[#d8d8d1] px-3 text-sm outline-none"
                    />
                    <input
                      value={row.sets}
                      onChange={(e) =>
                        setWorkoutRows((prev) => prev.map((r, j) => (j === i ? { ...r, sets: e.target.value } : r)))
                      }
                      inputMode="numeric"
                      className="h-10 border border-[#d8d8d1] px-2 text-sm outline-none"
                    />
                    <input
                      value={row.reps}
                      onChange={(e) =>
                        setWorkoutRows((prev) => prev.map((r, j) => (j === i ? { ...r, reps: e.target.value } : r)))
                      }
                      inputMode="numeric"
                      className="h-10 border border-[#d8d8d1] px-2 text-sm outline-none"
                    />
                    <input
                      value={row.weightKg}
                      onChange={(e) =>
                        setWorkoutRows((prev) => prev.map((r, j) => (j === i ? { ...r, weightKg: e.target.value } : r)))
                      }
                      inputMode="decimal"
                      placeholder="0 = bodyweight"
                      className="h-10 border border-[#d8d8d1] px-2 text-sm outline-none"
                    />
                    <button
                      type="button"
                      aria-label="Remove exercise"
                      disabled={workoutRows.length === 1}
                      onClick={() => setWorkoutRows((prev) => prev.filter((_, j) => j !== i))}
                      className="grid size-8 place-items-center transition hover:bg-[#efefe9] disabled:opacity-30"
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                ))}
                <button
                  type="button"
                  onClick={() => setWorkoutRows((prev) => [...prev, { exerciseName: "", sets: "3", reps: "10", weightKg: "0" }])}
                  className="flex items-center gap-1 text-xs font-bold text-[#395cbd]"
                >
                  <Plus className="size-3.5" /> Add exercise
                </button>
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">
                    Note for the client (optional)
                  </label>
                  <textarea
                    value={workoutMessage}
                    onChange={(e) => setWorkoutMessage(e.target.value)}
                    rows={2}
                    placeholder="e.g. Focus on slow, controlled reps today."
                    className="mt-1 w-full border border-[#d8d8d1] px-3 py-2 text-sm outline-none"
                  />
                </div>
                {workoutError && (
                  <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{workoutError}</div>
                )}
                <div className="flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setWorkoutMember(null)}
                    className="h-10 border border-[#d8d8d1] px-4 text-sm font-bold transition hover:bg-[#efefe9]"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleSendWorkout}
                    disabled={workoutSending}
                    className="h-10 bg-[#c7f36a] px-4 text-sm font-bold transition hover:bg-[#d8ff8a] disabled:opacity-50"
                  >
                    {workoutSending ? "Sending…" : "Send workout"}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
