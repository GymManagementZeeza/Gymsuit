import type { Metadata } from "next";
import { DM_Serif_Display, DM_Mono } from "next/font/google";
import { TrainerShell } from "@/components/trainer/TrainerShell";

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
  title: "Trainer workspace — GymFlow",
  description: "Progress is personal — your clients, sessions and earnings in one place.",
};

export default function TrainerDashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={`${dmSerif.variable} ${dmMono.variable}`}>
      <TrainerShell>{children}</TrainerShell>
    </div>
  );
}
