import { authFetch } from "@/lib/auth";

export type Gym = {
  id: number;
  name: string;
  addressLine: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  phone: string | null;
  email: string | null;
  logoUrl: string | null;
  upiId: string | null;
};

export function getGym(gymId: number) {
  return authFetch<Gym>(`/api/gyms/${gymId}`);
}

export type GymInput = {
  name: string;
  addressLine?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  upiId?: string;
};

/** Builds a full GymInput from an existing Gym, so a PUT never wipes out fields it isn't changing. */
export function gymToInput(gym: Gym): GymInput {
  return {
    name: gym.name,
    addressLine: gym.addressLine ?? undefined,
    city: gym.city ?? undefined,
    state: gym.state ?? undefined,
    postalCode: gym.postalCode ?? undefined,
    country: gym.country ?? undefined,
    phone: gym.phone ?? undefined,
    email: gym.email ?? undefined,
    logoUrl: gym.logoUrl ?? undefined,
    upiId: gym.upiId ?? undefined,
  };
}

export function updateGym(gymId: number, input: GymInput) {
  return authFetch<Gym>(`/api/gyms/${gymId}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}
