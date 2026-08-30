import { authFetch, authFetchOptional } from "@/lib/auth";

export type PayType = "HOURLY" | "SALARY";

export type ManagerCompensation = {
  payType: PayType;
  hourlyRate: number | null;
  monthlySalary: number | null;
  currency: string;
};

export type ManagerCompensationInput = {
  payType: PayType;
  hourlyRate?: number;
  monthlySalary?: number;
  currency: string;
};

export function getManagerCompensation(gymId: number, managerId: number) {
  return authFetchOptional<ManagerCompensation>(`/api/gyms/${gymId}/managers/${managerId}/compensation`);
}

export function setManagerCompensation(gymId: number, managerId: number, input: ManagerCompensationInput) {
  return authFetch<ManagerCompensation>(`/api/gyms/${gymId}/managers/${managerId}/compensation`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}
