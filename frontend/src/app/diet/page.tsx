import type { Metadata } from "next";
import DietAuthCard from "@/components/nutrition/DietAuthCard";

export const metadata: Metadata = {
  title: "GymSuite Diet — for nutrition experts",
  description: "Sign in or create your nutrition expert account. Follow your clients' meals, macros and weight.",
};

export default function DietPage() {
  return <DietAuthCard />;
}
