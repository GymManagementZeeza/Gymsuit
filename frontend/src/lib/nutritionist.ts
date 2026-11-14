import { API_BASE_URL, authFetch } from "@/lib/auth";

export type InviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";

export type NutritionClient = {
  memberId: number;
  gymId: number | null;
  firstName: string;
  lastName: string;
  email: string | null;
  weightKg: number | null;
  goalWeightKg: number | null;
  linkedAt: string;
};

export type NutritionInvite = {
  id: number;
  email: string;
  firstName: string | null;
  lastName: string | null;
  status: InviteStatus;
  createdAt: string;
  expiresAt: string;
};

export type NutritionInvitePreview = {
  nutritionistName: string;
  email: string;
  status: InviteStatus;
  expiresAt: string;
  hasAccount: boolean;
  staffAccount: boolean;
};

export function listNutritionClients() {
  return authFetch<NutritionClient[]>("/api/nutritionist/clients");
}

export function removeNutritionClient(memberId: number) {
  return authFetch<void>(`/api/nutritionist/clients/${memberId}`, { method: "DELETE" });
}

export function listNutritionInvites() {
  return authFetch<NutritionInvite[]>("/api/nutritionist/invites");
}

export function sendNutritionInvite(input: { email: string; firstName?: string; lastName?: string }) {
  return authFetch<NutritionInvite>("/api/nutritionist/invites", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function revokeNutritionInvite(inviteId: number) {
  return authFetch<void>(`/api/nutritionist/invites/${inviteId}`, { method: "DELETE" });
}

async function publicRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as { message?: string } | null;
    throw new Error(body?.message ?? `Request failed (${res.status})`);
  }
  return (await res.json()) as T;
}

/** Public — the token from the emailed link is the credential. */
export function previewNutritionInvite(token: string) {
  return publicRequest<NutritionInvitePreview>(`/api/nutritionist-invites/${encodeURIComponent(token)}`);
}

/** Accepts an invite. `signup` is required only when the invited email has no GymSuit account yet. */
export function acceptNutritionInvite(
  token: string,
  signup?: { firstName: string; lastName: string; phone: string },
) {
  return publicRequest<{ nutritionistName: string }>(
    `/api/nutritionist-invites/${encodeURIComponent(token)}/accept`,
    { method: "POST", body: signup ? JSON.stringify(signup) : undefined },
  );
}

export type MealPlan = {
  id: number;
  date: string;
  nutritionistName: string;
  breakfast: string | null;
  lunch: string | null;
  eveningSnack: string | null;
  dinner: string | null;
  note: string | null;
  updatedAt: string;
};

export type MealPlanInput = {
  date: string;
  breakfast?: string;
  lunch?: string;
  eveningSnack?: string;
  dinner?: string;
  note?: string;
};

export function listMealPlans(memberId: number) {
  return authFetch<MealPlan[]>(`/api/nutritionist/clients/${memberId}/meal-plans`);
}

/** Saves the plan (same day replaces it) and emails + notifies the client. */
export function sendMealPlan(memberId: number, input: MealPlanInput) {
  return authFetch<MealPlan>(`/api/nutritionist/clients/${memberId}/meal-plans`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}
