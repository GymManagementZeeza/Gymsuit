"use client";

/* Training Ledger page: Trainers — pick your coach, then book a class time that's actually free. */
import { useEffect, useMemo, useState } from "react";
import { Avatar, StatusPill } from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { getMember, assignMemberTrainer, type Member } from "@/lib/members";
import { listTrainers, type Trainer } from "@/lib/trainers";
import {
  listSessionRequests,
  createSessionRequest,
  cancelSessionRequest,
  type SessionRequest,
} from "@/lib/sessionRequests";
import { listSessions, type TrainingSession } from "@/lib/sessions";
import { Award, Check, X } from "lucide-react";

function initials(firstName: string, lastName: string) {
  return `${firstName[0] ?? ""}${lastName[0] ?? ""}`.toUpperCase();
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const STATUS_TONE: Record<SessionRequest["status"], "lime" | "orange" | "blue" | "ink"> = {
  PENDING: "blue",
  APPROVED: "lime",
  REJECTED: "orange",
  CANCELLED: "ink",
};

export default function TrainersPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const memberId = session?.memberId ?? null;

  const [member, setMember] = useState<Member | null>(null);
  const [trainers, setTrainers] = useState<Trainer[]>([]);
  const [requests, setRequests] = useState<SessionRequest[]>([]);
  const [sessions, setSessions] = useState<TrainingSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [choosing, setChoosing] = useState(false);
  const [assigningId, setAssigningId] = useState<number | null>(null);

  const [bookDate, setBookDate] = useState("");
  const [bookTime, setBookTime] = useState("");
  const [bookNotes, setBookNotes] = useState("");
  const [booking, setBooking] = useState(false);
  const [bookError, setBookError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  const load = () => {
    if (!gymId || !memberId) return;
    Promise.all([getMember(gymId, memberId), listTrainers(gymId), listSessionRequests(gymId), listSessions(gymId)])
      .then(([memberData, trainerData, requestData, sessionData]) => {
        setMember(memberData);
        setTrainers(trainerData);
        setRequests(requestData.sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        setSessions(sessionData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load trainers."))
      .finally(() => setLoading(false));
  };

  useEffect(load, [gymId, memberId]);

  const handleChoose = async (trainerId: number) => {
    if (!gymId || !memberId) return;
    setAssigningId(trainerId);
    try {
      const updated = await assignMemberTrainer(gymId, memberId, trainerId);
      setMember(updated);
      setChoosing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not choose this trainer.");
    } finally {
      setAssigningId(null);
    }
  };

  const myTrainer = member?.trainerId ? trainers.find((t) => t.id === member.trainerId) ?? null : null;

  const handleBook = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!gymId || !myTrainer || !bookDate || !bookTime) return;
    setBookError(null);
    setBooking(true);
    try {
      const created = await createSessionRequest(gymId, {
        trainerId: myTrainer.id,
        requestedStartTime: `${bookDate}T${bookTime}:00`,
        notes: bookNotes || undefined,
      });
      setRequests((prev) => [created, ...prev]);
      setBookDate("");
      setBookTime("");
      setBookNotes("");
    } catch (err) {
      setBookError(err instanceof Error ? err.message : "Could not book that time.");
    } finally {
      setBooking(false);
    }
  };

  const handleCancel = async (requestId: number) => {
    if (!gymId) return;
    setCancellingId(requestId);
    try {
      const updated = await cancelSessionRequest(gymId, requestId);
      setRequests((prev) => prev.map((r) => (r.id === requestId ? updated : r)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not cancel that booking.");
    } finally {
      setCancellingId(null);
    }
  };

  const sessionsById = useMemo(() => new Map(sessions.map((s) => [s.id, s])), [sessions]);

  if (loading) {
    return <p className="page-enter p-8 text-center text-sm text-[#76766f]">Loading trainers…</p>;
  }

  return (
    <div className="page-enter space-y-7">
      {error && <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {myTrainer && !choosing ? (
        <section className="cut-corner relative overflow-hidden bg-[#24241f] p-6 text-white sm:p-8">
          <div className="absolute inset-0 dot-field opacity-[0.12]" aria-hidden="true" />
          <div
            className="absolute -right-16 -bottom-20 h-56 w-56 rounded-full border-[30px] border-[#c7f36a]/20"
            aria-hidden="true"
          />
          <div className="relative flex flex-col gap-8 sm:flex-row sm:items-center sm:justify-between">
            <div className="max-w-md">
              <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Your go-to coach</p>
              <p className="display-face mt-3 text-3xl leading-[1.05] sm:text-4xl">
                {myTrainer.firstName}
                {"'s"} got your
                <br />
                training covered.
              </p>
              {myTrainer.bio && <p className="mt-4 text-sm leading-relaxed text-white/65">{myTrainer.bio}</p>}
              <div className="mt-6">
                <button
                  onClick={() => setChoosing(true)}
                  className="border border-white/30 px-3.5 py-2.5 text-sm font-bold transition hover:bg-white hover:text-[#24241f]"
                >
                  Change trainer
                </button>
              </div>
            </div>
            <div className="flex items-center gap-3 self-start bg-white/[0.06] p-4 sm:self-center">
              <Avatar initials={initials(myTrainer.firstName, myTrainer.lastName)} tone="sky" size="lg" />
              <div>
                <p className="text-sm font-bold">
                  {myTrainer.firstName} {myTrainer.lastName}
                </p>
                <p className="text-xs text-white/65">{myTrainer.specialization || "General coaching"}</p>
              </div>
            </div>
          </div>
        </section>
      ) : (
        <section className="border border-[#d8d8d1] bg-white">
          <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
            <div>
              <p className="ledger-label">Choose your coach</p>
              <h2 className="mt-2 text-xl font-bold tracking-[-0.03em]">
                {myTrainer ? "Pick a different trainer" : "Pick a trainer to get started"}
              </h2>
            </div>
            {myTrainer && (
              <button
                onClick={() => setChoosing(false)}
                className="text-xs font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4"
              >
                Cancel
              </button>
            )}
          </div>

          {trainers.length === 0 ? (
            <p className="p-8 text-center text-sm text-[#76766f]">Your gym hasn&apos;t added any trainers yet.</p>
          ) : (
            <div className="grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-3">
              {trainers.map((trainer) => {
                const isMine = myTrainer?.id === trainer.id;
                return (
                  <div key={trainer.id} className="flex flex-col gap-3 border border-[#d8d8d1] p-4">
                    <div className="flex items-center gap-3">
                      <Avatar initials={initials(trainer.firstName, trainer.lastName)} tone="peach" />
                      <div className="min-w-0">
                        <p className="text-sm font-bold">
                          {trainer.firstName} {trainer.lastName}
                        </p>
                        <p className="text-xs text-[#71716a]">{trainer.specialization || "General coaching"}</p>
                      </div>
                    </div>
                    {trainer.bio && <p className="text-xs leading-relaxed text-[#6e6e67]">{trainer.bio}</p>}
                    <button
                      onClick={() => handleChoose(trainer.id)}
                      disabled={isMine || assigningId === trainer.id}
                      className={`mt-auto flex items-center justify-center gap-2 px-3 py-2 text-xs font-bold transition ${
                        isMine
                          ? "bg-[#f1f8de] text-[#4c592e]"
                          : "bg-[#c7f36a] text-[#25251f] hover:bg-[#d8ff8a] disabled:opacity-60"
                      }`}
                    >
                      {isMine ? (
                        <>
                          <Check className="size-3.5" /> Your trainer
                        </>
                      ) : assigningId === trainer.id ? (
                        "Choosing…"
                      ) : (
                        "Choose as my trainer"
                      )}
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      )}

      {myTrainer && !choosing && (
        <section className="grid gap-4 xl:grid-cols-12">
          <div className="border border-[#d8d8d1] bg-white p-5 xl:col-span-5">
            <p className="ledger-label">Book a class</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Pick a time with {myTrainer.firstName}</h2>
            <p className="mt-2 text-xs leading-relaxed text-[#6e6e67]">
              If {myTrainer.firstName} is already booked at that time, you&apos;ll be asked to pick another slot.
            </p>
            <form className="mt-4 flex flex-col gap-3" onSubmit={handleBook}>
              <div className="grid grid-cols-2 gap-3">
                <label className="flex flex-col gap-1.5">
                  <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Date</span>
                  <input
                    type="date"
                    required
                    value={bookDate}
                    min={new Date().toISOString().slice(0, 10)}
                    onChange={(e) => setBookDate(e.target.value)}
                    className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
                  />
                </label>
                <label className="flex flex-col gap-1.5">
                  <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Time</span>
                  <input
                    type="time"
                    required
                    value={bookTime}
                    onChange={(e) => setBookTime(e.target.value)}
                    className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
                  />
                </label>
              </div>
              <label className="flex flex-col gap-1.5">
                <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Notes (optional)</span>
                <textarea
                  rows={2}
                  value={bookNotes}
                  onChange={(e) => setBookNotes(e.target.value)}
                  placeholder="What do you want to work on?"
                  className="border border-[#d8d8d1] bg-white p-3 text-sm outline-none transition-colors focus:border-[#24241f]"
                />
              </label>
              {bookError && <p className="text-xs font-semibold text-red-600">{bookError}</p>}
              <button
                type="submit"
                disabled={booking}
                className="bg-[#c7f36a] px-4 py-2.5 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
              >
                {booking ? "Requesting…" : "Request this time"}
              </button>
            </form>
          </div>

          <div className="border border-[#d8d8d1] bg-white xl:col-span-7">
            <div className="border-b border-[#e5e5de] p-5">
              <p className="ledger-label">Your bookings</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Requests &amp; sessions</h2>
            </div>
            {requests.length === 0 ? (
              <p className="p-8 text-center text-sm text-[#76766f]">No bookings yet — request a time above.</p>
            ) : (
              <div className="divide-y divide-[#ebebe5]">
                {requests.map((request) => {
                  const trainer = trainers.find((t) => t.id === request.trainerId);
                  const liveSession = request.sessionId ? sessionsById.get(request.sessionId) : null;
                  const displayStart = liveSession?.startTime ?? request.requestedStartTime;
                  return (
                    <div className="flex items-center justify-between gap-3 p-4" key={request.id}>
                      <div className="min-w-0">
                        <p className="text-sm font-bold">
                          {trainer ? `${trainer.firstName} ${trainer.lastName}` : "Trainer"}
                        </p>
                        <p className="mt-1 text-xs text-[#74746d]">{formatDateTime(displayStart)}</p>
                        {request.status === "REJECTED" && request.responseNote && (
                          <p className="mt-1 text-xs text-[#a44a35]">{request.responseNote}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <StatusPill label={request.status} tone={STATUS_TONE[request.status]} />
                        {request.status === "PENDING" && (
                          <button
                            onClick={() => handleCancel(request.id)}
                            disabled={cancellingId === request.id}
                            className="grid size-8 place-items-center text-[#8a8a82] transition hover:bg-[#efefe9] hover:text-red-600 disabled:opacity-50"
                            aria-label="Cancel booking"
                          >
                            <X className="size-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      )}

      {myTrainer && !choosing && (
        <section className="cut-corner bg-[#e6eaf6] p-5">
          <div className="flex items-start gap-2">
            <Award className="size-5 shrink-0 text-[#395cbd]" />
            <p className="ledger-label">Your coach</p>
          </div>
          <h3 className="mt-3 text-lg font-bold leading-snug tracking-[-0.02em]">
            {myTrainer.specialization || "Here to help you hit your goals."}
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-[#596276]">
            {myTrainer.firstName} reviews every request and can adjust the time if it doesn&apos;t work for them.
          </p>
        </section>
      )}
    </div>
  );
}
