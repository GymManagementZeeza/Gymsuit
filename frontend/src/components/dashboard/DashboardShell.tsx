"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { useSession } from "@/hooks/useSession";
import { clearSession, dashboardPathForRole, type LoginResponse } from "@/lib/auth";
import { getGym } from "@/lib/gyms";
import { getCurrentUser } from "@/lib/users";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import {
  Bell,
  BookOpenCheck,
  CalendarCheck2,
  CreditCard,
  Dumbbell,
  Gauge,
  Landmark,
  LockKeyhole,
  LogOut,
  Menu,
  Receipt,
  Settings2,
  ShieldCheck,
  UsersRound,
  WalletCards,
  X,
} from "lucide-react";

const navGroups = [
  { label: "Command", items: [{ label: "Overview", href: "/dashboard", icon: Gauge }] },
  {
    label: "People",
    items: [
      { label: "Members", href: "/dashboard/members", icon: UsersRound },
      { label: "Trainers", href: "/dashboard/trainers", icon: Dumbbell },
      { label: "Team access", href: "/dashboard/team-access", icon: ShieldCheck },
    ],
  },
  {
    label: "Training",
    items: [
      { label: "Session requests", href: "/dashboard/session-requests", icon: BookOpenCheck, badge: "3" },
      { label: "Schedule", href: "/dashboard/schedule", icon: CalendarCheck2 },
      { label: "Classes", href: "/dashboard/classes", icon: CalendarCheck2 },
    ],
  },
  {
    label: "Operations",
    items: [{ label: "Live floor", href: "/dashboard/live-floor", icon: Gauge }],
  },
  {
    label: "Hardware",
    items: [{ label: "Attendance", href: "/dashboard/attendance", icon: LockKeyhole }],
  },
  {
    label: "Finance",
    items: [
      { label: "Staff pay", href: "/dashboard/staff-pay", icon: WalletCards },
      { label: "Membership plans", href: "/dashboard/membership", icon: CreditCard },
      { label: "Payments", href: "/dashboard/payments", icon: Receipt },
    ],
  },
];

const sectionLabels: Record<string, string> = {
  "/dashboard": "Command · Overview",
  "/dashboard/members": "People · Members",
  "/dashboard/trainers": "People · Trainers",
  "/dashboard/team-access": "People · Team access",
  "/dashboard/session-requests": "Training · Session requests",
  "/dashboard/schedule": "Training · Schedule",
  "/dashboard/classes": "Training · Classes",
  "/dashboard/attendance": "Hardware · Attendance",
  "/dashboard/live-floor": "Operations · Live floor",
  "/dashboard/staff-pay": "Finance · Staff pay",
  "/dashboard/membership": "Finance · Membership plans",
  "/dashboard/payments": "Finance · Payments",
  "/dashboard/settings": "System · Settings",
};

