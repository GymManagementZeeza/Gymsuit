/* Training Ledger component: Sharp operational controls, status markers, and ledger-ready summaries. */
import type { ReactNode } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";

export function ActionButton({
  children,
  icon,
  onClick,
}: {
  children: ReactNode;
  icon?: ReactNode;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-2 bg-[#c7f36a] px-3.5 py-2.5 text-sm font-bold text-[#25251f] transition duration-150 hover:bg-[#d8ff8a] active:scale-[0.97]"
    >
      {icon}
      {children}
    </button>
  );
}

export function StatusPill({
  label,
  tone = "lime",
}: {
  label: string;
  tone?: "lime" | "orange" | "blue" | "ink";
}) {
  const styles = {
    lime: "bg-[#c7f36a] text-[#24241f]",
    orange: "bg-[#ffe0c7] text-[#8f4513]",
    blue: "bg-[#dce6ff] text-[#3154a2]",
    ink: "bg-[#32322d] text-white",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 px-2 py-1 text-[10px] font-bold uppercase tracking-[0.08em]",
        styles[tone]
      )}
    >
      <span
        className={cn(
          "size-1.5 rounded-full",
          tone === "lime"
            ? "bg-[#24241f]"
            : tone === "orange"
              ? "bg-[#f47539]"
              : tone === "blue"
                ? "bg-[#4a72db]"
                : "bg-[#c7f36a]"
        )}
      />
      {label}
    </span>
  );
}

export function MetricCard({
  label,
  value,
  detail,
  tone,
  icon,
  href,
  linkSuffix = "view details",
}: {
  label: string;
  value: string;
  detail: string;
  tone: "lime" | "paper" | "orange" | "blue";
  icon: ReactNode;
  href?: string;
  linkSuffix?: string;
}) {
  const tones = {
    lime: "border border-[#c7f36a] bg-[#fcfcf5] text-[#24241f]",
    paper: "border border-[#d8d8d1] bg-white text-[#24241f]",
    orange: "bg-[#ffded2] text-[#512319]",
    blue: "border border-[#c3d3fb] bg-[#eef2ff] text-[#24241f]",
  };
  const clickable = href ? "cursor-pointer transition hover:-translate-y-0.5 hover:shadow-[0_10px_28px_rgba(20,20,16,0.10)]" : "";
  const card = (
    <>
      {tone === "lime" && <span className="absolute inset-y-0 left-0 w-1 bg-[#c7f36a]" />}
      {tone === "blue" && <span className="absolute inset-y-0 left-0 w-1 bg-[#4a72db]" />}
      <div className="flex items-start justify-between">
        <p className="text-[10px] font-bold uppercase tracking-[0.13em] opacity-65">{label}</p>
        <div className={tone === "lime" ? "text-[#70991a]" : tone === "blue" ? "text-[#3154a2]" : "opacity-65"}>{icon}</div>
      </div>
      <div>
        <p
          className={`display-face mt-5 text-3xl leading-none ${tone === "lime" ? "text-[#668d13]" : tone === "blue" ? "text-[#3154a2]" : ""}`}
        >
          {value}
        </p>
        <p className="mt-2 text-[11px] font-medium opacity-65">{detail}</p>
      </div>
    </>
  );
  if (href) {
    return (
      <Link
        href={href}
        aria-label={`${label} — ${linkSuffix}`}
        className={`relative flex flex-col justify-between overflow-hidden p-4 ${tones[tone]} ${clickable}`}
      >
        {card}
      </Link>
    );
  }
  return (
    <article className={`relative flex flex-col justify-between overflow-hidden p-4 ${tones[tone]}`}>
      {card}
    </article>
  );
}

/* ------------------------------------------------------------------ */
/* FLEX design language — light-mode card system shared by every       */
/* dashboard page. Rounded cards, soft shadows, lime/amber/red tones.  */
/* ------------------------------------------------------------------ */

const flexCardShadow = "shadow-[0_2px_16px_rgba(20,20,16,0.06)]";

export function FlexCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn(`rounded-[1.75rem] bg-white p-5 ${flexCardShadow}`, className)}>
      {children}
    </section>
  );
}

export function FlexPageHeader({
  title,
  subtitle,
  actions,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 pt-1">
      <div className="min-w-0">
        <h1 className="text-2xl font-black tracking-tight text-stone-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm font-medium text-stone-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function FlexPill({
  children,
  tone = "lime",
  className,
}: {
  children: ReactNode;
  tone?: "lime" | "green" | "amber" | "red" | "stone";
  className?: string;
}) {
  const tones = {
    lime: "bg-[#c7f36a] text-stone-900",
    green: "border border-green-500 text-green-600",
    amber: "border border-amber-700/30 text-amber-800",
    red: "bg-[#ffe3e3] text-[#7f1d1d]",
    stone: "bg-stone-100 text-stone-600",
  };
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center rounded-full px-3 py-1 text-[10px] font-black tracking-[0.08em]",
        tones[tone],
        className
      )}
    >
      {children}
    </span>
  );
}

