import type { Metadata } from "next";
import { Suspense } from "react";
import RegisterChoiceCard from "@/components/RegisterChoiceCard";

export const metadata: Metadata = {
  title: "Register — GymSuite",
  description: "Create your GymSuite account as a gym owner or a member.",
};

export default function RegisterPage() {
  return (
    <Suspense fallback={null}>
      <RegisterChoiceCard />
    </Suspense>
  );
}
