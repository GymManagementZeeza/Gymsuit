import type { Metadata } from "next";
import JoiningFeeSetupCard from "@/components/JoiningFeeSetupCard";

export const metadata: Metadata = {
  title: "Set your joining fee — GymSuite",
  description: "Set the one-time joining fee new members pay to finish registration.",
};

export default function OwnerJoiningFeePage() {
  return <JoiningFeeSetupCard />;
}