export function FlexIconBadge({
  children,
  tone = "neutral",
  className,
}: {
  children: ReactNode;
  tone?: "neutral" | "lime" | "amber" | "red" | "green";
  className?: string;
}) {
  const tones = {
    neutral: "bg-black/[0.07] text-stone-800",
    lime: "bg-[#c7f36a] text-stone-900",
    amber: "bg-[#f5a623] text-white",
    red: "bg-[#ffe3e3] text-[#7f1d1d]",
    green: "bg-green-100 text-green-700",
  };
  return (
    <span className={cn("grid size-10 shrink-0 place-items-center rounded-2xl", tones[tone], className)}>
      {children}
    </span>
  );
}

export function FlexButton({
  children,
  onClick,
  variant = "lime",
  className,
  type = "button",
}: {
  children: ReactNode;
  onClick?: () => void;
  variant?: "lime" | "dark" | "ghost";
  className?: string;
  type?: "button" | "submit";
}) {
  const variants = {
    lime: "bg-[#c7f36a] text-stone-900 hover:bg-[#d8ff8a]",
    dark: "bg-stone-900 text-white hover:bg-stone-700",
    ghost: "bg-stone-100 text-stone-700 hover:bg-stone-200",
  };
  return (
    <button
      type={type}
      onClick={onClick}
      className={cn(
        "inline-flex items-center gap-2 rounded-full px-4 py-2.5 text-sm font-bold transition active:scale-[0.97]",
        variants[variant],
        className
      )}
    >
      {children}
    </button>
  );
}

export function FlexStatCard({
  label,
  value,
  detail,
  icon,
  href,
  variant = "white",
}: {
  label: string;
  value: string;
  detail?: string;
  icon?: ReactNode;
  href?: string;
  variant?: "white" | "lime" | "red" | "amber";
}) {
  const styles = {
    white: `bg-white text-stone-900 ${flexCardShadow}`,
    lime: "bg-gradient-to-br from-[#d3f34f] to-[#a4d614] text-[#1c2b06] shadow-[0_2px_16px_rgba(120,160,20,0.25)]",
    red: "bg-[#ffe3e3] text-[#7f1d1d] shadow-[0_2px_16px_rgba(160,30,30,0.10)]",
    amber: "bg-[#fdf1dc] text-stone-900 shadow-[0_2px_16px_rgba(160,120,30,0.10)]",
  };
  const card = (
    <>
      <div className="flex items-start justify-between gap-2">
        <p className="text-[11px] font-bold uppercase tracking-[0.12em] opacity-70">{label}</p>
        {icon && <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-black/[0.07]">{icon}</span>}
      </div>
      <div>
        <p className="mt-6 text-4xl font-black tracking-tight">{value}</p>
        {detail && <p className="mt-1 text-xs font-semibold opacity-70">{detail}</p>}
      </div>
    </>
  );
  const cls = `flex min-h-[148px] flex-col justify-between rounded-[1.75rem] p-5 transition ${styles[variant]}`;
  if (href) {
    return (
      <Link href={href} aria-label={`${label} — ${value}`} className={cn(cls, "hover:-translate-y-0.5")}>
        {card}
      </Link>
    );
  }
  return <article className={cls}>{card}</article>;
}

export function FlexEmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="rounded-2xl bg-stone-100/70 px-4 py-8 text-center">
      <p className="text-sm font-bold text-stone-600">{title}</p>
      {hint && <p className="mt-1 text-xs text-stone-400">{hint}</p>}
    </div>
  );
}

const avatarHues: Record<string, string> = {
  peach: "bg-[#f4cfbd]",
  sky: "bg-[#d7e4fd]",
  mint: "bg-[#d3ecd2]",
  lavender: "bg-[#e3d9fa]",
  lime: "bg-[#c7f36a]",
};

export function Avatar({ initials, tone = "peach", size = "md" }: { initials: string; tone?: keyof typeof avatarHues; size?: "sm" | "md" | "lg" }) {
  const sizes = { sm: "size-8 text-[9px]", md: "size-10 text-[10px]", lg: "size-14 text-sm" };
  return (
    <div className={cn("grid shrink-0 place-items-center rounded-full font-bold text-[#24241f]", sizes[size], avatarHues[tone] ?? avatarHues.peach)}>
      {initials}
    </div>
  );
}

export function AvatarStack({ initials }: { initials: string[] }) {
  const hues = ["bg-[#f5c7b5]", "bg-[#b8ccff]", "bg-[#d0e3cf]", "bg-[#f0dc9f]"];
  return (
    <div className="flex -space-x-2">
      {initials.slice(0, 3).map((item, index) => (
        <span
          key={`${item}-${index}`}
          className={`grid size-7 place-items-center rounded-full border-2 border-white text-[8px] font-bold text-[#24241f] ${hues[index]}`}
        >
          {item}
        </span>
      ))}
    </div>
  );
}

export function TableAction({
  children,
  onClick,
  disabled,
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="border border-[#d9d9d2] bg-white px-2.5 py-1.5 text-[11px] font-bold transition hover:border-[#24241f] hover:bg-[#f7f7f2] active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-[#d9d9d2] disabled:hover:bg-white"
    >
      {children}
    </button>
  );
}
