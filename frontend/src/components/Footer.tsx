import Image from "next/image";
import { BUILD_INFO } from "@/generated/build-info";

const socialLinks = [
  {
    label: "Instagram",
    href: "#",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-4 w-4">
        <rect x="3" y="3" width="18" height="18" rx="5" />
        <circle cx="12" cy="12" r="4" />
        <circle cx="17.2" cy="6.8" r="1" fill="currentColor" stroke="none" />
      </svg>
    ),
  },
  {
    label: "X",
    href: "#",
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
        <path d="M18.244 2H21.5l-7.5 8.57L22.75 22h-6.83l-5.35-6.99L4.5 22H1.24l8.02-9.17L1 2h6.99l4.84 6.4L18.244 2Zm-1.2 18.17h1.83L7.03 3.72H5.06l11.98 16.45Z" />
      </svg>
    ),
  },
  {
    label: "LinkedIn",
    href: "#",
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
        <path d="M4.98 3.5a2.5 2.5 0 1 1 0 5 2.5 2.5 0 0 1 0-5ZM3 9h4v12H3V9Zm7 0h3.83v1.64h.05c.53-1 1.83-2.06 3.77-2.06 4.03 0 4.77 2.65 4.77 6.1V21h-4v-5.6c0-1.34-.02-3.06-1.87-3.06-1.87 0-2.16 1.46-2.16 2.96V21h-4V9Z" />
      </svg>
    ),
  },
];

export default function Footer() {
  return (
    <footer className="bg-ink pb-8 text-paper/65">
      <div className="container flex flex-col gap-7 border-t border-paper/15 pt-8 sm:flex-row sm:items-center sm:justify-between">
        <a href="#top" className="flex items-center gap-2.5 text-paper">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-paper">
            <Image
              src="/GymSuit_logo_package/01-icon-brand.png"
              alt=""
              width={24}
              height={24}
              className="h-6 w-6 object-contain"
            />
          </span>
          <span className="font-display text-lg font-bold tracking-[-0.06em]">gymsuite</span>
        </a>
        <p className="text-xs leading-5">Gym management, thoughtfully built for India.</p>
        <div className="flex items-center gap-6">
          <div className="flex gap-5 text-xs font-semibold">
            <a href="mailto:info@gymmanagement.com" className="transition-colors hover:text-paper">
              Privacy
            </a>
            <a href="mailto:info@gymmanagement.com" className="transition-colors hover:text-paper">
              Terms
            </a>
          </div>
          <div className="flex items-center gap-3">
            {socialLinks.map((social) => (
              <a
                key={social.label}
                href={social.href}
                aria-label={social.label}
                className="grid h-8 w-8 place-items-center rounded-full border border-paper/15 text-paper/70 transition-colors hover:border-paper/35 hover:text-paper"
              >
                {social.icon}
              </a>
            ))}
          </div>
        </div>
      </div>
      <p className="container mt-6 text-xs text-paper/40">
        &copy; {new Date().getFullYear()} GymSuite. All rights reserved.
      </p>
      <p className="container mt-2 text-[11px] text-paper/40">
        Version {BUILD_INFO.version} · Built {BUILD_INFO.builtAt}
      </p>
    </footer>
  );
}
