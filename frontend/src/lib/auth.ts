export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "https://api.gymsuit.app";

export type LoginResponse = {
  token: string;
  role: "ADMIN" | "OWNER" | "MANAGER" | "TRAINER" | "MEMBER";
  gymId: number | null;
  trainerId: number | null;
  memberId: number | null;
  managerId: number | null;
  /** Present on fresh logins; the web client authenticates refresh via the httpOnly cookie instead. */
  refreshToken?: string;
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

/** True when the access token is expired or expires within the next minute. */
function isAccessTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split(".")[1] ?? "")) as { exp?: number };
    if (typeof payload.exp !== "number") return true;
    return payload.exp * 1000 <= Date.now() + 60_000;
  } catch {
    return true;
  }
}

// Single in-flight refresh shared by concurrent requests, so a burst of 401s
// triggers exactly one token rotation.
let refreshPromise: Promise<LoginResponse | null> | null = null;

function doRefresh(): Promise<LoginResponse | null> {
  return fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: "POST",
    credentials: "include",
  }).then(async (res) => {
    if (!res.ok) return null;
    return (await res.json()) as LoginResponse;
  }).catch(() => null);
}

/**
 * Silently renews the access token using the httpOnly refresh cookie.
 * Returns the updated session, or null when the refresh token is gone/expired.
 */
export function refreshSession(): Promise<LoginResponse | null> {
  if (!refreshPromise) {
    refreshPromise = doRefresh().then((session) => {
      refreshPromise = null;
      if (session) {
        // Preserve the stored profile fields; only the tokens rotate.
        const current = getSession();
        saveSession({ ...(current ?? session), ...session });
      } else {
        clearSession();
      }
      return session;
    });
  }
  return refreshPromise;
}

/** Ensures the stored access token is usable, refreshing it first when expired. */
async function ensureFreshSession(): Promise<LoginResponse> {
  const session = getSession();
  if (!session) {
    throw new AuthRequiredError();
  }
  if (!isAccessTokenExpired(session.token)) {
    return session;
  }
  const refreshed = await refreshSession();
  if (!refreshed) {
    throw new AuthRequiredError();
  }
  return refreshed;
}

function authHeaders(session: LoginResponse, options: RequestInit): HeadersInit {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${session.token}`,
    ...(options.headers ?? {}),
  };
}

/**
 * Performs an authenticated request, transparently refreshing the access token
 * once when it has expired (or the server rejects it with 401). Only when the
 * refresh token itself is invalid does this clear the session and throw
 * AuthRequiredError — so the user stays signed in across access-token expiry.
 */
async function fetchWithAuth(path: string, options: RequestInit = {}): Promise<Response> {
  let session = await ensureFreshSession();

  let res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: authHeaders(session, options),
  });

  if (res.status === 401) {
    const refreshed = await refreshSession();
    if (!refreshed) {
      throw new AuthRequiredError();
    }
    session = refreshed;
    res = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: authHeaders(session, options),
    });
    if (res.status === 401) {
      clearSession();
      throw new AuthRequiredError();
    }
  }
  return res;
}

// For endpoints that attach the signed-in user's JWT.
export async function authFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetchWithAuth(path, options);
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
  const res = await fetchWithAuth(path);
  if (res.status === 404) {
    return null;
  }
  if (!res.ok) {
    return throwApiError(res);
  }
  return (await res.json()) as T;
}

/** Signs out everywhere: revokes the server refresh token, then clears the local session. */
export async function logout(): Promise<void> {
  try {
    await fetch(`${API_BASE_URL}/api/auth/logout`, {
      method: "POST",
      credentials: "include",
    });
  } catch {
    // Best effort — the local session is cleared regardless.
  }
  refreshPromise = null;
  clearSession();
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

/**
 * For calls that issue a session (login / OTP verify / register / Google):
 * `credentials: "include"` lets the browser store the httpOnly refresh cookie
 * the backend sets on the response.
 */
function sessionRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  return apiRequest<T>(path, { ...options, credentials: "include" });
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
  return sessionRequest<LoginResponse>("/api/auth/otp/verify", {
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
  return sessionRequest<LoginResponse>("/api/auth/register/owner", {
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
  return sessionRequest<LoginResponse>("/api/auth/register/member", {
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
