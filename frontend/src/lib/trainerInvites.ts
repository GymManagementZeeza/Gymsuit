import { API_BASE_URL, authFetch } from "@/lib/auth";

export type TrainerInviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";

export type TrainerInvite = {
  id: number;
  email: string;
  firstName: string | null;
  lastName: string | null;
  status: TrainerInviteStatus;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
};

export type TrainerInvitePreview = {
  gymName: string;
  trainerName: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  status: TrainerInviteStatus;
  expiresAt: string;
  existingMember: boolean;
};

export function inviteClient(gymId: number, trainerId: number, input: { email: string; firstName?: string; lastName?: string }) {
  return authFetch<TrainerInvite>(`/api/gyms/${gymId}/trainers/${trainerId}/invites`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listTrainerInvites(gymId: number, trainerId: number) {
  return authFetch<TrainerInvite[]>(`/api/gyms/${gymId}/trainers/${trainerId}/invites`);
}

export function revokeTrainerInvite(gymId: number, trainerId: number, inviteId: number) {
  return authFetch<void>(`/api/gyms/${gymId}/trainers/${trainerId}/invites/${inviteId}`, {
    method: "DELETE",
  });
}

async function publicRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

export function previewTrainerInvite(token: string) {
  return publicRequest<TrainerInvitePreview>(`/api/trainer-invites/${encodeURIComponent(token)}`);
}

export function acceptTrainerInvite(
  token: string,
  input: { firstName?: string; lastName?: string; phone?: string; dateOfBirth?: string; gender?: string; heightCm?: number; weightKg?: number; waiverAccepted?: boolean },
) {
  return publicRequest<{ message: string }>(`/api/trainer-invites/${encodeURIComponent(token)}/accept`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}
