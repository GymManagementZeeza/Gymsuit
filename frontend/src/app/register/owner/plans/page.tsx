import type { Metadata } from "next";
import PlansSetupCard from "@/components/PlansSetupCard";

export const metadata: Metadata = {
  title: "Set your pricing — GymSuite",
  description: "Add membership plans and pricing for your gym.",
};

export default function OwnerPlansPage() {
  return <PlansSetupCard />;
}
