import type { Metadata } from "next";
import { DM_Serif_Display, DM_Mono } from "next/font/google";
import { ClientShell } from "@/components/client/ClientShell";

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
  title: "Member workspace — GymFlow",
  description: "A little progress looks good on you.",
};

export default function ClientDashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={`${dmSerif.variable} ${dmMono.variable}`}>
      <ClientShell>{children}</ClientShell>
    </div>
  );
}
