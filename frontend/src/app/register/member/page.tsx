import type { Metadata } from "next";
import { Suspense } from "react";
import MemberRegisterCard from "@/components/MemberRegisterCard";

export const metadata: Metadata = {
  title: "Join your gym — GymSuite",
  description: "Find your gym and register as a member on GymSuite.",
};

export default function MemberRegisterPage() {
  return (
    <Suspense fallback={null}>
      <MemberRegisterCard />
    </Suspense>
  );
}
