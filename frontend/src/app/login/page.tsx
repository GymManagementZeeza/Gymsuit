import type { Metadata } from "next";
import LoginCard from "@/components/LoginCard";

export const metadata: Metadata = {
  title: "Login — GymSuite",
  description: "Log in to your GymSuite dashboard to manage members, payments and access.",
};

export default function LoginPage() {
  return <LoginCard />;
}
