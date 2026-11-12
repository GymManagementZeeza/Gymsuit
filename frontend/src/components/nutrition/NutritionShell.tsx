"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { dashboardPathForRole, logout } from "@/lib/auth";
import { useSession } from "@/hooks/useSession";
import { BUILD_INFO } from "@/generated/build-info";
import { getCurrentUser } from "@/lib/users";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import { LogOut, Menu, UsersRound, X } from "lucide-react";

const navItems = [{ label: "Clients", href: "/nutritiondashboard", icon: UsersRound }];

function NavContent({
  pathname,
  onNavigate,
  displayName,
  initials,
}: {
  pathname: string;
  onNavigate: () => void;
  displayName: string;
  initials: string;
}) {
  return (
    <>
      <div className="p-4 pb-3">
        <Link href="/nutritiondashboard" onClick={onNavigate} className="flex items-center px-1 py-1" aria-label="GymSuite Diet home">
          <span className="inline-flex items-center rounded-lg bg-white px-3 py-2">
            <Image
              src="/GymSuit_logo_package/02-horizontal-brand.png"
              alt="GymSuite"
              width={280}
              height={120}
              className="h-7 w-auto object-contain"
            />
          </span>
        </Link>
        <div className="mt-5 flex w-full items-center gap-3 border border-white/15 bg-white/[0.05] px-3 py-2.5">
          <span className="grid size-8 shrink-0 place-items-center rounded-full border border-[#c7f36a]/40 bg-[#4b4b42] text-[10px] font-bold text-[#c7f36a]">
            {initials}
          </span>
          <span className="min-w-0">
            <span className="block truncate text-xs font-bold text-white">{displayName}</span>
            <span className="mt-0.5 block truncate text-[10px] text-[#adada4]">Nutrition expert</span>
          </span>
        </div>
      </div>
      <div className="mx-4 h-px bg-white/10" />
      <div className="flex-1 overflow-y-auto px-2 py-3">
        <p className="px-3 pb-2 text-[9px] font-bold uppercase tracking-[0.16em] text-[#898980]">Nutrition desk</p>
        <div className="flex flex-col gap-0.5">
          {navItems.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/clients`);
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
              </Link>
            );
          })}
        </div>
      </div>
    </>
  );
}

/** Shell for standalone nutrition experts (no gym): their invited clients and read-only nutrition progress. */
export function NutritionShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const session = useSession("/diet");
  const [navOpen, setNavOpen] = useState(false);
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);
  const [displayName, setDisplayName] = useState<string | null>(null);
  const closeNav = () => setNavOpen(false);

  // Gym roles have their own dashboards — send them there instead of showing an empty, forbidden view.
  useEffect(() => {
    if (session && session.role !== "NUTRITIONIST") {
      router.replace(dashboardPathForRole(session.role));
    }
  }, [session, router]);

  useEffect(() => {
    if (!session) return;
    getCurrentUser()
      .then((user) => setDisplayName(user.displayName))
      .catch(() => {});
  }, [session]);

  const resolvedName = displayName ?? "…";
  const initials =
    resolvedName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("") || "…";

  const handleLogout = () => {
    void logout().finally(() => router.push("/diet"));
  };

  const todayLabel = new Date().toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long" });

  return (
    <div className="flex min-h-screen bg-[#f6f6f0] text-[#24241f]">
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 shrink-0 flex-col bg-[#24241f] transition-transform duration-200 lg:static lg:translate-x-0 ${
          navOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <NavContent pathname={pathname} onNavigate={closeNav} displayName={resolvedName} initials={initials} />
      </aside>

      {navOpen && <div className="fixed inset-0 z-30 bg-black/40 lg:hidden" onClick={closeNav} aria-hidden="true" />}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-[#e1e1da] bg-[#f6f6f0]/90 px-4 backdrop-blur-sm sm:px-7">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setNavOpen((open) => !open)}
              className="grid size-9 place-items-center border border-[#ddddd6] bg-white lg:hidden"
              aria-label={navOpen ? "Close menu" : "Open menu"}
              aria-expanded={navOpen}
            >
              {navOpen ? <X className="size-4" /> : <Menu className="size-4" />}
            </button>
            <p className="ledger-label !text-[#777770]">{todayLabel}</p>
          </div>
          <button
            type="button"
            onClick={() => setLogoutConfirmOpen(true)}
            className="grid size-9 place-items-center border border-[#ddddd6] bg-white transition hover:border-[#24241f]"
            aria-label="Log out"
            title="Log out"
          >
            <LogOut className="size-4" />
          </button>
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
