import { authFetch } from "@/lib/auth";

export type SessionRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type SessionRequest = {
  id: number;
  gymId: number;
  memberId: number;
  trainerId: number;
  requestedStartTime: string;
  requestedEndTime: string | null;
  notes: string | null;
  status: SessionRequestStatus;
  createdAt: string;
  respondedAt: string | null;
  responseNote: string | null;
  sessionId: number | null;
};

export type SessionRequestInput = {
  trainerId: number;
  requestedStartTime: string;
  requestedEndTime?: string;
  notes?: string;
};

export function listSessionRequests(gymId: number) {
  return authFetch<SessionRequest[]>(`/api/gyms/${gymId}/session-requests`);
}

export function createSessionRequest(gymId: number, input: SessionRequestInput) {
  return authFetch<SessionRequest>(`/api/gyms/${gymId}/session-requests`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function approveSessionRequest(gymId: number, id: number) {
  return authFetch<SessionRequest>(`/api/gyms/${gymId}/session-requests/${id}/approve`, {
    method: "PUT",
  });
}

export function rejectSessionRequest(gymId: number, id: number, reason?: string) {
  return authFetch<SessionRequest>(`/api/gyms/${gymId}/session-requests/${id}/reject`, {
    method: "PUT",
    body: JSON.stringify({ reason }),
  });
}

export function cancelSessionRequest(gymId: number, id: number) {
  return authFetch<SessionRequest>(`/api/gyms/${gymId}/session-requests/${id}/cancel`, {
    method: "PUT",
  });
}
