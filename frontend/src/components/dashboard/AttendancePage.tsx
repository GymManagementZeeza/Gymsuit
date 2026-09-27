"use client";

/* Attendance page: live floor occupancy and entry-control hardware, in the FLEX light design language. */
import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { usePathname } from "next/navigation";
import {
  FlexButton,
  FlexCard,
  FlexEmptyState,
  FlexIconBadge,
  FlexPageHeader,
  FlexPill,
  FlexStatCard,
} from "@/components/dashboard/ui";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listActiveCheckIns, forceCheckOut, type CheckIn } from "@/lib/checkins";
import {
  Activity,
  ChevronRight,
  CircleCheck,
  Clock3,
  DoorOpen,
  Fingerprint,
  LogIn,
  ScanFace,
  ShieldCheck,
  Wifi,
} from "lucide-react";

const microLabel = "text-[11px] font-bold uppercase tracking-[0.12em] text-stone-400";

const recognitionEvents = [
  { name: "Maya Patel", time: "08:42:19", method: "Face recognized", result: "Access granted", tone: "lime" as const },
  { name: "Camille Hart", time: "08:17:04", method: "Fingerprint verified", result: "Access granted", tone: "blue" as const },
  { name: "Elliot Barnes", time: "07:53:42", method: "Face recognized", result: "Access granted", tone: "lime" as const },
];

function initialsFor(member: Member) {
  return `${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase();
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

function formatDuration(iso: string) {
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 60000));
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  return hours > 0 ? `${hours}h ${String(mins).padStart(2, "0")}m` : `${mins}m`;
}

export default function AttendancePage() {
  const pathname = usePathname();
  const live = pathname === "/dashboard/live-floor";

  if (!live) {
    return <EntryControl />;
  }

  return (
    <div className="page-enter space-y-5 pb-4">
      <div>
        <p className={microLabel}>Operations · Live floor</p>
        <FlexPageHeader
          title="Know who's in, now."
          subtitle="A front-desk view of members and staff currently on the floor."
          actions={
            <FlexButton>
              <LogIn className="size-4" /> Front desk check-in
            </FlexButton>
          }
        />
      </div>
      <LiveFloor />
    </div>
  );
}

function useCheckInFeed() {
  const session = useSession();
  const [checkIns, setCheckIns] = useState<CheckIn[]>([]);
  const [members, setMembers] = useState<Map<number, Member>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const [checkingOutId, setCheckingOutId] = useState<number | null>(null);

  const load = () => {
    if (!session?.gymId) return;
    setLoading(true);
    Promise.all([listActiveCheckIns(session.gymId), listMembers(session.gymId)])
      .then(([activeCheckIns, memberList]) => {
        setCheckIns(activeCheckIns);
        setMembers(new Map(memberList.map((m) => [m.id, m])));
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load attendance data."))
      .finally(() => setLoading(false));
  };

  useEffect(load, [session?.gymId]);

  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(interval);
  }, []);

  const handleCheckOut = async (checkInId: number) => {
    if (!session?.gymId) return;
    setCheckingOutId(checkInId);
    try {
      await forceCheckOut(session.gymId, checkInId);
      setCheckIns((prev) => prev.filter((c) => c.id !== checkInId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not check that member out.");
    } finally {
      setCheckingOutId(null);
    }
  };

  return { checkIns, members, loading, error, now, checkingOutId, handleCheckOut };
}

function CheckOutButton({ busy, onCheckOut }: { busy: boolean; onCheckOut: () => void }) {
  return (
    <FlexButton
      variant="ghost"
      onClick={busy ? undefined : onCheckOut}
      className={`shrink-0 px-3 py-1.5 text-[11px] ${busy ? "pointer-events-none opacity-50" : ""}`}
    >
      {busy ? "Checking out…" : "Check out"}
    </FlexButton>
  );
}

function LiveFloor() {
  const { checkIns, members, loading, error, now, checkingOutId, handleCheckOut } = useCheckInFeed();
  const lastUpdateLabel = new Date(now).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });

  return (
    <section className="grid gap-4 xl:grid-cols-12">
      <div className="xl:col-span-5">
        <FlexStatCard
          label="North Loop · Live"
          value={loading ? "…" : String(checkIns.length)}
          detail={loading ? "Loading…" : `member${checkIns.length === 1 ? "" : "s"} are training right now`}
          icon={<Activity className="size-5" />}
          variant="lime"
        />
      </div>
      <FlexCard className="overflow-hidden p-0 xl:col-span-7">
        <div className="flex items-center justify-between gap-3 p-5">
          <div>
            <p className={microLabel}>Currently in gym</p>
            <h2 className="mt-1.5 text-lg font-black tracking-tight text-stone-900">
              {loading ? "…" : `${checkIns.length} checked in`}
            </h2>
          </div>
          <FlexPill tone="lime">{`Last update ${lastUpdateLabel}`}</FlexPill>
        </div>

        {error && <p className="border-t border-stone-100 px-5 py-3 text-sm font-medium text-red-600">{error}</p>}

        {!loading && !error && checkIns.length === 0 && (
          <div className="px-5 pb-5">
            <FlexEmptyState title="Nobody is checked in right now." />
          </div>
        )}

        <div className="divide-y divide-stone-100 border-t border-stone-100">
          {loading &&
            [0, 1, 2].map((i) => (
              <div className="flex items-center gap-3 px-5 py-4" key={i}>
                <div className="size-9 shrink-0 animate-pulse rounded-full bg-stone-200" />
                <div className="h-3 w-2/3 animate-pulse rounded-full bg-stone-200" />
              </div>
            ))}
          {!loading &&
            checkIns.map((entry, index) => {
              const member = members.get(entry.memberId);
              const name = member ? `${member.firstName} ${member.lastName}` : `Member #${entry.memberId}`;
              const initials = member ? initialsFor(member) : "—";
              return (
                <div className="flex items-center gap-3 px-5 py-4" key={entry.id}>
                  <span className={`grid size-9 shrink-0 place-items-center rounded-full text-[10px] font-bold text-stone-800 ${index % 2 ? "bg-[#d7e3fc]" : "bg-[#f4ceb9]"}`}>
                    {initials}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold text-stone-900">{name}</p>
                    <p className="mt-1 text-[11px] text-stone-500">Checked in {formatTime(entry.checkInTime)}</p>
                  </div>
                  <p className="mono hidden text-[10px] text-stone-400 sm:block">{formatDuration(entry.checkInTime)}</p>
                  <CheckOutButton busy={checkingOutId === entry.id} onCheckOut={() => handleCheckOut(entry.id)} />
                </div>
              );
            })}
        </div>
      </FlexCard>
    </section>
  );
}

