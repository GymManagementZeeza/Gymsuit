import type { Metadata } from "next";
import { DM_Serif_Display, DM_Mono } from "next/font/google";
import { DashboardShell } from "@/components/dashboard/DashboardShell";

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

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={`${dmSerif.variable} ${dmMono.variable}`}>
      <DashboardShell>{children}</DashboardShell>
    </div>
  );
}
