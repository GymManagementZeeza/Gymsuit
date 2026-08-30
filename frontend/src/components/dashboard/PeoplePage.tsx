"use client";

/* Training Ledger page: People operations organized as a precise, human-centered membership ledger. */
import { useEffect, useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, StatusPill, TableAction } from "@/components/dashboard/ui";
import MemberFormModal from "@/components/dashboard/MemberFormModal";
import ChangePlanModal from "@/components/dashboard/ChangePlanModal";
import TakePaymentModal from "@/components/dashboard/TakePaymentModal";
import NotifyModal from "@/components/dashboard/NotifyModal";
import PaymentCountdownBar from "@/components/dashboard/PaymentCountdownBar";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listCurrentSubscriptions, listPendingSubscriptions, type MemberSubscription } from "@/lib/memberSubscriptions";
import { getGym, type Gym } from "@/lib/gyms";
import { getCurrentUser, type CurrentUser } from "@/lib/users";
import { listTeamManagers, removeManager as removeManagerRequest, type TeamManager } from "@/lib/team";
import { listTrainers, deleteTrainer, type Trainer } from "@/lib/trainers";
import { assignMemberTrainer } from "@/lib/members";
import TeamManagerModal from "@/components/dashboard/TeamManagerModal";
import TrainerFormModal from "@/components/dashboard/TrainerFormModal";
import TrainerPayModal from "@/components/dashboard/TrainerPayModal";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import { Search, Plus, ShieldCheck, Award, UserRoundPlus, BellRing, WalletCards } from "lucide-react";

function initials(firstName: string, lastName: string) {
  return `${firstName[0] ?? ""}${lastName[0] ?? ""}`.toUpperCase();
}

function scopeSummary(scopes: TeamManager["scopes"]) {
  if (scopes.length === 0) return "Staff & members";
  const labels: Record<TeamManager["scopes"][number], string> = { FINANCE: "Finance", SETTINGS: "Settings", TRAINERS: "Trainers" };
  return `Staff & members + ${scopes.map((s) => labels[s]).join(", ")}`;
}

export default function PeoplePage() {
  const pathname = usePathname();
  const session = useSession();
  const trainerMode = pathname === "/dashboard/trainers";
  const accessMode = pathname === "/dashboard/team-access";
  const title = accessMode ? "Assign access with intent." : trainerMode ? "Your coaching team." : "People who move here.";

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [payModalOpen, setPayModalOpen] = useState(false);

  useEffect(() => {
    getCurrentUser()
      .then(setCurrentUser)
      .catch(() => {});
  }, []);

  const canSetPay =
    currentUser?.role === "OWNER" || (currentUser?.role === "MANAGER" && currentUser.managerScopes.includes("FINANCE"));

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow={accessMode ? "Accounts & access" : trainerMode ? "People · Trainers" : "People · Members"}
        title={title}
        description={
          accessMode
            ? "Manage gym roles without blurring responsibilities across locations."
            : trainerMode
              ? "See coaching credentials, schedules, and payment readiness at a glance."
              : "Profiles, health notes, emergency contacts, and their membership history."
        }
        actions={
          trainerMode ? (
            canSetPay && (
              <ActionButton icon={<WalletCards className="size-4" />} onClick={() => setPayModalOpen(true)}>
                Pay trainer
              </ActionButton>
            )
          ) : undefined
        }
      />
      {accessMode ? <AccessPanel /> : trainerMode ? <TrainerPanel /> : <MembersPanel />}

      {payModalOpen && session?.gymId && (
        <TrainerPayModal gymId={session.gymId} onClose={() => setPayModalOpen(false)} />
      )}
    </div>
  );
}

