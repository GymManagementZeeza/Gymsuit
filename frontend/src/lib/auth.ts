export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "https://api.gymsuit.app";

export type LoginResponse = {
  token: string;
  role: "ADMIN" | "OWNER" | "MANAGER" | "TRAINER" | "MEMBER";
  gymId: number | null;
  trainerId: number | null;
  memberId: number | null;
  managerId: number | null;
};

const STORAGE_KEY = "gymsuite_auth";

export function saveSession(session: LoginResponse) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function getSession(): LoginResponse | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? (JSON.parse(raw) as LoginResponse) : null;
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
}

export function dashboardPathForRole(role: LoginResponse["role"]): string {
  switch (role) {
    case "TRAINER":
      return "/trainerdashboard";
    case "MEMBER":
      return "/clientdashboard";
    default:
      return "/dashboard";
  }
}

type ApiError = {
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>;
};

async function throwApiError(res: Response): Promise<never> {
  const body = (await res.json().catch(() => null)) as ApiError | null;
  throw new Error(body?.message ?? `Request failed (${res.status})`);
}

export class AuthRequiredError extends Error {
  constructor() {
    super("Not authenticated");
    this.name = "AuthRequiredError";
  }
}

// For endpoints that attach the signed-in user's JWT — redirects to /login on missing/expired session.
export async function authFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const session = getSession();
  if (!session) {
    throw new AuthRequiredError();
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${session.token}`,
      ...(options.headers ?? {}),
    },
  });

  if (res.status === 401) {
    clearSession();
    throw new AuthRequiredError();
  }
  if (!res.ok) {
    return throwApiError(res);
  }
  if (res.status === 204 || res.status === 202) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

// Like authFetch, but a 404 resolves to null instead of throwing — for "does this exist" lookups.
export async function authFetchOptional<T>(path: string): Promise<T | null> {
  const session = getSession();
  if (!session) {
    throw new AuthRequiredError();
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${session.token}` },
  });

  if (res.status === 404) {
    return null;
  }
  if (res.status === 401) {
    clearSession();
    throw new AuthRequiredError();
  }
  if (!res.ok) {
    return throwApiError(res);
  }
  return (await res.json()) as T;
}

async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
  });

  if (!res.ok) {
    return throwApiError(res);
  }

  if (res.status === 204 || res.status === 202) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export function checkEmailExists(email: string) {
  return apiRequest<{ exists: boolean }>("/api/auth/email/check", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function requestOtp(email: string) {
  return apiRequest<void>("/api/auth/otp/request", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function verifyOtp(email: string, otp: string) {
  return apiRequest<LoginResponse>("/api/auth/otp/verify", {
    method: "POST",
    body: JSON.stringify({ email, otp }),
  });
}

export type GymSearchResult = {
  id: number;
  name: string;
  city: string | null;
  state: string | null;
};

export function searchGyms(query: string) {
  return apiRequest<GymSearchResult[]>(`/api/gyms/search?q=${encodeURIComponent(query)}`);
}

export type OwnerRegisterPayload = {
  email: string;
  gymName: string;
  addressLine?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  gymPhone?: string;
  firstName: string;
  lastName: string;
  phone?: string;
};

export function registerOwner(payload: OwnerRegisterPayload) {
  return apiRequest<LoginResponse>("/api/auth/register/owner", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export type Gender = "MALE" | "FEMALE" | "OTHER";

export type MemberRegisterPayload = {
  email: string;
  gymId: number;
  planId: number;
  firstName: string;
  lastName: string;
  phone: string;
  dateOfBirth?: string;
  gender?: Gender;
  waiverAccepted: boolean;
};

export function registerMember(payload: MemberRegisterPayload) {
  return apiRequest<LoginResponse>("/api/auth/register/member", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export type PublicMembershipPlan = {
  id: number;
  gymId: number;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  billingCycle: string;
  active: boolean;
};

export function listPublicPlans(gymId: number) {
  return apiRequest<PublicMembershipPlan[]>(`/api/auth/register/member/plans/${gymId}`);
}
