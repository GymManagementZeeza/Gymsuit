import type { Metadata } from "next";
import { DM_Serif_Display, DM_Mono } from "next/font/google";
import { NutritionShell } from "@/components/nutrition/NutritionShell";

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
  title: "Nutrition workspace — GymSuite Diet",
  description: "Your clients and what they eat, in one place.",
};

export default function NutritionDashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={`${dmSerif.variable} ${dmMono.variable}`}>
      <NutritionShell>{children}</NutritionShell>
    </div>
  );
}