function EntryControl() {
  const { checkIns, members, loading, error, now, checkingOutId, handleCheckOut } = useCheckInFeed();

  const today = useMemo(
    () => new Intl.DateTimeFormat(undefined, { weekday: "long", month: "long", day: "numeric" }).format(new Date(now)),
    [now]
  );
  const lastUpdate = useMemo(
    () => new Date(now).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" }),
    [now]
  );

  return (
    <div className="page-enter space-y-5 pb-4">
      <div className="rounded-[1.75rem] bg-[#ffe3e3] p-5 text-sm font-semibold leading-relaxed text-[#7f1d1d]">
        Automatic unlock system is not installed in this gym. Contact CyberFix Solutions for installation — prices
        are given below.
      </div>

      <div>
        <p className={microLabel}>Hardware · Attendance</p>
        <FlexPageHeader
          title="Gym attendance, controlled at the entry."
          subtitle="Monitor member access, live occupancy, and biometric hardware from one front-desk screen."
          actions={
            <>
              <FlexButton variant="ghost" className="hidden sm:inline-flex">
                View daily report
              </FlexButton>
              <FlexButton>
                <DoorOpen className="size-4" /> Manual check-in
              </FlexButton>
            </>
          }
        />
      </div>

      <section className="grid gap-4 xl:grid-cols-12">
        <FlexCard className="xl:col-span-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className={microLabel}>Live facility status</p>
              <p className="mt-3 text-5xl font-black tracking-tight text-stone-900">{loading ? "…" : checkIns.length}</p>
              <p className="mt-1 text-sm font-medium text-stone-500">members currently training</p>
            </div>
            <FlexPill tone="lime">All systems online</FlexPill>
          </div>

          <div className="mt-5 grid grid-cols-3 divide-x divide-stone-200 rounded-2xl bg-stone-50 px-4 py-3">
            <div className="pr-3">
              <p className="text-lg font-black text-stone-900">{loading ? "…" : checkIns.length}</p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.12em] text-stone-400">On floor</p>
            </div>
            <div className="px-3">
              <p className="text-lg font-black text-stone-900">03</p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.12em] text-stone-400">Guests</p>
            </div>
            <div className="pl-3">
              <p className="text-lg font-black text-stone-900">0</p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.12em] text-stone-400">Alerts</p>
            </div>
          </div>
        </FlexCard>

        <FlexCard className="xl:col-span-7">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className={microLabel}>Entry lane A</p>
              <h2 className="mt-1.5 text-lg font-black tracking-tight text-stone-900">Biometric check-in</h2>
              <p className="mt-1 text-xs font-medium leading-relaxed text-stone-500">
                Member verification is ready at the main entry lane.
              </p>
            </div>
            <FlexIconBadge tone="lime">
              <CircleCheck className="size-5" />
            </FlexIconBadge>
          </div>

          <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="flex flex-1 items-center gap-4 rounded-2xl bg-stone-50 p-4">
              <FlexIconBadge tone="neutral">
                <ScanFace className="size-5" />
              </FlexIconBadge>
              <div className="min-w-0">
                <p className="text-sm font-bold text-stone-900">Ready to scan</p>
                <p className="mt-1 text-[11px] text-stone-500">Face recognition and fingerprint verification enabled</p>
              </div>
            </div>
            <FlexButton className="justify-center">
              <DoorOpen className="size-4" /> Open lane
            </FlexButton>
          </div>

          <div className="mt-5 flex flex-wrap gap-x-5 gap-y-2 border-t border-stone-100 pt-4 text-[11px] font-bold text-stone-500">
            <span className="inline-flex items-center gap-2">
              <Wifi className="size-3.5 text-green-600" /> Network connected
            </span>
            <span className="inline-flex items-center gap-2">
              <ShieldCheck className="size-3.5 text-green-600" /> Access policy synced
            </span>
            <span className="inline-flex items-center gap-2">
              <Clock3 className="size-3.5 text-green-600" /> Updated {lastUpdate}
            </span>
          </div>
        </FlexCard>
      </section>

      <section className="grid gap-4 xl:grid-cols-12">
        <FlexCard className="overflow-hidden p-0 xl:col-span-8">
          <div className="flex flex-wrap items-center justify-between gap-3 p-5">
            <div>
              <p className={microLabel}>Live attendance</p>
              <h2 className="mt-1.5 text-lg font-black tracking-tight text-stone-900">Currently in the gym</h2>
            </div>
            <FlexPill tone="lime">{`Updated ${lastUpdate}`}</FlexPill>
          </div>

          {error && (
            <p className="border-t border-stone-100 bg-[#ffe3e3] px-5 py-3 text-sm font-medium text-[#7f1d1d]">{error}</p>
          )}

          {!loading && !error && checkIns.length === 0 && (
            <div className="px-5 pb-5">
              <FlexEmptyState title="The gym floor is clear." hint="New check-ins will appear here in real time." />
            </div>
          )}

          <div className="divide-y divide-stone-100 border-t border-stone-100">
            {loading &&
              [0, 1, 2, 3].map((index) => (
                <div className="flex items-center gap-3 px-5 py-4" key={index}>
                  <div className="size-9 shrink-0 animate-pulse rounded-full bg-stone-200" />
                  <div className="h-3 w-1/2 animate-pulse rounded-full bg-stone-200" />
                </div>
              ))}

            {!loading &&
              checkIns.slice(0, 5).map((entry, index) => {
                const member = members.get(entry.memberId);
                const name = member ? `${member.firstName} ${member.lastName}` : `Member #${entry.memberId}`;
                const initials = member ? initialsFor(member) : "—";
                return (
                  <div className="flex items-center gap-3 px-5 py-4" key={entry.id}>
                    <span className={`grid size-9 shrink-0 place-items-center rounded-full text-[10px] font-bold text-stone-800 ${index % 2 ? "bg-[#d7e3fc]" : "bg-[#f4ceb9]"}`}>
                      {initials}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold text-stone-900">{name}</p>
                      <p className="mt-1 text-[11px] text-stone-500">Entered at {formatTime(entry.checkInTime)} · Main entry</p>
                    </div>
                    <div className="hidden text-right sm:block">
                      <p className="mono text-[11px] font-bold text-stone-700">{formatDuration(entry.checkInTime)}</p>
                      <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.1em] text-stone-400">on floor</p>
                    </div>
                    <CheckOutButton busy={checkingOutId === entry.id} onCheckOut={() => handleCheckOut(entry.id)} />
                  </div>
                );
              })}
          </div>
        </FlexCard>

        <FlexCard className="overflow-hidden p-0 xl:col-span-4">
          <div className="p-5">
            <p className={microLabel}>Recognition feed</p>
            <h2 className="mt-1.5 text-lg font-black tracking-tight text-stone-900">Latest entry activity</h2>
          </div>
          <div className="divide-y divide-stone-100 border-t border-stone-100">
            {recognitionEvents.map((event) => (
              <div className="flex gap-3 px-5 py-4" key={`${event.name}-${event.time}`}>
                <FlexIconBadge tone={event.tone === "lime" ? "lime" : "neutral"} className="size-8 rounded-full">
                  {event.method.startsWith("Face") ? <ScanFace className="size-4" /> : <Fingerprint className="size-4" />}
                </FlexIconBadge>
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-2">
                    <p className="truncate text-sm font-bold text-stone-900">{event.name}</p>
                    <p className="mono shrink-0 text-[10px] text-stone-400">{event.time}</p>
                  </div>
                  <p className="mt-1 text-[11px] text-stone-500">{event.method}</p>
                  <p className="mt-2 text-[10px] font-bold uppercase tracking-[0.12em] text-green-700">{event.result}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="border-t border-stone-100 p-4">
            <FlexButton variant="ghost" className="w-full justify-between">
              Open complete event log <ChevronRight className="size-4" />
            </FlexButton>
          </div>
        </FlexCard>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <HardwareCard
          eyebrow="Identity reader"
          title="Hikvision MinMoe biometric terminal"
          description="Face recognition plus fingerprint verification keeps member entry touch-light, quick, and auditable."
          imageSrc="/hikvision-fingerprint-face-terminal.png"
          imageAlt="Hikvision biometric face and fingerprint recognition terminal"
          icon={<Fingerprint className="size-4" />}
          price="INR 17,000"
          details={["Face + fingerprint enabled", `Last health check: ${lastUpdate}`, "Reader firmware: current"]}
        />
        <HardwareCard
          eyebrow="Physical entry"
          title="Hikvision tripod entry system"
          description="A controlled, bidirectional tripod lane pairs with the reader to verify every passage at the gym entrance."
          imageSrc="/hikvision-tripod-turnstile.png"
          imageAlt="Hikvision stainless-steel tripod turnstile"
          icon={<Activity className="size-4" />}
          price="INR 38,000"
          details={["Lane A: available", "Direction: entry + exit", "Passage status: normal"]}
        />
      </section>

      <p className="mono text-[10px] uppercase tracking-[0.14em] text-stone-400">{today} · Main entry control room</p>
    </div>
  );
}

type HardwareCardProps = {
  eyebrow: string;
  title: string;
  description: string;
  imageSrc: string;
  imageAlt: string;
  icon: React.ReactNode;
  price: string;
  details: string[];
};

function HardwareCard({ eyebrow, title, description, imageSrc, imageAlt, icon, price, details }: HardwareCardProps) {
  return (
    <FlexCard className="overflow-hidden">
      <div className="flex flex-col gap-5 sm:flex-row">
        <div className="relative flex shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-stone-50 p-5 sm:w-[42%]">
          <Image
            src={imageSrc}
            alt={imageAlt}
            width={420}
            height={420}
            className="h-auto max-h-[175px] w-full object-contain mix-blend-multiply"
          />
        </div>
        <div className="flex min-w-0 flex-1 flex-col justify-between">
          <div>
            <div className="flex items-start justify-between gap-3">
              <p className={microLabel}>{eyebrow}</p>
              <FlexPill tone="lime">{price}</FlexPill>
            </div>
            <h2 className="mt-1.5 text-lg font-black leading-tight tracking-tight text-stone-900">{title}</h2>
            <p className="mt-2 text-xs font-medium leading-relaxed text-stone-500">{description}</p>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {details.map((detail) => (
              <FlexPill key={detail} tone="stone" className="gap-1.5">
                {icon} {detail}
              </FlexPill>
            ))}
          </div>
        </div>
      </div>
    </FlexCard>
  );
}
