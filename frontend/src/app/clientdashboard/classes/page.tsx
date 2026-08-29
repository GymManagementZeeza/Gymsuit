"use client";

/* Training Ledger page: Book a class — timetable, tabs, and one-tap booking. */
import { useState } from "react";
import { StatusPill } from "@/components/dashboard/ui";
import { CalendarDays, Check } from "lucide-react";

const classes = [
  { name: "Pilates flow", time: "Today · 18:00", coach: "Rina Patel", spots: "4 spots", tone: "blue" as const },
  { name: "Strength foundations", time: "Wed · 07:30", coach: "Maya Chen", spots: "2 spots", tone: "orange" as const },
  { name: "Stretch & restore", time: "Thu · 19:15", coach: "Sofia James", spots: "8 spots", tone: "lime" as const },
];

const tabs = ["Today", "This week", "My bookings"] as const;

export default function ClassesPage() {
  const [tab, setTab] = useState<(typeof tabs)[number]>("Today");
  const [booked, setBooked] = useState<string[]>([]);

  return (
    <div className="page-enter space-y-7">
      <div className="cut-corner flex flex-col justify-between gap-6 bg-[#24241f] p-6 text-white sm:flex-row sm:items-center">
        <div>
          <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Move together</p>
          <p className="display-face mt-3 text-3xl leading-[1.05]">
            What kind of
            <br />
            good do you need?
          </p>
          <p className="mt-3 text-sm text-white/65">Book classes that make it easier to show up.</p>
        </div>
        <div className="flex flex-col items-center border border-white/20 px-6 py-4 text-center">
          <span className="text-[10px] font-bold uppercase tracking-[0.15em] text-white/60">Mar</span>
          <span className="display-face text-3xl">11</span>
          <span className="text-[10px] font-bold uppercase tracking-[0.15em] text-white/60">Tuesday</span>
        </div>
      </div>

      <div className="flex gap-2 border-b border-[#d8d8d1]">
        {tabs.map((item) => (
          <button
            key={item}
            onClick={() => setTab(item)}
            className={`-mb-px border-b-2 px-3 py-2.5 text-xs font-bold ${
              tab === item ? "border-[#c7f36a] text-[#24241f]" : "border-transparent text-[#8a8a82] hover:text-[#24241f]"
            }`}
          >
            {item}
          </button>
        ))}
      </div>

      <section className="grid gap-4">
        {classes.map((item) => {
          const isBooked = booked.includes(item.name);
          return (
            <article key={item.name} className="flex flex-col gap-4 border border-[#d8d8d1] bg-white p-5 sm:flex-row sm:items-center">
              <div className="grid size-12 shrink-0 place-items-center bg-[#24241f] text-[#c7f36a]">
                <CalendarDays className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <StatusPill label={item.spots} tone={item.tone} />
                <h3 className="mt-2 text-lg font-bold tracking-[-0.02em]">{item.name}</h3>
                <p className="mt-1 text-xs text-[#6e6e67]">
                  {item.time} · with {item.coach}
                </p>
              </div>
              <button
                onClick={() => setBooked((prev) => [...prev, item.name])}
                disabled={isBooked}
                className={`flex shrink-0 items-center justify-center gap-2 px-3.5 py-2.5 text-sm font-bold transition ${
                  isBooked ? "bg-[#ececE5] text-[#8a8a82]" : "bg-[#c7f36a] text-[#25251f] hover:bg-[#d8ff8a]"
                }`}
              >
                {isBooked ? (
                  <>
                    <Check className="size-4" /> Reserved
                  </>
                ) : (
                  "Book class"
                )}
              </button>
            </article>
          );
        })}
      </section>
    </div>
  );
}
