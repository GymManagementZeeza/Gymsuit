import { authFetch, authFetchBlob } from "@/lib/auth";

export type IntakeEntry = {
  externalId: string;
  meal: string | null;
  dishName: string | null;
  calories: number | null;
  proteinG: number | null;
  carbsG: number | null;
  fatG: number | null;
  hasPhoto: boolean;
};

export type ProgressDay = {
  date: string;
  steps: number | null;
  activeCalories: number | null;
  totalCalories: number | null;
  distanceMeters: number | null;
  latestHeartRateBpm: number | null;
  restingHeartRateBpm: number | null;
  minHeartRateBpm: number | null;
  maxHeartRateBpm: number | null;
  sleepDurationMinutes: number | null;
  sleepStartTime: string | null;
  sleepEndTime: string | null;
  weightKg: number | null;
  sourceDevice: string | null;
  intakeCalories: number | null;
  intakeProteinG: number | null;
  intakeCarbsG: number | null;
  intakeFatG: number | null;
  intakeEntries: IntakeEntry[];
  workoutCount: number | null;
  workoutCalories: number | null;
};

export type ProgressWorkout = {
  title: string;
  startTime: string;
  endTime: string;
  durationMinutes: number | null;
  caloriesBurned: number | null;
  setCount: number | null;
  totalReps: number | null;
  totalVolumeKg: number | null;
};

export type ClientProgress = {
  memberId: number;
  firstName: string;
  lastName: string;
  email: string | null;
  hasAppAccount: boolean;
  hasAnyRecords: boolean;
  from: string;
  to: string;
  days: ProgressDay[];
  workouts: ProgressWorkout[];
};

export function getClientProgress(gymId: number, memberId: number, from: string, to: string) {
  return authFetch<ClientProgress>(
    `/api/gyms/${gymId}/members/${memberId}/progress?from=${from}&to=${to}`,
  );
}

/** Fetches one meal photo as a blob URL for an <img> tag (the endpoint needs auth, so a plain <img src> cannot be used). */
export async function getClientIntakePhotoUrl(gymId: number, memberId: number, externalId: string) {
  const blob = await authFetchBlob(
    `/api/gyms/${gymId}/members/${memberId}/progress/photo/${externalId}`,
  );
  return blob ? URL.createObjectURL(blob) : null;
}
