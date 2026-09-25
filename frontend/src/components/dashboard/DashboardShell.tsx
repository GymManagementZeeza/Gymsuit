"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { useSession } from "@/hooks/useSession";
import { dashboardPathForRole, logout, type LoginResponse } from "@/lib/auth";
import { getGym } from "@/lib/gyms";
import { getCurrentUser } from "@/lib/users";
import { listSessionRequests } from "@/lib/sessionRequests";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import NotificationSheet from "@/components/dashboard/NotificationSheet";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { BUILD_INFO } from "@/generated/build-info";
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

function buildNavGroups(t: Dictionary) {
  return [
    { label: t.nav.groups.command, items: [{ label: t.nav.overview, href: "/dashboard", icon: Gauge }] },
    {
      label: t.nav.groups.people,
      items: [
        { label: t.nav.members, href: "/dashboard/members", icon: UsersRound },
        { label: t.nav.trainers, href: "/dashboard/trainers", icon: Dumbbell },
        { label: t.nav.teamAccess, href: "/dashboard/team-access", icon: ShieldCheck },
      ],
    },
    {
      label: t.nav.groups.training,
      items: [
        { label: t.nav.sessionRequests, href: "/dashboard/session-requests", icon: BookOpenCheck },
        { label: t.nav.schedule, href: "/dashboard/schedule", icon: CalendarCheck2 },
      ],
    },
    {
      label: t.nav.groups.operations,
      items: [{ label: t.nav.liveFloor, href: "/dashboard/live-floor", icon: Gauge }],
    },
    {
      label: t.nav.groups.hardware,
      items: [{ label: t.nav.attendance, href: "/dashboard/attendance", icon: LockKeyhole }],
    },
    {
      label: t.nav.groups.finance,
      items: [
        { label: t.nav.staffPay, href: "/dashboard/staff-pay", icon: WalletCards },
        { label: t.nav.membershipPlans, href: "/dashboard/membership", icon: CreditCard },
        { label: t.nav.payments, href: "/dashboard/payments", icon: Receipt },
      ],
    },
  ];
}

function sectionLabelFor(pathname: string, t: Dictionary): string {
  const labels: Record<string, string> = {
    "/dashboard": `${t.nav.groups.command} · ${t.nav.overview}`,
    "/dashboard/members": `${t.nav.groups.people} · ${t.nav.members}`,
    "/dashboard/trainers": `${t.nav.groups.people} · ${t.nav.trainers}`,
    "/dashboard/team-access": `${t.nav.groups.people} · ${t.nav.teamAccess}`,
    "/dashboard/session-requests": `${t.nav.groups.training} · ${t.nav.sessionRequests}`,
    "/dashboard/schedule": `${t.nav.groups.training} · ${t.nav.schedule}`,
    "/dashboard/classes": `${t.nav.groups.training} · ${t.nav.classes}`,
    "/dashboard/attendance": `${t.nav.groups.hardware} · ${t.nav.attendance}`,
    "/dashboard/live-floor": `${t.nav.groups.operations} · ${t.nav.liveFloor}`,
    "/dashboard/staff-pay": `${t.nav.groups.finance} · ${t.nav.staffPay}`,
    "/dashboard/membership": `${t.nav.groups.finance} · ${t.nav.membershipPlans}`,
    "/dashboard/payments": `${t.nav.groups.finance} · ${t.nav.payments}`,
    "/dashboard/settings": `${t.nav.groups.system} · ${t.nav.settings}`,
  };
  return labels[pathname] ?? t.nav.groups.operations;
}

