"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { clearSession } from "@/lib/auth";
import { useSession } from "@/hooks/useSession";
import { getCurrentUser } from "@/lib/users";
import { getTrainer } from "@/lib/trainers";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import {
  Bell,
  BookOpenCheck,
  CalendarDays,
  CircleUserRound,
  Gauge,
  LogOut,
  Menu,
  UsersRound,
  WalletCards,
  X,
} from "lucide-react";

const navItems = [
  { label: "Overview", href: "/trainerdashboard", icon: Gauge },
  { label: "Clients", href: "/trainerdashboard/clients", icon: UsersRound },
  { label: "Bookings", href: "/trainerdashboard/bookings", icon: BookOpenCheck, badge: "3" },
  { label: "Calendar", href: "/trainerdashboard/calendar", icon: CalendarDays },
  { label: "Earnings", href: "/trainerdashboard/earnings", icon: WalletCards },
];

const sectionLabels: Record<string, [string, string]> = {
  "/trainerdashboard": ["Tuesday, 11 March", "Your day has a good rhythm."],
  "/trainerdashboard/clients": ["Client desk", "Care for the details."],
  "/trainerdashboard/bookings": ["Booking desk", "Every hour has a purpose."],
  "/trainerdashboard/calendar": ["Week 11 · March", "A well-paced week."],
  "/trainerdashboard/earnings": ["March 2025", "Your work is adding up."],
  "/trainerdashboard/profile": ["Coach profile", "This is how clients find you."],
};

function NavContent({
  pathname,
  onNavigate,
  displayName,
  initials,
  specialization,
}: {
  pathname: string;
  onNavigate: () => void;
  displayName: string;
  initials: string;
  specialization: string;
}) {
  return (
    <>
      <div className="p-4 pb-3">
        <Link href="/trainerdashboard" onClick={onNavigate} className="flex items-center gap-3 px-1 py-1">
          <Image src="/gymflow-mark.png" alt="" width={36} height={36} className="size-9" />
          <div>
            <p className="text-[15px] font-bold tracking-[-0.04em] text-white">GYMFLOW</p>
            <p className="mono mt-0.5 text-[9px] tracking-[0.14em] text-[#a7a79e]">TRAINER</p>
          </div>
        </Link>
        <Link
          href="/trainerdashboard/profile"
          onClick={onNavigate}
          className="mt-5 flex w-full items-center gap-3 border border-white/15 bg-white/[0.05] px-3 py-2.5 text-left transition hover:bg-white/10"
        >
          <span className="grid size-8 shrink-0 place-items-center rounded-full border border-[#c7f36a]/40 bg-[#4b4b42] text-[10px] font-bold text-[#c7f36a]">
            {initials}
          </span>
          <span className="min-w-0">
            <span className="block truncate text-xs font-bold text-white">{displayName}</span>
            <span className="mt-0.5 block truncate text-[10px] text-[#adada4]">{specialization}</span>
          </span>
        </Link>
      </div>
      <div className="mx-4 h-px bg-white/10" />
      <div className="flex-1 overflow-y-auto px-2 py-3">
        <p className="px-3 pb-2 text-[9px] font-bold uppercase tracking-[0.16em] text-[#898980]">Studio</p>
        <div className="flex flex-col gap-0.5">
          {navItems.map((item) => {
            const active = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.label}
                href={item.href}
                onClick={onNavigate}
                className={`relative flex h-9 items-center gap-2 px-3 text-xs font-semibold transition ${
                  active ? "bg-[#c7f36a] text-[#25251f]" : "text-[#d0d0c8] hover:bg-white/10 hover:text-white"
                }`}
              >
                <Icon className="size-4" />
                <span>{item.label}</span>
                {"badge" in item && item.badge && (
                  <span className="ml-auto bg-[#c7f36a] px-1.5 py-0.5 text-[9px] font-bold text-[#24241f]">{item.badge}</span>
                )}
              </Link>
            );
          })}
        </div>
      </div>
      <div className="p-3">
        <Link
          href="/trainerdashboard/profile"
          onClick={onNavigate}
          className={`flex items-center gap-3 px-3 py-2.5 text-xs font-semibold transition ${
            pathname === "/trainerdashboard/profile" ? "bg-[#c7f36a] text-[#24241f]" : "text-[#d0d0c8] hover:bg-white/10 hover:text-white"
          }`}
        >
          <CircleUserRound className="size-4" /> My profile
        </Link>
      </div>
    </>
  );
}

export function TrainerShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const session = useSession();
  const [navOpen, setNavOpen] = useState(false);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);
  const [displayName, setDisplayName] = useState<string | null>(null);
  const [specialization, setSpecialization] = useState<string | null>(null);
  const closeNav = () => setNavOpen(false);
  const [eyebrow, title] = sectionLabels[pathname] ?? ["Studio Compass", "Trainer workspace"];

  useEffect(() => {
    if (!session) return;
    getCurrentUser()
      .then((user) => setDisplayName(user.displayName))
      .catch(() => {});
  }, [session]);

  useEffect(() => {
    if (!session?.gymId || !session.trainerId) return;
    getTrainer(session.gymId, session.trainerId)
      .then((trainer) => setSpecialization(trainer.specialization))
      .catch(() => {});
  }, [session?.gymId, session?.trainerId]);

  const resolvedName = displayName ?? "…";
  const initials =
    resolvedName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("") || "…";

  const handleLogout = () => {
    clearSession();
    router.push("/login");
  };

  return (
    <div className="flex min-h-screen bg-[#f6f6f0] text-[#24241f]">
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 shrink-0 flex-col bg-[#24241f] transition-transform duration-200 lg:static lg:translate-x-0 ${
          navOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <NavContent
          pathname={pathname}
          onNavigate={closeNav}
          displayName={resolvedName}
          initials={initials}
          specialization={specialization || "Personal trainer"}
        />
      </aside>

      {navOpen && <div className="fixed inset-0 z-30 bg-black/40 lg:hidden" onClick={closeNav} aria-hidden="true" />}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-[#e1e1da] bg-[#f6f6f0]/90 px-4 backdrop-blur-sm sm:px-7">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setNavOpen((open) => !open)}
              className="grid size-9 place-items-center border border-[#dddDd6] bg-white lg:hidden"
              aria-label={navOpen ? "Close menu" : "Open menu"}
              aria-expanded={navOpen}
            >
              {navOpen ? <X className="size-4" /> : <Menu className="size-4" />}
            </button>
            <p className="ledger-label !text-[#777770]">{eyebrow}</p>
          </div>
          <div className="flex items-center gap-3">
            <button className="relative grid size-9 place-items-center border border-[#dddDd6] bg-white transition hover:border-[#24241f]" aria-label="Open notifications">
              <Bell className="size-4" />
              <span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-[#ff825d]" />
            </button>
            <button
              type="button"
              onClick={() => setLogoutConfirmOpen(true)}
              className="grid size-9 place-items-center border border-[#dddDd6] bg-white transition hover:border-[#24241f]"
              aria-label="Log out"
              title="Log out"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </header>
        <main className="min-w-0 flex-1 px-4 py-6 sm:px-7 lg:px-9 lg:py-8">
          <p className="display-face mb-6 text-2xl leading-[1.05] tracking-[-0.03em] sm:text-3xl">{title}</p>
          {children}
        </main>
      </div>

      {logoutConfirmOpen && (
        <ConfirmDialog
          title="Log out?"
          description="You'll need to sign in again to get back to your dashboard."
          confirmLabel="Log out"
          onConfirm={handleLogout}
          onCancel={() => setLogoutConfirmOpen(false)}
        />
      )}
    </div>
  );
}
