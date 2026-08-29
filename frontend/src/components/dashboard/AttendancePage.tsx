"use client";

/* Training Ledger page: Attendance operations join real-time floor visibility with entry-control hardware. */
import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, StatusPill, TableAction } from "@/components/dashboard/ui";
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
  Users,
  Wifi,
} from "lucide-react";

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
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow="Operations · Live floor"
        title="Know who's in, now."
        description="A front-desk view of members and staff currently on the floor."
        actions={<ActionButton icon={<LogIn className="size-4" />}>Front desk check-in</ActionButton>}
      />
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

function LiveFloor() {
  const { checkIns, members, loading, error, now, checkingOutId, handleCheckOut } = useCheckInFeed();
  const lastUpdateLabel = new Date(now).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });

  return (
    <section className="grid gap-4 xl:grid-cols-12">
      <div className="cut-corner relative min-h-[282px] overflow-hidden bg-[#24241f] p-6 text-white xl:col-span-5">
        <Image src="/gymflow-floor.jpg" alt="North Loop training floor" fill className="object-cover opacity-45" />
        <div className="absolute inset-0 bg-gradient-to-t from-[#171715] to-transparent" />
        <div className="relative flex h-full flex-col justify-between">
          <div>
            <p className="ledger-label !text-[#dbdbd4] before:!bg-[#c7f36a]">North Loop · Live</p>
            <p className="display-face mt-4 text-5xl">{loading ? "…" : checkIns.length}</p>
            <p className="mt-1 text-sm text-white/70">
              {loading ? "Loading…" : `member${checkIns.length === 1 ? "" : "s"} are training right now`}
            </p>
          </div>
        </div>
      </div>
      <div className="border border-[#d8d8d1] bg-white xl:col-span-7">
        <div className="flex items-center justify-between border-b border-[#e6e6e0] p-5">
          <div>
            <p className="ledger-label">Currently in gym</p>
            <h2 className="mt-2 text-xl font-bold">{loading ? "…" : `${checkIns.length} checked in`}</h2>
          </div>
          <StatusPill label={`Last update ${lastUpdateLabel}`} tone="blue" />
        </div>

        {error && <p className="p-4 text-sm text-red-600">{error}</p>}

        {!loading && !error && checkIns.length === 0 && (
          <p className="p-8 text-center text-sm text-[#76766f]">Nobody is checked in right now.</p>
        )}

        <div className="divide-y divide-[#ecece6]">
          {loading &&
            [0, 1, 2].map((i) => (
              <div className="flex items-center gap-3 p-4" key={i}>
                <div className="size-9 shrink-0 animate-pulse rounded-full bg-[#ededE7]" />
                <div className="h-3 w-2/3 animate-pulse rounded bg-[#ededE7]" />
              </div>
            ))}
          {!loading &&
            checkIns.map((entry, index) => {
              const member = members.get(entry.memberId);
              const name = member ? `${member.firstName} ${member.lastName}` : `Member #${entry.memberId}`;
              const initials = member ? initialsFor(member) : "—";
              return (
                <div className="flex items-center gap-3 p-4" key={entry.id}>
                  <span className={`grid size-9 place-items-center rounded-full text-[10px] font-bold ${index % 2 ? "bg-[#d7e3fc]" : "bg-[#f4ceb9]"}`}>
                    {initials}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold">{name}</p>
                    <p className="mt-1 text-[11px] text-[#74746d]">Checked in {formatTime(entry.checkInTime)}</p>
                  </div>
                  <p className="mono hidden text-[10px] text-[#777770] sm:block">{formatDuration(entry.checkInTime)}</p>
                  <TableAction onClick={() => handleCheckOut(entry.id)} disabled={checkingOutId === entry.id}>
                    {checkingOutId === entry.id ? "Checking out…" : "Check out"}
                  </TableAction>
                </div>
              );
            })}
        </div>
      </div>
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
      <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-600">
        Automatic unlock system is not installed in this gym. Contact CyberFix Solutions for installation — prices
        are given below.
      </div>

      <PageHeading
        eyebrow="Hardware · Attendance"
        title="Gym attendance, controlled at the entry."
        description="Monitor member access, live occupancy, and biometric hardware from one front-desk screen."
        actions={
          <div className="flex items-center gap-2">
            <button className="hidden border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:bg-[#fafaf6] sm:inline-flex">
              View daily report
            </button>
            <ActionButton icon={<DoorOpen className="size-4" />}>Manual check-in</ActionButton>
          </div>
        }
      />

      <section className="grid gap-4 xl:grid-cols-12">
        <div className="relative overflow-hidden bg-[#24241f] p-5 text-white xl:col-span-5 xl:min-h-[246px]">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_90%_10%,rgba(199,243,106,.24),transparent_29%),linear-gradient(135deg,#24241f_0%,#141412_100%)]" />
          <div className="relative flex h-full flex-col justify-between gap-8">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="ledger-label !text-[#d9dbd1] before:!bg-[#c7f36a]">Live facility status</p>
                <p className="display-face mt-4 text-5xl leading-none">{loading ? "…" : checkIns.length}</p>
                <p className="mt-2 text-sm text-white/70">members currently training</p>
              </div>
              <StatusPill label="All systems online" tone="lime" />
            </div>

            <div className="grid grid-cols-3 divide-x divide-white/15 border-y border-white/15 py-3">
              <div className="pr-3">
                <p className="mono text-lg font-bold">{loading ? "…" : checkIns.length}</p>
                <p className="mt-1 text-[10px] uppercase tracking-[0.12em] text-white/55">On floor</p>
              </div>
              <div className="px-3">
                <p className="mono text-lg font-bold">03</p>
                <p className="mt-1 text-[10px] uppercase tracking-[0.12em] text-white/55">Guests</p>
              </div>
              <div className="pl-3">
                <p className="mono text-lg font-bold">0</p>
                <p className="mt-1 text-[10px] uppercase tracking-[0.12em] text-white/55">Alerts</p>
              </div>
            </div>
          </div>
        </div>

        <div className="border border-[#d8d8d1] bg-white p-5 xl:col-span-7">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="ledger-label">Entry lane A</p>
              <h2 className="mt-2 text-xl font-bold">Biometric check-in</h2>
              <p className="mt-1 text-xs leading-relaxed text-[#6d6d66]">
                Member verification is ready at the main entry lane.
              </p>
            </div>
            <span className="grid size-9 shrink-0 place-items-center rounded-full bg-[#eff8d7] text-[#517117]">
              <CircleCheck className="size-5" />
            </span>
          </div>

          <div className="mt-5 grid gap-3 sm:grid-cols-[1fr_auto]">
            <div className="flex items-center gap-4 border border-[#e7e7e1] bg-[#fafaf6] p-3">
              <div className="grid size-10 shrink-0 place-items-center rounded-full bg-[#24241f] text-[#c7f36a]">
                <ScanFace className="size-5" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-bold">Ready to scan</p>
                <p className="mt-1 text-[11px] text-[#74746d]">Face recognition and fingerprint verification enabled</p>
              </div>
            </div>
            <button className="inline-flex items-center justify-center gap-2 bg-[#c7f36a] px-4 py-3 text-xs font-bold text-[#24241f] transition hover:bg-[#b8e55c]">
              <DoorOpen className="size-4" /> Open lane
            </button>
          </div>

          <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 border-t border-[#ecece6] pt-4 text-[11px] font-bold text-[#56564f]">
            <span className="inline-flex items-center gap-2">
              <Wifi className="size-3.5 text-[#67951c]" /> Network connected
            </span>
            <span className="inline-flex items-center gap-2">
              <ShieldCheck className="size-3.5 text-[#67951c]" /> Access policy synced
            </span>
            <span className="inline-flex items-center gap-2">
              <Clock3 className="size-3.5 text-[#67951c]" /> Updated {lastUpdate}
            </span>
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e6e6e0] p-5">
            <div>
              <p className="ledger-label">Live attendance</p>
              <h2 className="mt-2 text-xl font-bold">Currently in the gym</h2>
            </div>
            <StatusPill label={`Updated ${lastUpdate}`} tone="blue" />
          </div>

          {error && <p className="border-b border-red-100 bg-red-50 px-5 py-3 text-sm text-red-700">{error}</p>}

          {!loading && !error && checkIns.length === 0 && (
            <div className="flex min-h-48 flex-col items-center justify-center px-5 text-center">
              <Users className="size-7 text-[#a6a69f]" />
              <p className="mt-3 text-sm font-bold">The gym floor is clear.</p>
              <p className="mt-1 text-xs text-[#74746d]">New check-ins will appear here in real time.</p>
            </div>
          )}

          <div className="divide-y divide-[#ecece6]">
            {loading &&
              [0, 1, 2, 3].map((index) => (
                <div className="flex items-center gap-3 px-5 py-4" key={index}>
                  <div className="size-9 animate-pulse rounded-full bg-[#ededE7]" />
                  <div className="h-3 w-1/2 animate-pulse rounded bg-[#ededE7]" />
                </div>
              ))}

            {!loading &&
              checkIns.slice(0, 5).map((entry, index) => {
                const member = members.get(entry.memberId);
                const name = member ? `${member.firstName} ${member.lastName}` : `Member #${entry.memberId}`;
                const initials = member ? initialsFor(member) : "—";
                return (
                  <div className="flex items-center gap-3 px-5 py-4" key={entry.id}>
                    <span className={`grid size-9 shrink-0 place-items-center rounded-full text-[10px] font-bold ${index % 2 ? "bg-[#d7e3fc]" : "bg-[#f4ceb9]"}`}>
                      {initials}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold">{name}</p>
                      <p className="mt-1 text-[11px] text-[#74746d]">Entered at {formatTime(entry.checkInTime)} · Main entry</p>
                    </div>
                    <div className="hidden text-right sm:block">
                      <p className="mono text-[11px] font-bold">{formatDuration(entry.checkInTime)}</p>
                      <p className="mt-1 text-[10px] uppercase tracking-[.1em] text-[#83837c]">on floor</p>
                    </div>
                    <TableAction onClick={() => handleCheckOut(entry.id)} disabled={checkingOutId === entry.id}>
                      {checkingOutId === entry.id ? "Checking out…" : "Check out"}
                    </TableAction>
                  </div>
                );
              })}
          </div>
        </div>

        <aside className="border border-[#d8d8d1] bg-[#fafaf6] xl:col-span-4">
          <div className="border-b border-[#e2e2db] p-5">
            <p className="ledger-label">Recognition feed</p>
            <h2 className="mt-2 text-xl font-bold">Latest entry activity</h2>
          </div>
          <div className="divide-y divide-[#e7e7e0]">
            {recognitionEvents.map((event) => (
              <div className="flex gap-3 p-4" key={`${event.name}-${event.time}`}>
                <span
                  className={`mt-0.5 grid size-8 shrink-0 place-items-center rounded-full ${event.tone === "lime" ? "bg-[#ecf7ce] text-[#5d811a]" : "bg-[#e7efff] text-[#4569ae]"}`}
                >
                  {event.method.startsWith("Face") ? <ScanFace className="size-4" /> : <Fingerprint className="size-4" />}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-2">
                    <p className="truncate text-sm font-bold">{event.name}</p>
                    <p className="mono shrink-0 text-[10px] text-[#777770]">{event.time}</p>
                  </div>
                  <p className="mt-1 text-[11px] text-[#74746d]">{event.method}</p>
                  <p className="mt-2 text-[10px] font-bold uppercase tracking-[.12em] text-[#58801b]">{event.result}</p>
                </div>
              </div>
            ))}
          </div>
          <button className="flex w-full items-center justify-between border-t border-[#e2e2db] px-5 py-3 text-xs font-bold transition hover:bg-white">
            Open complete event log <ChevronRight className="size-3.5" />
          </button>
        </aside>
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

      <p className="mono text-[10px] uppercase tracking-[.14em] text-[#8a8a83]">{today} · Main entry control room</p>
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
    <article className="relative flex min-h-[220px] overflow-hidden border border-[#d8d8d1] bg-white">
      <div className="relative flex w-[42%] shrink-0 items-center justify-center overflow-hidden border-r border-[#e8e8e2] bg-[#f5f5f0] p-5">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(199,243,106,.22),transparent_58%)]" />
        <Image
          src={imageSrc}
          alt={imageAlt}
          width={420}
          height={420}
          className="relative h-auto max-h-[175px] w-full object-contain mix-blend-multiply"
        />
      </div>
      <div className="flex min-w-0 flex-1 flex-col justify-between p-5">
        <div>
          <div className="flex items-start justify-between gap-3">
            <p className="ledger-label">{eyebrow}</p>
            <span className="shrink-0 bg-[#eff8d7] px-2 py-1 text-xs font-bold text-[#517117]">{price}</span>
          </div>
          <h2 className="mt-2 text-lg font-bold leading-tight">{title}</h2>
          <p className="mt-3 text-xs leading-relaxed text-[#6d6d66]">{description}</p>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          {details.map((detail) => (
            <span
              className="inline-flex items-center gap-1.5 border border-[#e4e4de] bg-[#fafaf6] px-2 py-1.5 text-[10px] font-bold text-[#595952]"
              key={detail}
            >
              {icon} {detail}
            </span>
          ))}
        </div>
      </div>
    </article>
  );
}
