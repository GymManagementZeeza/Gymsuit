"use client";

/* Mobile list pattern: a compact summary row that expands to full detail on tap.
   Used below the `md` breakpoint wherever desktop tables would otherwise force
   horizontal scrolling. Desktop keeps the full table untouched. */
import { useState, type ReactNode } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

export function ExpandableRow({
  summary,
  children,
  className,
  defaultOpen = false,
}: {
  summary: ReactNode;
  children: ReactNode;
  className?: string;
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className={cn("bg-white", className)}>
      <button
        type="button"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
        className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition active:bg-[#fafaf6]"
      >
        <span className="min-w-0 flex-1">{summary}</span>
        <ChevronDown
          className={cn(
            "size-4 shrink-0 text-[#8a8a82] transition-transform duration-200",
            open && "rotate-180"
          )}
        />
      </button>
      <div
        className={cn(
          "grid transition-all duration-200 ease-out",
          open ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
        )}
      >
        <div className="overflow-hidden">
          <div className="border-t border-[#efefe9] px-4 py-4">{children}</div>
        </div>
      </div>
    </div>
  );
}

/* Label/value pair for the expanded detail area. */
export function DetailRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-1.5">
      <p className="shrink-0 text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">{label}</p>
      <div className="min-w-0 text-right text-xs text-[#24241f]">{children}</div>
    </div>
  );
}
