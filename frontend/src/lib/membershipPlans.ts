import { authFetch } from "@/lib/auth";

export type BillingCycle = "WEEKLY" | "BIWEEKLY" | "MONTHLY" | "QUARTERLY" | "SEMI_ANNUAL" | "ANNUAL";

export type MembershipPlan = {
  id: number;
  gymId: number;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  billingCycle: BillingCycle;
  active: boolean;
};

export type MembershipPlanInput = {
  name: string;
  description?: string;
  price: number;
  currency: string;
  billingCycle: BillingCycle;
};

export function listMembershipPlans(gymId: number) {
  return authFetch<MembershipPlan[]>(`/api/gyms/${gymId}/membership-plans`);
}

export function createMembershipPlan(gymId: number, input: MembershipPlanInput) {
  return authFetch<MembershipPlan>(`/api/gyms/${gymId}/membership-plans`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function deleteMembershipPlan(gymId: number, id: number) {
  return authFetch<void>(`/api/gyms/${gymId}/membership-plans/${id}`, {
    method: "DELETE",
  });
}
