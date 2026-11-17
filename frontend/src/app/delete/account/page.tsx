// Correctly spelled alias for /detele/account, so either link works.
import type { Metadata } from "next";
import DeleteAccountCard from "@/components/DeleteAccountCard";

export const metadata: Metadata = {
  title: "Delete your account — GymSuite",
  description: "Permanently delete your GymSuit account and its data. Confirm with a one-time code sent to your email.",
};

export default function DeleteAccountPage() {
  return <DeleteAccountCard />;
}
