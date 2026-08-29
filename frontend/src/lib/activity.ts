import { authFetch } from "@/lib/auth";

export type ActivityType =
  | "MEMBER_JOINED"
  | "MEMBER_REMOVED"
  | "PAYMENT_RECEIVED"
  | "PLAN_ASSIGNED"
  | "PLAN_CHANGED"
  | "SUBSCRIPTION_CANCELLED"
  | "INCOME_RECORDED"
  | "EXPENSE_RECORDED"
  | "NOTIFICATION_SENT";

export type GymActivity = {
  id: number;
  type: ActivityType;
  message: string;
  createdAt: string;
};

export function listRecentActivity(gymId: number) {
  return authFetch<GymActivity[]>(`/api/gyms/${gymId}/activity`);
}