function NavContent({
  pathname,
  onNavigate,
  gymName,
  roleLabel,
  displayName,
  initials,
}: {
  pathname: string;
  onNavigate: () => void;
  gymName: string;
  roleLabel: string;
  displayName: string;
  initials: string;
}) {
  return (
    <>
      <div className="p-4 pb-3">
        <Link href="/dashboard" onClick={onNavigate} className="flex items-center gap-3 px-1 py-1">
          <Image src="/gymflow-mark.png" alt="" width={36} height={36} className="size-9" />
          <div>
            <p className="text-[15px] font-bold tracking-[-0.04em] text-white">GYMFLOW</p>
            <p className="mono mt-0.5 text-[9px] tracking-[0.14em] text-[#a7a79e]">OPERATIONS</p>
          </div>
        </Link>
        <div className="mt-5 flex w-full items-center justify-between border border-white/15 bg-white/[0.05] px-3 py-2.5 text-left">
          <span>
            <span className="block truncate text-xs font-bold text-white">{gymName}</span>
            <span className="mt-0.5 block text-[10px] text-[#adada4]">1 of 1 gym</span>
          </span>
        </div>
      </div>
      <div className="mx-4 h-px bg-white/10" />
      <div className="flex-1 overflow-y-auto px-2 py-3">
        {navGroups.map((group) => (
          <div key={group.label} className="p-0 pb-3">
            <p className="px-3 pb-2 text-[9px] font-bold uppercase tracking-[0.16em] text-[#898980]">
              {group.label}
            </p>
            <div className="flex flex-col gap-0.5">
              {group.items.map((item) => {
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
                      <span className="ml-auto bg-[#c7f36a] px-1.5 py-0.5 text-[9px] font-bold text-[#24241f]">
                        {item.badge}
                      </span>
                    )}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </div>
      <div className="p-3">
        <Link
          href="/dashboard/settings"
          onClick={onNavigate}
          className={`flex items-center gap-3 px-3 py-2.5 text-xs font-semibold transition ${
            pathname === "/dashboard/settings" ? "bg-[#c7f36a] text-[#24241f]" : "text-[#d0d0c8] hover:bg-white/10 hover:text-white"
          }`}
        >
          <Settings2 className="size-4" /> Settings
        </Link>
        <div className="mx-0 my-3 h-px bg-white/10" />
        <div className="flex items-center gap-2.5 px-2 pt-1">
          <div className="grid size-8 shrink-0 place-items-center rounded-none border border-[#c7f36a]/40 bg-[#4b4b42] text-[10px] font-bold text-[#c7f36a]">
            {initials}
          </div>
          <div className="min-w-0">
            <p className="truncate text-xs font-bold text-white">{displayName}</p>
            <p className="mt-0.5 truncate text-[10px] text-[#a7a79e]">
              {roleLabel} · {gymName}
            </p>
          </div>
        </div>
      </div>
    </>
  );
}

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Admin",
  OWNER: "Owner",
  MANAGER: "Manager",
  TRAINER: "Trainer",
  MEMBER: "Member",
};

// /dashboard is the staff console — trainers and members have their own dashboards and don't belong here.
const STAFF_ROLES: LoginResponse["role"][] = ["ADMIN", "OWNER", "MANAGER"];

export function DashboardShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const session = useSession();
  const [navOpen, setNavOpen] = useState(false);
  const [gymName, setGymName] = useState<string | null>(null);
  const [userName, setUserName] = useState<string | null>(null);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);
  const closeNav = () => setNavOpen(false);
  const sectionLabel = sectionLabels[pathname] ?? "Operations";

  useEffect(() => {
    if (!session) return;
    if (!STAFF_ROLES.includes(session.role)) {
      router.replace(dashboardPathForRole(session.role));
    }
  }, [session, router]);

  useEffect(() => {
    if (!session?.gymId) return;
    getGym(session.gymId)
      .then((gym) => setGymName(gym.name))
      .catch(() => {});
  }, [session?.gymId]);

  useEffect(() => {
    if (!session) return;
    getCurrentUser()
      .then((user) => setUserName(user.displayName))
      .catch(() => {});
  }, [session]);

  const displayGymName = gymName ?? "Your gym";
  const roleLabel = session ? (ROLE_LABELS[session.role] ?? session.role) : "";
  const displayName = userName ?? "";
  const initials =
    displayName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("") || "…";

  const handleLogout = () => {
    clearSession();
    router.push("/login");
  };

  if (session && !STAFF_ROLES.includes(session.role)) {
    return null;
  }

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
          gymName={displayGymName}
          roleLabel={roleLabel}
          displayName={displayName || "…"}
          initials={initials}
        />
      </aside>

      {navOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/40 lg:hidden"
          onClick={closeNav}
          aria-hidden="true"
        />
      )}

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
            <p className="hidden text-xs font-semibold text-[#777770] sm:block">
              {displayGymName} <span className="mx-1 text-[#b1b1aa]">/</span> {sectionLabel}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button className="relative grid size-9 place-items-center border border-[#dddDd6] bg-white transition hover:border-[#24241f]" aria-label="Open notifications">
              <Bell className="size-4" />
              <span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-[#ff825d]" />
            </button>
            <button className="hidden items-center gap-2 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#42423a] sm:flex">
              <Landmark className="size-3.5 text-[#c7f36a]" /> Gym administration
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
        <main className="min-w-0 flex-1 px-4 py-6 sm:px-7 lg:px-9 lg:py-8">{children}</main>
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

export function PageHeading({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: ReactNode;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <section className="relative flex flex-col gap-5 overflow-hidden border-b border-[#d8d8d1] pb-7 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="ledger-label">{eyebrow}</p>
        <h1 className="display-face mt-3 text-4xl leading-[0.88] tracking-[-0.045em] text-[#24241f] sm:text-5xl">
          {title}
        </h1>
        <p className="mt-3 max-w-md text-sm text-[#6e6e67]">{description}</p>
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </section>
  );
}
