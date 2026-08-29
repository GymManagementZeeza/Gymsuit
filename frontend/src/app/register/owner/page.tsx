import type { Metadata } from "next";
import { Suspense } from "react";
import OwnerRegisterCard from "@/components/OwnerRegisterCard";

export const metadata: Metadata = {
  title: "Register your gym — GymSuite",
  description: "Set up your gym on GymSuite as the owner.",
};

export default function OwnerRegisterPage() {
  return (
    <Suspense fallback={null}>
      <OwnerRegisterCard />
    </Suspense>
  );
}
