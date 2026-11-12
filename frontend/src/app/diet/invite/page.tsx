import type { Metadata } from "next";
import { Suspense } from "react";
import DietInviteCard from "@/components/nutrition/DietInviteCard";

export const metadata: Metadata = {
  title: "Nutrition invite — GymSuite",
  description: "Accept a nutrition expert's invitation to follow your meals and progress.",
};

export default function DietInvitePage() {
  return (
    <Suspense fallback={null}>
      <DietInviteCard />
    </Suspense>
  );
}
