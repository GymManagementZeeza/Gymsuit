"use client";

/* People pages: members, trainers, and team access in the FLEX light-mode card language. */
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
import MemberFormModal from "@/components/dashboard/MemberFormModal";
import { ExpandableRow, DetailRow } from "@/components/dashboard/ExpandableRow";
import ChangePlanModal from "@/components/dashboard/ChangePlanModal";
import TakePaymentModal from "@/components/dashboard/TakePaymentModal";
import NotifyModal from "@/components/dashboard/NotifyModal";
import PaymentCountdownBar from "@/components/dashboard/PaymentCountdownBar";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listCurrentSubscriptions, listPendingSubscriptions, type MemberSubscription } from "@/lib/memberSubscriptions";
import { listMembershipPlans, type MembershipPlan } from "@/lib/membershipPlans";
import { getGym, type Gym } from "@/lib/gyms";
import { getCurrentUser, type CurrentUser } from "@/lib/users";
import { listTeamManagers, removeManager as removeManagerRequest, type TeamManager } from "@/lib/team";
import { listTrainers, deleteTrainer, type Trainer } from "@/lib/trainers";
import { assignMemberTrainer } from "@/lib/members";
import TeamManagerModal from "@/components/dashboard/TeamManagerModal";
import TrainerFormModal from "@/components/dashboard/TrainerFormModal";
import TrainerPayModal from "@/components/dashboard/TrainerPayModal";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import { Search, Plus, ShieldCheck, Award, UserRoundPlus, BellRing, WalletCards, AlertTriangle, Phone, Mail, MessageCircle, ChevronLeft, ChevronRight } from "lucide-react";

function initials(firstName: string, lastName: string) {
  return `${firstName[0] ?? ""}${lastName[0] ?? ""}`.toUpperCase();
}

function scopeSummary(scopes: TeamManager["scopes"]) {
  if (scopes.length === 0) return "Staff & members";
  const labels: Record<TeamManager["scopes"][number], string> = { FINANCE: "Finance", SETTINGS: "Settings", TRAINERS: "Trainers" };
  return `Staff & members + ${scopes.map((s) => labels[s]).join(", ")}`;
}

/* Small rounded action chip used in rows and tables. */
function RowAction({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="shrink-0 rounded-full bg-stone-100 px-3.5 py-2 text-xs font-bold text-stone-700 transition hover:bg-stone-200 active:scale-[0.97]"
    >
      {children}
    </button>
  );
}

function DangerRowAction({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="shrink-0 rounded-full border border-red-200 px-3.5 py-2 text-xs font-bold text-red-700 transition hover:bg-[#ffe3e3] active:scale-[0.97]"
    >
      {children}
    </button>
  );
}