function NavContent({
  pathname,
  onNavigate,
  gymName,
  roleLabel,
  displayName,
  initials,
  pendingRequests,
}: {
  pathname: string;
  onNavigate: () => void;
  gymName: string;
  roleLabel: string;
  displayName: string;
  initials: string;
  pendingRequests: number;
}) {
  const { t } = useLanguage();
  const navGroups = buildNavGroups(t);
  return (
    <>
      <div className="p-4 pb-3">
        <Link href="/dashboard" onClick={onNavigate} className="flex items-center gap-3 px-1 py-1">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white">
            <Image
              src="/GymSuit_logo_package/01-icon-brand.png"
              alt=""
              width={28}
              height={28}
              className="size-7 object-contain"
            />
          </span>
          <div>
            <p className="text-[15px] font-bold tracking-[-0.04em] text-white">GymSuite</p>
            <p className="mono mt-0.5 text-[9px] tracking-[0.14em] text-[#a7a79e]">{t.nav.operationsTag.toUpperCase()}</p>
          </div>
        </Link>
        <div className="mt-5 flex w-full items-center justify-between border border-white/15 bg-white/[0.05] px-3 py-2.5 text-left">
          <span>
            <span className="block truncate text-xs font-bold text-white">{gymName}</span>
            <span className="mt-0.5 block text-[10px] text-[#adada4]">{t.nav.gymCount}</span>
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
                // Live count for session requests; no badge when there is nothing pending.
                const badge =
                  item.href === "/dashboard/session-requests" ? pendingRequests : 0;
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
                    {badge > 0 && (
                      <span className="ml-auto bg-[#c7f36a] px-1.5 py-0.5 text-[9px] font-bold text-[#24241f]">
                        {badge}
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
          <Settings2 className="size-4" /> {t.nav.settings}
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

// /dashboard is the staff console — trainers and members have their own dashboards and don't belong here.
const STAFF_ROLES: LoginResponse["role"][] = ["ADMIN", "OWNER", "MANAGER"];

export function DashboardShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const session = useSession();
  const { t } = useLanguage();
  const [navOpen, setNavOpen] = useState(false);
  const [gymName, setGymName] = useState<string | null>(null);
  const [pendingRequests, setPendingRequests] = useState(0);
  const [userName, setUserName] = useState<string | null>(null);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const closeNav = () => setNavOpen(false);
  const sectionLabel = sectionLabelFor(pathname, t);

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

  // Keep the Session requests badge honest: reflect the real pending count,
  // and refresh on navigation so it updates after approve/reject actions.
  useEffect(() => {
    if (!session?.gymId) return;
    listSessionRequests(session.gymId)
      .then((requests) =>
        setPendingRequests(requests.filter((r) => r.status === "PENDING").length)
      )
      .catch(() => {});
  }, [session?.gymId, pathname]);

  useEffect(() => {
    if (!session) return;
    getCurrentUser()
      .then((user) => setUserName(user.displayName))
      .catch(() => {});
  }, [session]);

  const displayGymName = gymName ?? t.settings.yourGym;
  const roleLabel = session ? (t.roles[session.role] ?? session.role) : "";
  const displayName = userName ?? "";
  const initials =
    displayName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("") || "…";

  const handleLogout = () => {
    void logout().finally(() => router.push("/login"));
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
          pendingRequests={pendingRequests}
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
              aria-label={navOpen ? t.header.closeMenu : t.header.openMenu}
              aria-expanded={navOpen}
            >
              {navOpen ? <X className="size-4" /> : <Menu className="size-4" />}
            </button>
            <p className="hidden text-xs font-semibold text-[#777770] sm:block">
              {displayGymName} <span className="mx-1 text-[#b1b1aa]">/</span> {sectionLabel}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setNotificationsOpen(true)}
              className="relative grid size-9 place-items-center border border-[#dddDd6] bg-white transition hover:border-[#24241f]"
              aria-label={t.header.openNotifications}
            >
              <Bell className="size-4" />
            </button>
            <button className="hidden items-center gap-2 bg-[#24241f] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#42423a] sm:flex">
              <Landmark className="size-3.5 text-[#c7f36a]" /> {t.header.gymAdministration}
            </button>
            <button
              type="button"
              onClick={() => setLogoutConfirmOpen(true)}
              className="grid size-9 place-items-center border border-[#dddDd6] bg-white transition hover:border-[#24241f]"
              aria-label={t.header.logOut}
              title={t.header.logOut}
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </header>
        <main className="min-w-0 flex-1 px-4 py-6 sm:px-7 lg:px-9 lg:py-8">{children}</main>
        <footer className="shrink-0 px-4 pb-5 sm:px-7 lg:px-9">
          <p className="text-[11px] text-[#a3a39a]">
            Version {BUILD_INFO.version} · Built {BUILD_INFO.builtAt}
          </p>
        </footer>
      </div>

      {logoutConfirmOpen && (
        <ConfirmDialog
          title={t.header.logOutTitle}
          description={t.header.logOutDescription}
          confirmLabel={t.header.logOut}
          onConfirm={handleLogout}
          onCancel={() => setLogoutConfirmOpen(false)}
        />
      )}
      {notificationsOpen && <NotificationSheet onClose={() => setNotificationsOpen(false)} />}
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
