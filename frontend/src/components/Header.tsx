"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Menu, X } from "lucide-react";

const navItems = [
  { label: "Product", href: "#product" },
  { label: "Access control", href: "#access" },
  { label: "Member app", href: "#members" },
  { label: "Pricing", href: "#pricing" },
];

export default function Header() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const closeMenu = () => setMobileOpen(false);

  return (
    <header className="sticky top-0 z-50 border-b border-ink/10 bg-paper/95 backdrop-blur-md">
      <nav
        className="container flex h-[76px] items-center justify-between"
        aria-label="Main navigation"
      >
        <a href="#top" className="group flex items-center gap-2.5" aria-label="GymSuite home">
          <span className="grid h-10 w-10 place-items-center rounded-[13px] bg-white shadow-[0_8px_20px_rgba(20,35,33,0.18)] transition-transform duration-200 group-hover:-rotate-3">
            <Image
              src="/GymSuit_logo_package/01-icon-brand.png"
              alt=""
              width={28}
              height={28}
              className="h-7 w-7 object-contain"
            />
          </span>
          <span className="font-display text-xl font-bold tracking-[-0.06em]">gymsuite</span>
        </a>

        <div className="hidden items-center gap-7 lg:flex">
          {navItems.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className="text-sm font-semibold text-ink/65 transition-colors hover:text-ink"
            >
              {item.label}
            </a>
          ))}
        </div>

        <div className="hidden items-center gap-5 lg:flex">
          <Link href="/login" className="button-ink px-5 py-2.5">
            Login
          </Link>
        </div>

        <button
          type="button"
          onClick={() => setMobileOpen((open) => !open)}
          className="grid h-10 w-10 place-items-center rounded-full border border-ink/15 bg-white text-ink lg:hidden"
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
          aria-expanded={mobileOpen}
        >
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </nav>

      {mobileOpen && (
        <div className="container animate-menu-in border-t border-ink/10 bg-paper pb-6 pt-4 lg:hidden">
          <div className="flex flex-col gap-1">
            {navItems.map((item) => (
              <a
                key={item.href}
                href={item.href}
                onClick={closeMenu}
                className="rounded-xl px-3 py-3 text-base font-semibold hover:bg-ink/5"
              >
                {item.label}
              </a>
            ))}
            <Link
              href="/login"
              onClick={closeMenu}
              className="button-ink mt-3 w-full justify-center py-3"
            >
              Login
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
