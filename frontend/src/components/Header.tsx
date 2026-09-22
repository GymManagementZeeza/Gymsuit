"use client";

import { useState, useEffect } from "react";
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

  // Prevent background scrolling when mobile menu is open
  useEffect(() => {
    if (mobileOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileOpen]);

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

        {/* Mobile Hamburger Button */}
        <button
          type="button"
          onClick={() => setMobileOpen((open) => !open)}
          className="relative z-50 grid h-10 w-10 place-items-center rounded-full border border-ink/15 bg-white text-ink transition-colors active:scale-95 lg:hidden cursor-pointer"
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
          aria-expanded={mobileOpen}
        >
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </nav>

      {/* Mobile Drawer Dropdown */}
      {mobileOpen && (
        <>
          {/* Backdrop overlay */}
          <div
            className="fixed inset-0 top-[76px] z-40 bg-ink/40 backdrop-blur-xs lg:hidden"
            onClick={closeMenu}
            aria-hidden="true"
          />

          <div className="container relative z-50 animate-menu-in border-t border-ink/10 bg-paper pb-6 pt-4 lg:hidden shadow-xl">
            <div className="flex flex-col gap-1">
              {navItems.map((item) => (
                <a
                  key={item.href}
                  href={item.href}
                  onClick={closeMenu}
                  className="rounded-xl px-4 py-3.5 text-base font-semibold text-ink hover:bg-ink/5 transition-colors active:bg-ink/10"
                >
                  {item.label}
                </a>
              ))}
              <Link
                href="/login"
                onClick={closeMenu}
                className="button-ink mt-3 w-full justify-center py-3.5 text-base shadow-md"
              >
                Login
              </Link>
            </div>
          </div>
        </>
      )}
    </header>
  );
}
