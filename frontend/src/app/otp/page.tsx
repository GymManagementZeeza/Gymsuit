import type { Metadata } from "next";
import { Suspense } from "react";
import OtpCard from "@/components/OtpCard";

export const metadata: Metadata = {
  title: "Verify — GymSuite",
  description: "Enter the code we sent to your email to sign in to GymSuite.",
};

export default function OtpPage() {
  return (
    <Suspense fallback={null}>
      <OtpCard />
    </Suspense>
  );
}
