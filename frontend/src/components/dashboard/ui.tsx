/* Training Ledger component: Sharp operational controls, status markers, and ledger-ready summaries. */
import type { ReactNode } from "react";
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
}: {
  label: string;
  value: string;
  detail: string;
  tone: "lime" | "paper" | "orange" | "blue";
  icon: ReactNode;
}) {
  const tones = {
    lime: "border border-[#c7f36a] bg-[#fcfcf5] text-[#24241f]",
    paper: "border border-[#d8d8d1] bg-white text-[#24241f]",
    orange: "bg-[#ffded2] text-[#512319]",
    blue: "border border-[#c3d3fb] bg-[#eef2ff] text-[#24241f]",
  };
  return (
    <article className={`relative flex flex-col justify-between overflow-hidden p-4 ${tones[tone]}`}>
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
    </article>
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
