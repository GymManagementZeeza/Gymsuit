import type { Metadata } from "next";
import { cookies } from "next/headers";
import { DM_Serif_Display, DM_Mono } from "next/font/google";
import { DashboardShell } from "@/components/dashboard/DashboardShell";
import { LanguageProvider } from "@/lib/i18n/LanguageContext";
import type { Locale } from "@/lib/i18n/dictionaries";

const dmSerif = DM_Serif_Display({
  weight: "400",
  subsets: ["latin"],
  variable: "--font-dm-serif",
});

const dmMono = DM_Mono({
  weight: ["400", "500"],
  subsets: ["latin"],
  variable: "--font-dm-mono",
});

export const metadata: Metadata = {
  title: "Dashboard — GymSuite",
  description: "Run today's floor with nothing missed.",
};

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const cookieStore = await cookies();
  const initialLocale: Locale = cookieStore.get("gymsuite_locale")?.value === "ml" ? "ml" : "en";
  return (
    <div className={`${dmSerif.variable} ${dmMono.variable}`}>
      <LanguageProvider initialLocale={initialLocale}>
        <DashboardShell>{children}</DashboardShell>
      </LanguageProvider>
    </div>
  );
}
