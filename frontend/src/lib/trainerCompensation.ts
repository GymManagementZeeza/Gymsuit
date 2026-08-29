import { authFetch, authFetchOptional } from "@/lib/auth";

export type PayType = "HOURLY" | "SALARY";

export type TrainerCompensation = {
  payType: PayType;
  hourlyRate: number | null;
  monthlySalary: number | null;
  currency: string;
};

export type TrainerCompensationInput = {
  payType: PayType;
  hourlyRate?: number;
  monthlySalary?: number;
  currency: string;
};

export function getTrainerCompensation(gymId: number, trainerId: number) {
  return authFetchOptional<TrainerCompensation>(`/api/gyms/${gymId}/trainers/${trainerId}/compensation`);
}

export function setTrainerCompensation(gymId: number, trainerId: number, input: TrainerCompensationInput) {
  return authFetch<TrainerCompensation>(`/api/gyms/${gymId}/trainers/${trainerId}/compensation`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}
