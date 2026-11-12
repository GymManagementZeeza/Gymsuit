import Link from "next/link";
import Image from "next/image";
import { Sparkles } from "lucide-react";
import type { ReactNode } from "react";

export default function AuthShell({
  eyebrow,
  headline,
  description,
  children,
  panelWidth = "max-w-[400px]",
  variant = "gym",
}: {
  eyebrow: string;
  headline: string;
  description: string;
  children: ReactNode;
  panelWidth?: string;
  /** "diet" is the standalone nutrition expert site (diet.gymsuit.app): its own pitch, no gym stats. */
  variant?: "gym" | "diet";
}) {
  const diet = variant === "diet";
  const homeHref = diet ? "/diet" : "/";
  return (
    <div className="grid min-h-screen bg-paper text-ink lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-ink px-14 py-12 text-paper lg:flex lg:flex-col lg:justify-between">
        <div className="absolute inset-0 dot-field opacity-40" aria-hidden="true" />
        <div
          className="absolute -right-24 -top-24 h-72 w-72 rounded-full border-[36px] border-saffron/20"
          aria-hidden="true"
        />

        <Link href={homeHref} className="group relative z-10 flex items-center gap-2.5">
          <span className="grid h-10 w-10 place-items-center rounded-[13px] bg-paper shadow-[0_8px_20px_rgba(20,35,33,0.18)] transition-transform duration-200 group-hover:-rotate-3">
            <Image
              src="/GymSuit_logo_package/01-icon-brand.png"
              alt=""
              width={28}
              height={28}
              className="h-7 w-7 object-contain"
            />
          </span>
          <span className="font-display text-xl font-bold tracking-[-0.06em]">{diet ? "gymsuite diet" : "gymsuite"}</span>
        </Link>

        <div className="relative z-10 max-w-[440px]">
          <div className="eyebrow mb-6 border-paper/25 bg-paper/10 text-paper">
            <Sparkles className="h-3.5 w-3.5" /> {diet ? "For independent nutrition experts" : "Run your gym from one place"}
          </div>
          <h1 className="font-display text-[clamp(2.4rem,3.6vw,3.2rem)] font-bold leading-[0.98] tracking-[-0.06em]">
            {diet ? "Every client's plate, in one calm view." : "Members, payments and access — one calm dashboard."}
          </h1>
          <p className="mt-6 max-w-[380px] text-base leading-7 text-paper/65">
            {diet
              ? "Invite clients by email. Once they accept, follow the meals, macros and weight they log in the GymSuit app — no gym needed."
              : "Log in to check today's attendance, follow up on dues and keep your front desk moving without the paperwork."}
          </p>
        </div>

        <div className={`relative z-10 flex flex-wrap items-center gap-3 ${diet ? "invisible" : ""}`} aria-hidden={diet}>
          <div className="rounded-2xl bg-paper/95 px-4 py-3 text-ink shadow-lg">
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.13em] text-ink/50">
              <span className="pulse-dot" /> Today, 10:24 AM
            </div>
            <p className="mt-1 font-display text-base font-bold tracking-[-0.03em]">
              32 members checked in
            </p>
          </div>
          <div className="rounded-2xl bg-saffron px-4 py-3 text-white shadow-lg">
            <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-white/75">
              Collections
            </p>
            <p className="font-display text-xl font-bold tracking-[-0.04em]">₹18,420</p>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-center px-6 py-16 sm:px-10">
        <div className={`w-full ${panelWidth}`}>
          <Link href={homeHref} className="group mb-10 flex items-center gap-2.5 lg:hidden">
            <span className="grid h-10 w-10 place-items-center rounded-[13px] bg-paper shadow-[0_8px_20px_rgba(20,35,33,0.18)] ring-1 ring-ink/10 transition-transform duration-200 group-hover:-rotate-3">
              <Image
                src="/GymSuit_logo_package/01-icon-brand.png"
                alt=""
                width={28}
                height={28}
                className="h-7 w-7 object-contain"
              />
            </span>
            <span className="font-display text-xl font-bold tracking-[-0.06em]">{diet ? "gymsuite diet" : "gymsuite"}</span>
          </Link>

          <div className="eyebrow mb-5">{eyebrow}</div>
          <h2 className="display-title text-[clamp(2.1rem,3.4vw,2.8rem)]">{headline}</h2>
          <p className="mt-4 text-base leading-7 text-ink/60">{description}</p>

          {children}
        </div>
      </div>
    </div>
  );
}