export default function PeoplePage() {
  const pathname = usePathname();
  const session = useSession();
  const trainerMode = pathname === "/dashboard/trainers";
  const accessMode = pathname === "/dashboard/team-access";
  const title = accessMode ? "Assign access with intent." : trainerMode ? "Your coaching team." : "People who move here.";
  const eyebrow = accessMode ? "Accounts & access" : trainerMode ? "People · Trainers" : "People · Members";
  const description = accessMode
    ? "Manage gym roles without blurring responsibilities across locations."
    : trainerMode
      ? "See coaching credentials, schedules, and payment readiness at a glance."
      : "Profiles, health notes, emergency contacts, and their membership history.";

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
    <div className="page-enter mx-auto w-full max-w-5xl space-y-4 pb-4">
      <p className="pt-1 text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">{eyebrow}</p>
      <FlexPageHeader
        title={title}
        subtitle={description}
        actions={
          trainerMode ? (
            canSetPay && (
              <FlexButton variant="lime" onClick={() => setPayModalOpen(true)}>
                <WalletCards className="size-4" /> Pay trainer
              </FlexButton>
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

function MemberMobileCard({
  member,
  index,
  subscription,
  pending,
  hasNoActivePlan,
  trainers,
  assigningTrainerFor,
  onAssignTrainer,
  onTakePayment,
  onPlan,
  onEdit,
  onNotify,
}: {
  member: Member;
  index: number;
  subscription?: MemberSubscription;
  pending?: MemberSubscription;
  hasNoActivePlan: boolean;
  trainers: Trainer[];
  assigningTrainerFor: number | null;
  onAssignTrainer: (trainerId: number | null) => void;
  onTakePayment: () => void;
  onPlan: () => void;
  onEdit: () => void;
  onNotify: () => void;
}) {
  return (
    <div
      className={`overflow-hidden rounded-2xl bg-white shadow-[0_2px_16px_rgba(20,20,16,0.06)] ${
        hasNoActivePlan ? "ring-1 ring-red-200" : ""
      }`}
    >
      <ExpandableRow
        summary={
          <div className="flex items-center gap-3">
            <div className="relative shrink-0">
              <span
                className={`grid size-10 place-items-center rounded-full text-[11px] font-bold ${index % 2 ? "bg-[#d7e4fd]" : "bg-[#f4cfbd]"}`}
              >
                {initials(member.firstName, member.lastName)}
              </span>
              {hasNoActivePlan && (
                <span className="absolute -bottom-1 -right-1 grid size-4 place-items-center rounded-full bg-red-600 text-white shadow">
                  <AlertTriangle className="size-2.5" />
                </span>
              )}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-stone-900">
                {member.firstName} {member.lastName}
              </p>
              <p className="mt-0.5 truncate text-xs text-stone-500">
                {subscription
                  ? `${subscription.planName} · ${subscription.planCurrency} ${subscription.planPrice}`
                  : pending
                    ? `${pending.planName} · awaiting payment`
                    : "No active plan"}
              </p>
            </div>
            {pending ? (
              <FlexPill tone="amber">Awaiting</FlexPill>
            ) : hasNoActivePlan ? (
              <FlexPill tone="amber">No plan</FlexPill>
            ) : null}
          </div>
        }
      >
        <div className="space-y-1">
          <DetailRow label="Contact">
            <p>{member.phone}</p>
            {member.email && <p className="text-stone-500">{member.email}</p>}
          </DetailRow>
          <DetailRow label="Plan">
            {subscription ? (
              <>
                <p className="font-semibold">{subscription.planName}</p>
                <p className="text-stone-500">
                  {subscription.planCurrency} {subscription.planPrice}
                </p>
              </>
            ) : pending ? (
              <>
                <p className="font-semibold">{pending.planName}</p>
                <FlexPill tone="amber">Awaiting payment</FlexPill>
              </>
            ) : (
              <FlexPill tone="amber">No active plan</FlexPill>
            )}
          </DetailRow>
          <DetailRow label="Trainer">
            <select
              value={member.trainerId ?? ""}
              disabled={assigningTrainerFor === member.id}
              onChange={(e) => onAssignTrainer(e.target.value ? Number(e.target.value) : null)}
              className="h-9 max-w-[150px] rounded-full border border-stone-200 bg-white px-3 text-xs outline-none transition focus:border-stone-900 disabled:opacity-50"
            >
              <option value="">No trainer</option>
              {trainers.map((trainer) => (
                <option key={trainer.id} value={trainer.id}>
                  {trainer.firstName} {trainer.lastName}
                </option>
              ))}
            </select>
          </DetailRow>
          <DetailRow label="Next payment">
            {subscription && subscription.currentPeriodStart && subscription.currentPeriodEnd ? (
              <div className="flex flex-col items-end gap-1">
                <span className="text-stone-500">{subscription.currentPeriodEnd}</span>
                <PaymentCountdownBar
                  periodStart={subscription.currentPeriodStart}
                  periodEnd={subscription.currentPeriodEnd}
                />
              </div>
            ) : (
              <span className="text-stone-500">—</span>
            )}
          </DetailRow>
          <DetailRow label="Joined">{member.joinDate}</DetailRow>
          <DetailRow label="Waiver">
            <FlexPill tone={member.waiverAccepted ? "lime" : "amber"}>
              {member.waiverAccepted ? "Accepted" : "Missing"}
            </FlexPill>
          </DetailRow>
          <div className="flex flex-wrap items-center gap-2 pt-3">
            {pending && <RowAction onClick={onTakePayment}>Take payment</RowAction>}
            <RowAction onClick={onPlan}>Plan</RowAction>
            <RowAction onClick={onEdit}>Edit</RowAction>
            <button
              type="button"
              className="grid size-9 shrink-0 place-items-center rounded-2xl bg-black/[0.07] text-stone-800 transition hover:bg-black/[0.12]"
              aria-label={`Notify ${member.firstName} ${member.lastName}`}
              onClick={onNotify}
            >
              <BellRing className="size-4" />
            </button>
            {member.phone && (
              <a
                href={`tel:${member.phone}`}
                className="grid size-9 shrink-0 place-items-center rounded-2xl border border-red-200 bg-red-50 text-red-700"
                aria-label={`Call ${member.firstName}`}
              >
                <Phone className="size-3.5" />
              </a>
            )}
            {member.phone && (
              <a
                href={`https://wa.me/${member.phone.replace(/\D/g, "")}`}
                target="_blank"
                rel="noopener noreferrer"
                className="grid size-9 shrink-0 place-items-center rounded-2xl border border-emerald-200 bg-emerald-50 text-emerald-700"
                aria-label={`WhatsApp ${member.firstName}`}
              >
                <MessageCircle className="size-3.5" />
              </a>
            )}
            {member.email && (
              <a
                href={`mailto:${member.email}`}
                className="grid size-9 shrink-0 place-items-center rounded-2xl border border-red-200 bg-red-50 text-red-700"
                aria-label={`Email ${member.firstName}`}
              >
                <Mail className="size-3.5" />
              </a>
            )}
          </div>
        </div>
      </ExpandableRow>
    </div>
  );
}

function MembersPanel() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [members, setMembers] = useState<Member[]>([]);
  const [subscriptionsByMember, setSubscriptionsByMember] = useState<Record<number, MemberSubscription>>({});
  const [pendingByMember, setPendingByMember] = useState<Record<number, MemberSubscription>>({});
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [selectedPlanFilter, setSelectedPlanFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const PAGE_SIZE = 10;
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
      listMembershipPlans(gymId),
    ])
      .then(([memberData, active, pending, gymData, trainerData, planData]) => {
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
        setPlans(planData);
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
    const list = members.filter((m) => {
      const matchesSearch = !q || `${m.firstName} ${m.lastName} ${m.email ?? ""} ${m.phone}`.toLowerCase().includes(q);
      if (!matchesSearch) return false;

      const sub = subscriptionsByMember[m.id];
      const pending = pendingByMember[m.id];

      if (selectedPlanFilter === "ALL") return true;
      if (selectedPlanFilter === "NO_PLAN") return !sub && !pending;
      return sub?.planName === selectedPlanFilter || pending?.planName === selectedPlanFilter;
    });

    return [...list].sort((a, b) => {
      const subA = subscriptionsByMember[a.id];
      const subB = subscriptionsByMember[b.id];

      const expiryA = subA?.currentPeriodEnd ?? null;
      const expiryB = subB?.currentPeriodEnd ?? null;

      if (expiryA && expiryB) return expiryA.localeCompare(expiryB);
      if (expiryA) return -1;
      if (expiryB) return 1;
      return `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`);
    });
  }, [members, query, selectedPlanFilter, subscriptionsByMember, pendingByMember]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const startIndex = (currentPage - 1) * PAGE_SIZE;
  const paginatedMembers = filtered.slice(startIndex, startIndex + PAGE_SIZE);

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
      <FlexEmptyState title="This account isn't linked to a gym, so there's no member directory to show." />
    );
  }

  return (
    <section className="overflow-hidden rounded-[1.75rem] bg-white shadow-[0_2px_16px_rgba(20,20,16,0.06)]">
      <div className="flex flex-col gap-4 border-b border-stone-100 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Member directory</p>
          <h2 className="mt-1 text-lg font-black tracking-tight text-stone-900">
            {loading ? "Loading members…" : `${filtered.length} member${filtered.length === 1 ? "" : "s"}`}
          </h2>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <label className="flex h-10 items-center gap-2 rounded-full bg-stone-100 px-4 text-stone-500">
            <Search className="size-4" />
            <input
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setCurrentPage(1);
              }}
              className="w-32 bg-transparent text-sm text-stone-900 outline-none placeholder:text-stone-400"
              placeholder="Find a member"
            />
          </label>
          <select
            value={selectedPlanFilter}
            onChange={(e) => {
              setSelectedPlanFilter(e.target.value);
              setCurrentPage(1);
            }}
            className="h-10 rounded-full border border-stone-200 bg-white px-4 text-xs font-bold text-stone-800 outline-none transition focus:border-stone-900"
          >
            <option value="ALL">All plans</option>
            <option value="NO_PLAN">No active plan</option>
            {plans.map((p) => (
              <option key={p.id} value={p.name}>
                {p.name}
              </option>
            ))}
          </select>
          <FlexButton variant="lime" onClick={() => setModalState("create")}>
            <Plus className="size-4" /> Add member
          </FlexButton>
        </div>
      </div>

      {error && (
        <div className="px-5 pt-4">
          <ErrorBanner message={error} />
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <div className="p-5">
          <FlexEmptyState
            title={members.length === 0 ? "No members yet — add your first one." : "No members match your search."}
          />
        </div>
      )}

      {filtered.length > 0 && (
        <div className="hidden overflow-x-auto md:block">
          <table className="w-full min-w-[1120px] text-left">
            <thead className="border-b border-stone-100 bg-stone-50/60">
              <tr className="text-[10px] uppercase tracking-[0.12em] text-stone-500">
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
            <tbody className="divide-y divide-stone-100">
              {paginatedMembers.map((member, index) => {
                const subscription = subscriptionsByMember[member.id];
                const pending = pendingByMember[member.id];
                const hasNoActivePlan = !subscription;
                return (
                <tr className={`transition ${hasNoActivePlan ? "bg-[#ffe3e3]/40 hover:bg-[#ffe3e3]/70" : "hover:bg-stone-50"}`} key={member.id}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <div className="relative shrink-0">
                        <span
                          className={`grid size-9 place-items-center rounded-full text-[10px] font-bold ${index % 2 ? "bg-[#d7e4fd]" : "bg-[#f4cfbd]"}`}
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
                        <div className="flex flex-wrap items-center gap-1.5">
                          <p className="text-sm font-bold text-stone-900">
                            {member.firstName} {member.lastName}
                          </p>
                          {hasNoActivePlan && (
                            <FlexPill tone="red">
                              <AlertTriangle className="size-2.5" /> No active plan
                            </FlexPill>
                          )}
                        </div>
                        {member.gender && <p className="mt-0.5 text-[11px] text-stone-500">{member.gender}</p>}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-4 text-xs text-stone-500">
                    <p>{member.phone}</p>
                    {member.email && <p className="mt-0.5">{member.email}</p>}
                  </td>
                  <td className="px-4 py-4">
                    {subscription ? (
                      <>
                        <p className="text-sm font-semibold text-stone-900">{subscription.planName}</p>
                        <p className="mt-0.5 text-[11px] text-stone-500">
                          {subscription.planCurrency} {subscription.planPrice}
                        </p>
                      </>
                    ) : pending ? (
                      <>
                        <p className="text-sm font-semibold text-stone-900">{pending.planName}</p>
                        <FlexPill tone="amber">Awaiting payment</FlexPill>
                      </>
                    ) : (
                      <div className="flex items-center gap-1.5">
                        <AlertTriangle className="size-4 shrink-0 text-red-600" />
                        <FlexPill tone="amber">No active plan</FlexPill>
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-4">
                    <select
                      value={member.trainerId ?? ""}
                      disabled={assigningTrainerFor === member.id}
                      onChange={(e) => handleAssignTrainer(member.id, e.target.value ? Number(e.target.value) : null)}
                      className="h-9 max-w-[140px] rounded-full border border-stone-200 bg-white px-3 text-xs text-stone-800 outline-none transition focus:border-stone-900 disabled:opacity-50"
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
                        <span className="text-xs text-stone-500">{subscription.currentPeriodEnd}</span>
                        <PaymentCountdownBar
                          periodStart={subscription.currentPeriodStart}
                          periodEnd={subscription.currentPeriodEnd}
                        />
                      </div>
                    ) : (
                      <span className="text-xs text-stone-500">—</span>
                    )}
                  </td>
                  <td className="px-4 py-4 text-xs text-stone-500">{member.joinDate}</td>
                  <td className="px-4 py-4">
                    <FlexPill tone={member.waiverAccepted ? "lime" : "amber"}>
                      {member.waiverAccepted ? "Accepted" : "Missing"}
                    </FlexPill>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center justify-end gap-2">
                      {member.phone && (
                        <a
                          href={`tel:${member.phone}`}
                          className="grid size-9 shrink-0 place-items-center rounded-2xl border border-red-200 bg-red-50 text-red-700 transition hover:bg-red-100"
                          title={`Call ${member.firstName} (${member.phone})`}
                        >
                          <Phone className="size-3.5" />
                        </a>
                      )}
                      {member.phone && (
                        <a
                          href={`https://wa.me/${member.phone.replace(/\D/g, "")}?text=${encodeURIComponent(`Hi ${member.firstName}, we missed seeing you at the gym! Let us know if you need help renewing your membership.`)}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="grid size-9 shrink-0 place-items-center rounded-2xl border border-emerald-200 bg-emerald-50 text-emerald-700 transition hover:bg-emerald-100"
                          title={`WhatsApp ${member.firstName} (${member.phone})`}
                        >
                          <MessageCircle className="size-3.5" />
                        </a>
                      )}
                      {member.email && (
                        <a
                          href={`mailto:${member.email}?subject=We%20miss%20you%20at%20the%20gym!`}
                          className="grid size-9 shrink-0 place-items-center rounded-2xl border border-red-200 bg-red-50 text-red-700 transition hover:bg-red-100"
                          title={`Email ${member.firstName} (${member.email})`}
                        >
                          <Mail className="size-3.5" />
                        </a>
                      )}
                      {pending && (
                        <RowAction onClick={() => setTakePaymentMember(member)}>Take payment</RowAction>
                      )}
                      <RowAction onClick={() => setPlanModalMember(member)}>Plan</RowAction>
                      <RowAction onClick={() => setModalState(member)}>Edit</RowAction>
                      <button
                        type="button"
                        className="grid size-9 shrink-0 place-items-center rounded-2xl bg-black/[0.07] text-stone-800 transition hover:bg-black/[0.12]"
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

      {filtered.length > 0 && (
        <div className="space-y-3 p-4 md:hidden">
          {paginatedMembers.map((member, index) => {
            const subscription = subscriptionsByMember[member.id];
            const pending = pendingByMember[member.id];
            const hasNoActivePlan = !subscription;
            return (
              <MemberMobileCard
                key={member.id}
                member={member}
                index={index}
                subscription={subscription}
                pending={pending}
                hasNoActivePlan={hasNoActivePlan}
                trainers={trainers}
                assigningTrainerFor={assigningTrainerFor}
                onAssignTrainer={(trainerId) => handleAssignTrainer(member.id, trainerId)}
                onTakePayment={() => setTakePaymentMember(member)}
                onPlan={() => setPlanModalMember(member)}
                onEdit={() => setModalState(member)}
                onNotify={() => setNotifyMemberTarget(member)}
              />
            );
          })}
        </div>
      )}

      {filtered.length > 0 && (
        <div className="flex flex-col gap-3 border-t border-stone-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-stone-500">
            Showing <span className="font-bold text-stone-900">{startIndex + 1}</span> to{" "}
            <span className="font-bold text-stone-900">{Math.min(startIndex + PAGE_SIZE, filtered.length)}</span> of{" "}
            <span className="font-bold text-stone-900">{filtered.length}</span> members
          </p>

          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              className="flex h-9 items-center gap-1 rounded-full bg-stone-100 px-4 text-xs font-bold text-stone-700 transition hover:bg-stone-200 disabled:opacity-40"
            >
              <ChevronLeft className="size-4" /> Previous
            </button>
            <span className="mono text-xs font-semibold text-stone-500">
              Page {currentPage} of {totalPages}
            </span>
            <button
              type="button"
              disabled={currentPage >= totalPages}
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              className="flex h-9 items-center gap-1 rounded-full bg-stone-100 px-4 text-xs font-bold text-stone-700 transition hover:bg-stone-200 disabled:opacity-40"
            >
              Next <ChevronRight className="size-4" />
            </button>
          </div>
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
      <div className="overflow-hidden rounded-[1.75rem] bg-white shadow-[0_2px_16px_rgba(20,20,16,0.06)] lg:col-span-8">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 p-5">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Trainer roster</p>
            <h2 className="mt-1 text-lg font-black tracking-tight text-stone-900">
              {loading ? "Loading trainers…" : `${trainers.length} coach${trainers.length === 1 ? "" : "es"}`}
            </h2>
          </div>
          {canManage && (
            <FlexButton variant="lime" onClick={() => setModalState("create")}>
              <Plus className="size-4" /> Add trainer
            </FlexButton>
          )}
        </div>

        {error && (
          <div className="p-4">
            <ErrorBanner message={error} />
          </div>
        )}

        {!loading && trainers.length === 0 && (
          <div className="p-5">
            <FlexEmptyState
              title="No trainers yet."
              hint={canManage ? "Add your first coach to build out the roster." : undefined}
            />
          </div>
        )}

        <div className="divide-y divide-stone-100">
          {loading &&
            [0, 1].map((i) => (
              <div className="flex items-center gap-4 p-5" key={i}>
                <div className="size-14 shrink-0 animate-pulse rounded-2xl bg-stone-100" />
                <div className="h-3 w-1/3 animate-pulse rounded-full bg-stone-100" />
              </div>
            ))}
          {trainers.map((trainer, index) => (
            <div className="flex flex-wrap items-center gap-4 p-5" key={trainer.id}>
              {trainer.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element -- gym-provided URL, not a static asset
                <img src={trainer.imageUrl} alt={`${trainer.firstName} ${trainer.lastName}`} className="size-14 shrink-0 rounded-2xl object-cover" />
              ) : (
                <div className={`grid size-14 shrink-0 place-items-center rounded-2xl text-sm font-bold text-stone-900 ${index % 2 ? "bg-[#d6e1fe]" : "bg-[#f5dd9f]"}`}>
                  {trainer.firstName[0]}
                  {trainer.lastName[0]}
                </div>
              )}
              <div className="min-w-0 flex-1 basis-40">
                <p className="font-bold text-stone-900">
                  {trainer.firstName} {trainer.lastName}
                </p>
                <p className="mt-1 text-xs text-stone-500">{trainer.specialization || "General coaching"}</p>
                <div className="mt-2 flex flex-wrap items-center gap-x-2 text-[11px] text-stone-500">
                  <span>{trainer.phone}</span>
                  {trainer.email && <span>· {trainer.email}</span>}
                </div>
              </div>
              {canManage && (
                <div className="flex shrink-0 flex-wrap items-center gap-2">
                  <RowAction onClick={() => setModalState(trainer)}>Edit</RowAction>
                  <DangerRowAction onClick={() => setRemovingTrainer(trainer)}>Remove</DangerRowAction>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <FlexCard className="lg:col-span-4">
        <FlexIconBadge tone="lime">
          <Award className="size-5" />
        </FlexIconBadge>
        <p className="mt-4 text-lg font-black leading-snug tracking-tight text-stone-900">
          Every member
          <br />
          picks their coach.
        </p>
        <p className="mt-3 text-sm leading-relaxed text-stone-500">
          Members choose a trainer from their own dashboard, or you can assign one directly from the member
          directory. {canManage ? "You can add, edit, or remove trainers here." : "Ask an owner or a manager with trainer access to change the roster."}
        </p>
      </FlexCard>

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
      <div className="overflow-hidden rounded-[1.75rem] bg-white shadow-[0_2px_16px_rgba(20,20,16,0.06)] xl:col-span-8">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 p-5">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">Role directory</p>
            <h2 className="mt-1 text-lg font-black tracking-tight text-stone-900">Access follows the gym</h2>
          </div>
          {isOwner && (
            <FlexButton variant="lime" onClick={() => setModalState("create")}>
              <UserRoundPlus className="size-4" /> Add manager
            </FlexButton>
          )}
        </div>

        {error && (
          <div className="p-4">
            <ErrorBanner message={error} />
          </div>
        )}

        <div className="divide-y divide-stone-100">
          {currentUser && (
            <div className="flex flex-wrap items-center gap-3 p-4">
              <div className="grid size-10 shrink-0 place-items-center rounded-full bg-[#c7f36a] text-[10px] font-bold text-stone-900">
                {initials(currentUser.displayName.split(" ")[0] ?? "", currentUser.displayName.split(" ").slice(1).join(" ") || "")}
              </div>
              <div className="min-w-0 flex-1 basis-40">
                <p className="text-sm font-bold text-stone-900">
                  {currentUser.displayName} <span className="font-normal text-stone-400">(you)</span>
                </p>
                <p className="mt-1 text-xs text-stone-500">{currentUser.role === "OWNER" ? "Owner" : currentUser.role}</p>
              </div>
              <FlexPill tone="lime">Full access</FlexPill>
            </div>
          )}

          {loading &&
            [0, 1].map((i) => (
              <div className="flex items-center gap-3 p-4" key={i}>
                <div className="size-10 shrink-0 animate-pulse rounded-full bg-stone-100" />
                <div className="h-3 w-1/3 animate-pulse rounded-full bg-stone-100" />
              </div>
            ))}

          {!loading && managers.length === 0 && (
            <div className="p-5">
              <FlexEmptyState
                title="No managers yet."
                hint={isOwner ? "Add one by email to share ownership of daily operations." : undefined}
              />
            </div>
          )}

          {managers.map((manager) => (
            <div className="flex flex-wrap items-center gap-3 p-4" key={manager.id}>
              <div className="grid size-10 shrink-0 place-items-center rounded-full bg-stone-100 text-[10px] font-bold text-stone-700">
                {initials(manager.firstName, manager.lastName)}
              </div>
              <div className="min-w-0 flex-1 basis-40">
                <p className="text-sm font-bold text-stone-900">
                  {manager.firstName} {manager.lastName}
                </p>
                <p className="mt-1 truncate text-xs text-stone-500">{manager.email}</p>
              </div>
              <FlexPill tone="stone">{scopeSummary(manager.scopes)}</FlexPill>
              {isOwner && (
                <div className="flex shrink-0 items-center gap-2">
                  <RowAction onClick={() => setModalState(manager)}>Edit</RowAction>
                  <DangerRowAction onClick={() => setRemovingManager(manager)}>Remove</DangerRowAction>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <FlexCard className="xl:col-span-4">
        <FlexIconBadge tone="lime">
          <ShieldCheck className="size-5" />
        </FlexIconBadge>
        <p className="mt-4 text-lg font-black leading-snug tracking-tight text-stone-900">
          One person,
          <br />
          more than one role.
        </p>
        <p className="mt-3 text-sm leading-relaxed text-stone-500">
          Managers use the same dashboard as you, scoped to your gym. Every manager can handle members, trainers,
          and attendance — grant finance or settings access only where you want it.
        </p>
      </FlexCard>

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
