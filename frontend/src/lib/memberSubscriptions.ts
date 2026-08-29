import { authFetch, authFetchOptional } from "@/lib/auth";

export type SubscriptionStatus = "PENDING" | "ACTIVE" | "PAUSED" | "CANCELLED" | "EXPIRED";

export type MemberSubscription = {
  id: number;
  gymId: number;
  memberId: number;
  planId: number;
  planName: string;
  planPrice: number;
  planCurrency: string;
  planBillingCycle: string;
  status: SubscriptionStatus;
  // Null while PENDING — a plan doesn't have real dates until it's paid for and activated.
  startDate: string | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  autoRenew: boolean;
};

export function subscribeMember(gymId: number, memberId: number, planId: number) {
  return authFetch<MemberSubscription>(`/api/gyms/${gymId}/members/${memberId}/subscriptions`, {
    method: "POST",
    body: JSON.stringify({ planId }),
  });
}

export function getCurrentSubscription(gymId: number, memberId: number) {
  return authFetchOptional<MemberSubscription>(`/api/gyms/${gymId}/members/${memberId}/subscriptions/current`);
}

export function listCurrentSubscriptions(gymId: number) {
  return authFetch<MemberSubscription[]>(`/api/gyms/${gymId}/subscriptions/current`);
}

export function listPendingSubscriptions(gymId: number) {
  return authFetch<MemberSubscription[]>(`/api/gyms/${gymId}/subscriptions/pending`);
}

export function changeMemberPlan(gymId: number, memberId: number, planId: number) {
  return authFetch<MemberSubscription>(`/api/gyms/${gymId}/members/${memberId}/subscriptions/current/change-plan`, {
    method: "PUT",
    body: JSON.stringify({ planId }),
  });
}
