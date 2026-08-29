import { authFetch } from "@/lib/auth";

export type WeightLog = {
  id: number;
  weightKg: number;
  recordedAt: string;
};

export function listWeightHistory(gymId: number, memberId: number) {
  return authFetch<WeightLog[]>(`/api/gyms/${gymId}/members/${memberId}/weight-history`);
}
