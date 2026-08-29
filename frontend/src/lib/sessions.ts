import { authFetch } from "@/lib/auth";

export type TrainingSession = {
  id: number;
  gymId: number;
  trainerId: number;
  title: string;
  description: string | null;
  startTime: string;
  endTime: string | null;
  capacity: number | null;
  enrolledCount: number;
};

export type SessionInput = {
  trainerId: number;
  title: string;
  description?: string;
  startTime: string;
  endTime?: string;
  capacity?: number;
};

export function listSessions(gymId: number) {
  return authFetch<TrainingSession[]>(`/api/gyms/${gymId}/sessions`);
}

export function createSession(gymId: number, input: SessionInput) {
  return authFetch<TrainingSession>(`/api/gyms/${gymId}/sessions`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateSession(gymId: number, id: number, input: SessionInput) {
  return authFetch<TrainingSession>(`/api/gyms/${gymId}/sessions/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteSession(gymId: number, id: number) {
  return authFetch<void>(`/api/gyms/${gymId}/sessions/${id}`, {
    method: "DELETE",
  });
}
