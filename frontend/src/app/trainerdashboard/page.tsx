/* Training Ledger page: Trainer command view — the coach's day, load, and client moments. */
import Image from "next/image";
import Link from "next/link";
import { Avatar, MetricCard, StatusPill } from "@/components/dashboard/ui";
import { ArrowUpRight, CalendarDays, Dumbbell, Users } from "lucide-react";

const weekLoad = [64, 87, 48, 76, 56, 31, 18];
const weekDays = ["M", "T", "W", "T", "F", "S", "S"];

const sessions = [
  { time: "09:00", client: "Alex Murphy", focus: "Upper body strength", duration: "60 min", dot: "bg-[#c7f36a]" },
  { time: "10:30", client: "Camille Hart", focus: "Progressive overload", duration: "60 min", dot: "bg-[#f4cfbd]" },
  { time: "13:00", client: "Noah Kim", focus: "Race pace intervals", duration: "45 min", dot: "bg-[#d7e4fd]" },
  { time: "17:30", client: "Noah Kim", focus: "Stride mechanics", duration: "45 min", dot: "bg-[#e3d9fa]" },
];

export default function TrainerOverview() {
  return (
    <div className="page-enter space-y-7">
      <section className="grid gap-4 lg:grid-cols-12">
        <div className="cut-corner relative min-h-[300px] overflow-hidden bg-[#24241f] text-white lg:col-span-7">
          <Image
            src="/studio-compass-coach.webp"
            alt="Trainer Jordan Cole"
            fill
            className="object-cover opacity-70"
            sizes="(min-width: 1024px) 620px, 100vw"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#161614] via-[#161614]/50 to-transparent" />
          <div className="relative flex h-full flex-col justify-end p-6">
            <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Your next session</p>
            <p className="display-face mt-3 text-4xl leading-[0.95]">
              Progress is
              <br />
              personal.
            </p>
            <div className="mt-6 flex items-center gap-3 bg-[#f6f6f0]/95 p-3 text-[#24241f]">
              <Avatar initials="CH" tone="peach" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-bold">Camille Hart</p>
                <p className="text-xs text-[#6e6e67]">10:30 · Strength program</p>
              </div>
              <Link href="/trainerdashboard/bookings" className="flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
                Open session <ArrowUpRight className="size-3" />
              </Link>
            </div>
          </div>
        </div>

        <div className="border border-[#d8d8d1] bg-white p-5 lg:col-span-5">
          <div className="flex items-start justify-between">
            <div>
              <p className="ledger-label">This week</p>
              <h3 className="mt-2 text-lg font-bold tracking-[-0.02em]">Training load</h3>
            </div>
            <span className="mono text-xs font-bold text-[#777770]">17 / 20</span>
          </div>
          <div className="mt-6 flex h-28 items-end gap-3">
            {weekLoad.map((height, index) => (
              <div key={index} className="flex flex-1 items-end self-stretch">
                <div
                  className={`w-full transition-all duration-300 ${index === 1 ? "bg-[#c7f36a]" : "bg-[#ececE5]"}`}
                  style={{ height: `${height}%` }}
                />
              </div>
            ))}
          </div>
          <div className="mt-2 flex gap-3 text-[10px] font-bold text-[#8a8a82]">
            {weekDays.map((day, index) => (
              <span key={index} className="flex-1 text-center">
                {day}
              </span>
            ))}
          </div>
          <p className="mt-5 text-xs text-[#6e6e67]">
            <em className="not-italic font-bold text-[#24241f]">3</em> sessions still available this week
          </p>
          <Link href="/trainerdashboard/calendar" className="mt-3 inline-flex items-center gap-1 text-xs font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">
            Open calendar →
          </Link>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <MetricCard label="Monthly revenue" value="$4,860" detail="↑ 18.6% vs. Feb" tone="blue" icon={<ArrowUpRight className="size-5" />} />
        <MetricCard label="Sessions completed" value="48" detail="6 sessions this week" tone="paper" icon={<Dumbbell className="size-5" />} />
        <MetricCard label="Active clients" value="17" detail="2 need a check-in" tone="orange" icon={<Users className="size-5" />} />
      </section>

      <section className="grid gap-4 lg:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white lg:col-span-8">
          <div className="flex items-center justify-between border-b border-[#e5e5de] px-5 py-4">
            <div>
              <p className="ledger-label">Today&apos;s flow</p>
              <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Four sessions, one focus.</h2>
            </div>
            <Link href="/trainerdashboard/calendar" className="flex items-center gap-1 text-sm font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">
              Full day <CalendarDays className="size-4" />
            </Link>
          </div>
          <div className="divide-y divide-[#ebebe5]">
            {sessions.map((session, index) => (
              <div className="flex items-center gap-4 px-5 py-4" key={`${session.time}-${session.client}`}>
                <div className="w-[56px] shrink-0">
                  <p className="mono text-sm font-bold">{session.time}</p>
                  <p className="mt-1 text-[10px] text-[#777770]">{session.duration}</p>
                </div>
                <span className={`size-2.5 shrink-0 rounded-full ${session.dot}`} />
                <div className="min-w-0 flex-1">
                  <p className="font-bold tracking-[-0.015em]">{session.client}</p>
                  <p className="mt-1 text-xs text-[#777770]">{session.focus}</p>
                </div>
                {index === 1 ? <StatusPill label="Next up" tone="orange" /> : <ArrowUpRight className="size-4 text-[#a5a59c]" />}
              </div>
            ))}
          </div>
        </div>

        <aside className="cut-corner bg-[#c7f36a] p-5 text-[#24241f] lg:col-span-4">
          <p className="ledger-label !text-[#4f5b32] before:!bg-[#24241f]">Client moment</p>
          <p className="display-face mt-4 text-2xl leading-[1.1]">Two people could use a little nudge.</p>
          <p className="mt-3 text-xs leading-relaxed text-[#4c592e]">
            Camille&apos;s check-in is waiting and Aria hasn&apos;t logged a session plan this week.
          </p>
          <div className="mt-6 flex items-center gap-2">
            <Avatar initials="CH" tone="peach" size="sm" />
            <Avatar initials="AW" tone="mint" size="sm" />
            <Link href="/trainerdashboard/clients" className="ml-auto flex items-center gap-1 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#46463e]">
              Open clients <ArrowUpRight className="size-3" />
            </Link>
          </div>
        </aside>
      </section>
    </div>
  );
}
