import { authFetch } from "@/lib/auth";
import type { ManagerAccessScope } from "@/lib/team";

export type CurrentUser = {
  userId: number;
  email: string;
  displayName: string;
  role: "ADMIN" | "OWNER" | "MANAGER" | "TRAINER" | "MEMBER";
  gymId: number | null;
  trainerId: number | null;
  memberId: number | null;
  managerId: number | null;
  managerScopes: ManagerAccessScope[];
};

export function getCurrentUser() {
  return authFetch<CurrentUser>("/api/users/me");
}