function MembersPanel() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [members, setMembers] = useState<Member[]>([]);
  const [subscriptionsByMember, setSubscriptionsByMember] = useState<Record<number, MemberSubscription>>({});
  const [pendingByMember, setPendingByMember] = useState<Record<number, MemberSubscription>>({});
  const [gym, setGym] = useState<Gym | null>(null);
  const [trainers, setTrainers] = useState<Trainer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [modalState, setModalState] = useState<"closed" | "create" | Member>("closed");
  const [planModalMember, setPlanModalMember] = useState<Member | null>(null);
  const [takePaymentMember, setTakePaymentMember] = useState<Member | null>(null);
  const [notifyMemberTarget, setNotifyMemberTarget] = useState<Member | null>(null);
  const [assigningTrainerFor, setAssigningTrainerFor] = useState<number | null>(null);

  const handleAssignTrainer = async (memberId: number, trainerId: number | null) => {
    if (!gymId) return;
    setAssigningTrainerFor(memberId);
    try {
      const updated = await assignMemberTrainer(gymId, memberId, trainerId);
      setMembers((prev) => prev.map((m) => (m.id === memberId ? updated : m)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not assign a trainer.");
    } finally {
      setAssigningTrainerFor(null);
    }
  };

  const refreshSubscriptions = () => {
    if (!gymId) return;
    Promise.all([listCurrentSubscriptions(gymId), listPendingSubscriptions(gymId)])
      .then(([active, pending]) => {
        const byMember: Record<number, MemberSubscription> = {};
        for (const sub of active) byMember[sub.memberId] = sub;
        setSubscriptionsByMember(byMember);
        const pendingMap: Record<number, MemberSubscription> = {};
        for (const sub of pending) pendingMap[sub.memberId] = sub;
        setPendingByMember(pendingMap);
      })
      .catch(() => {});
  };

  useEffect(() => {
    if (!gymId) return;
    let cancelled = false;
    Promise.all([
      listMembers(gymId),
      listCurrentSubscriptions(gymId),
      listPendingSubscriptions(gymId),
      getGym(gymId),
      listTrainers(gymId),
    ])
      .then(([memberData, active, pending, gymData, trainerData]) => {
        if (cancelled) return;
        setMembers(memberData);
        const byMember: Record<number, MemberSubscription> = {};
        for (const sub of active) byMember[sub.memberId] = sub;
        setSubscriptionsByMember(byMember);
        const pendingMap: Record<number, MemberSubscription> = {};
        for (const sub of pending) pendingMap[sub.memberId] = sub;
        setPendingByMember(pendingMap);
        setGym(gymData);
        setTrainers(trainerData);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load members.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [gymId]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return members;
    return members.filter((m) =>
      `${m.firstName} ${m.lastName} ${m.email ?? ""} ${m.phone}`.toLowerCase().includes(q)
    );
  }, [members, query]);

  const handleSaved = (saved: Member) => {
    setMembers((prev) => {
      const exists = prev.some((m) => m.id === saved.id);
      return exists ? prev.map((m) => (m.id === saved.id ? saved : m)) : [saved, ...prev];
    });
    setModalState("closed");
    refreshSubscriptions();
  };

  if (!session) {
    return null;
  }

  if (!gymId) {
    return (
      <section className="border border-[#d8d8d1] bg-white p-8 text-center text-sm text-[#76766f]">
        This account isn&apos;t linked to a gym, so there&apos;s no member directory to show.
      </section>
    );
  }

  return (
    <section className="border border-[#d8d8d1] bg-white">
      <div className="flex flex-col gap-4 border-b border-[#e5e5de] p-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="ledger-label">Member directory</p>
          <h2 className="mt-2 text-xl font-bold tracking-[-0.03em]">
            {loading ? "Loading members…" : `${members.length} member${members.length === 1 ? "" : "s"}`}
          </h2>
        </div>
        <div className="flex items-center gap-2">
          <label className="flex h-9 items-center gap-2 border border-[#d8d8d1] px-3 text-[#777770]">
            <Search className="size-4" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-32 bg-transparent text-sm outline-none placeholder:text-[#9b9b94]"
              placeholder="Find a member"
            />
          </label>
          <ActionButton icon={<Plus className="size-4" />} onClick={() => setModalState("create")}>
            Add member
          </ActionButton>
        </div>
      </div>

      {error && (
        <div className="px-5 pt-4">
          <ErrorBanner message={error} />
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <p className="px-5 py-10 text-center text-sm text-[#76766f]">
          {members.length === 0 ? "No members yet — add your first one." : "No members match your search."}
        </p>
      )}

      {filtered.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1120px] text-left">
            <thead className="border-b border-[#e5e5de] bg-[#fafaf6]">
              <tr className="text-[10px] uppercase tracking-[0.12em] text-[#76766f]">
                <th className="px-5 py-3 font-bold">Member</th>
                <th className="px-4 py-3 font-bold">Contact</th>
                <th className="px-4 py-3 font-bold">Plan</th>
                <th className="px-4 py-3 font-bold">Trainer</th>
                <th className="px-4 py-3 font-bold">Next payment</th>
                <th className="px-4 py-3 font-bold">Joined</th>
                <th className="px-4 py-3 font-bold">Waiver</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-[#efefe9]">
              {filtered.map((member, index) => {
                const subscription = subscriptionsByMember[member.id];
                const pending = pendingByMember[member.id];
                return (
                <tr className="transition hover:bg-[#fafaf6]" key={member.id}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <span
                        className={`grid size-9 place-items-center rounded-full text-[10px] font-bold ${index % 2 ? "bg-[#d7e4fd]" : "bg-[#f4cfbd]"}`}
                      >
                        {member.firstName[0]}
                        {member.lastName[0]}
                      </span>
                      <div>
                        <p className="text-sm font-bold">
                          {member.firstName} {member.lastName}
                        </p>
                        {member.gender && <p className="mt-0.5 text-[11px] text-[#74746d]">{member.gender}</p>}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-4 text-xs text-[#696962]">
                    <p>{member.phone}</p>
                    {member.email && <p className="mt-0.5">{member.email}</p>}
                  </td>
                  <td className="px-4 py-4">
                    {subscription ? (
                      <>
                        <p className="text-sm font-semibold">{subscription.planName}</p>
                        <p className="mt-0.5 text-[11px] text-[#74746d]">
                          {subscription.planCurrency} {subscription.planPrice}
                        </p>
                      </>
                    ) : pending ? (
                      <>
                        <p className="text-sm font-semibold">{pending.planName}</p>
                        <StatusPill label="Awaiting payment" tone="orange" />
                      </>
                    ) : (
                      <StatusPill label="No active plan" tone="orange" />
                    )}
                  </td>
                  <td className="px-4 py-4">
                    <select
                      value={member.trainerId ?? ""}
                      disabled={assigningTrainerFor === member.id}
                      onChange={(e) => handleAssignTrainer(member.id, e.target.value ? Number(e.target.value) : null)}
                      className="h-8 max-w-[140px] border border-[#d8d8d1] bg-white px-2 text-xs outline-none transition-colors focus:border-[#24241f] disabled:opacity-50"
                    >
                      <option value="">No trainer</option>
                      {trainers.map((trainer) => (
                        <option key={trainer.id} value={trainer.id}>
                          {trainer.firstName} {trainer.lastName}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-4">
                    {subscription && subscription.currentPeriodStart && subscription.currentPeriodEnd ? (
                      <div className="flex flex-col gap-1">
                        <span className="text-xs text-[#696962]">{subscription.currentPeriodEnd}</span>
                        <PaymentCountdownBar
                          periodStart={subscription.currentPeriodStart}
                          periodEnd={subscription.currentPeriodEnd}
                        />
                      </div>
                    ) : (
                      <span className="text-xs text-[#696962]">—</span>
                    )}
                  </td>
                  <td className="px-4 py-4 text-xs text-[#696962]">{member.joinDate}</td>
                  <td className="px-4 py-4">
                    <StatusPill label={member.waiverAccepted ? "Accepted" : "Missing"} tone={member.waiverAccepted ? "lime" : "orange"} />
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center justify-end gap-2">
                      {pending && (
                        <TableAction onClick={() => setTakePaymentMember(member)}>Take payment</TableAction>
                      )}
                      <TableAction onClick={() => setPlanModalMember(member)}>Plan</TableAction>
                      <TableAction onClick={() => setModalState(member)}>Edit</TableAction>
                      <button
                        type="button"
                        className="grid size-8 place-items-center text-[#3154a2] transition hover:bg-[#dce6ff]"
                        aria-label={`Notify ${member.firstName} ${member.lastName}`}
                        onClick={() => setNotifyMemberTarget(member)}
                      >
                        <BellRing className="size-4" />
                      </button>
                    </div>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {modalState !== "closed" && (
        <MemberFormModal
          gymId={gymId}
          member={modalState === "create" ? null : modalState}
          onClose={() => setModalState("closed")}
          onSaved={handleSaved}
        />
      )}

      {planModalMember && (
        <ChangePlanModal
          gymId={gymId}
          member={planModalMember}
          onClose={() => setPlanModalMember(null)}
          onChanged={() => {
            setPlanModalMember(null);
            refreshSubscriptions();
          }}
        />
      )}

      {takePaymentMember && pendingByMember[takePaymentMember.id] && (
        <TakePaymentModal
          gymId={gymId}
          member={takePaymentMember}
          subscription={pendingByMember[takePaymentMember.id]}
          gym={gym}
          onClose={() => setTakePaymentMember(null)}
          onPaid={() => {
            setTakePaymentMember(null);
            refreshSubscriptions();
          }}
        />
      )}

      {notifyMemberTarget && (
        <NotifyModal
          gymId={gymId}
          member={notifyMemberTarget}
          onClose={() => setNotifyMemberTarget(null)}
          onSent={() => setNotifyMemberTarget(null)}
        />
      )}
    </section>
  );
}

function TrainerPanel() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [trainers, setTrainers] = useState<Trainer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalState, setModalState] = useState<"closed" | "create" | Trainer>("closed");
  const [removingTrainer, setRemovingTrainer] = useState<Trainer | null>(null);
  const [removing, setRemoving] = useState(false);

  const canManage =
    currentUser?.role === "OWNER" || (currentUser?.role === "MANAGER" && currentUser.managerScopes.includes("TRAINERS"));
  const canSetPay =
    currentUser?.role === "OWNER" || (currentUser?.role === "MANAGER" && currentUser.managerScopes.includes("FINANCE"));

  useEffect(() => {
    getCurrentUser()
      .then(setCurrentUser)
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!gymId) return;
    listTrainers(gymId)
      .then(setTrainers)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load the trainer roster."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const handleSaved = (trainer: Trainer) => {
    setTrainers((prev) =>
      prev.some((t) => t.id === trainer.id) ? prev.map((t) => (t.id === trainer.id ? trainer : t)) : [...prev, trainer]
    );
    setModalState("closed");
  };

  const handleRemove = async () => {
    if (!gymId || !removingTrainer) return;
    setRemoving(true);
    try {
      await deleteTrainer(gymId, removingTrainer.id);
      setTrainers((prev) => prev.filter((t) => t.id !== removingTrainer.id));
      setRemovingTrainer(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not remove this trainer.");
    } finally {
      setRemoving(false);
    }
  };

  return (
    <section className="grid gap-4 lg:grid-cols-12">
      <div className="border border-[#d8d8d1] bg-white lg:col-span-8">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Trainer roster</p>
            <h2 className="mt-2 text-xl font-bold tracking-[-0.03em]">
              {loading ? "Loading trainers…" : `${trainers.length} coach${trainers.length === 1 ? "" : "es"}`}
            </h2>
          </div>
          {canManage && (
            <ActionButton icon={<Plus className="size-4" />} onClick={() => setModalState("create")}>
              Add trainer
            </ActionButton>
          )}
        </div>

        {error && (
          <div className="p-4">
            <ErrorBanner message={error} />
          </div>
        )}

        {!loading && trainers.length === 0 && (
          <p className="p-8 text-center text-sm text-[#76766f]">
            No trainers yet.{canManage && " Add your first coach to build out the roster."}
          </p>
        )}

        <div className="divide-y divide-[#e8e8e2]">
          {loading &&
            [0, 1].map((i) => (
              <div className="flex items-center gap-4 p-5" key={i}>
                <div className="size-14 shrink-0 animate-pulse bg-[#ededE7]" />
                <div className="h-3 w-1/3 animate-pulse rounded bg-[#ededE7]" />
              </div>
            ))}
          {trainers.map((trainer, index) => (
            <div className="flex items-center gap-4 p-5" key={trainer.id}>
              {trainer.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element -- gym-provided URL, not a static asset
                <img src={trainer.imageUrl} alt={`${trainer.firstName} ${trainer.lastName}`} className="size-14 object-cover" />
              ) : (
                <div className={`grid size-14 place-items-center text-sm font-bold ${index % 2 ? "bg-[#d6e1fe]" : "bg-[#f5dd9f]"}`}>
                  {trainer.firstName[0]}
                  {trainer.lastName[0]}
                </div>
              )}
              <div className="min-w-0 flex-1">
                <p className="font-bold">
                  {trainer.firstName} {trainer.lastName}
                </p>
                <p className="mt-1 text-xs text-[#71716a]">{trainer.specialization || "General coaching"}</p>
                <div className="mt-2 flex items-center gap-2 text-[11px] text-[#777770]">
                  <span>{trainer.phone}</span>
                  {trainer.email && <span>· {trainer.email}</span>}
                </div>
              </div>
              {canManage && (
                <div className="flex items-center gap-2">
                  <TableAction onClick={() => setModalState(trainer)}>Edit</TableAction>
                  <button
                    type="button"
                    onClick={() => setRemovingTrainer(trainer)}
                    className="border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold text-[#8a4636] transition hover:border-red-600 hover:text-red-600"
                  >
                    Remove
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <aside className="cut-corner bg-[#e6eaf6] p-5 lg:col-span-4">
        <Award className="size-6 text-[#395cbd]" />
        <p className="display-face mt-5 text-3xl leading-[1.05]">
          Every member
          <br />
          picks their coach.
        </p>
        <p className="mt-4 text-xs leading-relaxed text-[#596276]">
          Members choose a trainer from their own dashboard, or you can assign one directly from the member
          directory. {canManage ? "You can add, edit, or remove trainers here." : "Ask an owner or a manager with trainer access to change the roster."}
        </p>
      </aside>

      {modalState !== "closed" && gymId && (
        <TrainerFormModal
          gymId={gymId}
          trainer={modalState === "create" ? null : modalState}
          canSetPay={canSetPay}
          onClose={() => setModalState("closed")}
          onSaved={handleSaved}
        />
      )}

      {removingTrainer && (
        <ConfirmDialog
          title="Remove trainer?"
          description={`${removingTrainer.firstName} ${removingTrainer.lastName} will be removed from the roster. Members assigned to them will lose that assignment.`}
          confirmLabel={removing ? "Removing…" : "Remove"}
          onConfirm={handleRemove}
          onCancel={() => setRemovingTrainer(null)}
          disabled={removing}
        />
      )}
    </section>
  );
}

function AccessPanel() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const isOwner = session?.role === "OWNER";

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [managers, setManagers] = useState<TeamManager[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalState, setModalState] = useState<"closed" | "create" | TeamManager>("closed");
  const [removingManager, setRemovingManager] = useState<TeamManager | null>(null);
  const [removing, setRemoving] = useState(false);

  useEffect(() => {
    getCurrentUser()
      .then(setCurrentUser)
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!gymId) return;
    listTeamManagers(gymId)
      .then(setManagers)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load the team roster."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const handleSaved = (manager: TeamManager) => {
    setManagers((prev) =>
      prev.some((m) => m.id === manager.id) ? prev.map((m) => (m.id === manager.id ? manager : m)) : [...prev, manager]
    );
    setModalState("closed");
  };

  const handleRemove = async () => {
    if (!gymId || !removingManager) return;
    setRemoving(true);
    try {
      await removeManagerRequest(gymId, removingManager.id);
      setManagers((prev) => prev.filter((m) => m.id !== removingManager.id));
      setRemovingManager(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not remove this manager.");
    } finally {
      setRemoving(false);
    }
  };

  return (
    <section className="grid gap-4 xl:grid-cols-12">
      <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Role directory</p>
            <h2 className="mt-2 text-xl font-bold tracking-[-0.03em]">Access follows the gym</h2>
          </div>
          {isOwner && (
            <button
              type="button"
              onClick={() => setModalState("create")}
              className="flex items-center gap-2 bg-[#c7f36a] px-3.5 py-2.5 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
            >
              <UserRoundPlus className="size-4" /> Add manager
            </button>
          )}
        </div>

        {error && (
          <div className="p-4">
            <ErrorBanner message={error} />
          </div>
        )}

        <div className="divide-y divide-[#e9e9e3]">
          {currentUser && (
            <div className="flex items-center gap-3 p-4">
              <div className="grid size-10 place-items-center rounded-full bg-[#c7f36a] text-[10px] font-bold">
                {initials(currentUser.displayName.split(" ")[0] ?? "", currentUser.displayName.split(" ").slice(1).join(" ") || "")}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-bold">
                  {currentUser.displayName} <span className="font-normal text-[#8a8a82]">(you)</span>
                </p>
                <p className="mt-1 text-xs text-[#72726b]">{currentUser.role === "OWNER" ? "Owner" : currentUser.role}</p>
              </div>
              <StatusPill label="Full access" tone="ink" />
            </div>
          )}

          {loading &&
            [0, 1].map((i) => (
              <div className="flex items-center gap-3 p-4" key={i}>
                <div className="size-10 shrink-0 animate-pulse rounded-full bg-[#ededE7]" />
                <div className="h-3 w-1/3 animate-pulse rounded bg-[#ededE7]" />
              </div>
            ))}

          {!loading && managers.length === 0 && (
            <p className="p-8 text-center text-sm text-[#76766f]">
              No managers yet.{isOwner && " Add one by email to share ownership of daily operations."}
            </p>
          )}

          {managers.map((manager) => (
            <div className="flex items-center gap-3 p-4" key={manager.id}>
              <div className="grid size-10 place-items-center rounded-full bg-[#e1e1dd] text-[10px] font-bold">
                {initials(manager.firstName, manager.lastName)}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-bold">
                  {manager.firstName} {manager.lastName}
                </p>
                <p className="mt-1 text-xs text-[#72726b]">{manager.email}</p>
              </div>
              <StatusPill label={scopeSummary(manager.scopes)} tone="blue" />
              {isOwner && (
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setModalState(manager)}
                    className="border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold transition hover:border-[#24241f]"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => setRemovingManager(manager)}
                    className="border border-[#d8d8d1] px-3 py-1.5 text-xs font-bold text-[#8a4636] transition hover:border-red-600 hover:text-red-600"
                  >
                    Remove
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <aside className="cut-corner bg-[#24241f] p-5 text-white xl:col-span-4">
        <ShieldCheck className="size-6 text-[#c7f36a]" />
        <p className="display-face mt-5 text-3xl leading-[1.02]">
          One person,
          <br />
          more than one role.
        </p>
        <p className="mt-4 text-xs leading-relaxed text-[#babaae]">
          Managers use the same dashboard as you, scoped to your gym. Every manager can handle members, trainers,
          and attendance — grant finance or settings access only where you want it.
        </p>
      </aside>

      {modalState !== "closed" && gymId && (
        <TeamManagerModal
          gymId={gymId}
          manager={modalState === "create" ? null : modalState}
          onClose={() => setModalState("closed")}
          onSaved={handleSaved}
        />
      )}

      {removingManager && (
        <ConfirmDialog
          title="Remove manager?"
          description={`${removingManager.firstName} ${removingManager.lastName} will lose access to this gym's dashboard immediately.`}
          confirmLabel={removing ? "Removing…" : "Remove"}
          onConfirm={handleRemove}
          onCancel={() => setRemovingManager(null)}
          disabled={removing}
        />
      )}
    </section>
  );
}
