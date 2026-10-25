import { authFetch } from "@/lib/auth";

export type SuggestedExerciseInput = {
  exerciseName: string;
  sets: number;
  reps: number;
  weightKg: number;
};

export type SendTodayWorkoutInput = {
  message?: string;
  exercises: SuggestedExerciseInput[];
};

/** Trainer sends today's workout to one of their clients (push + email go out from the backend). */
export function sendTodayWorkout(
  gymId: number,
  trainerId: number,
  memberId: number,
  input: SendTodayWorkoutInput
) {
  return authFetch<void>(
    `/api/gyms/${gymId}/trainers/${trainerId}/clients/${memberId}/workout-suggestions`,
    {
      method: "POST",
      body: JSON.stringify(input),
    }
  );
}
