import { authFetch } from "@/lib/auth";

export type CheckIn = {
  id: number;
  gymId: number;
  memberId: number;
  checkInTime: string;
  checkOutTime: string | null;
};

export function checkIn(gymId: number, memberId: number) {
  return authFetch<CheckIn>(`/api/gyms/${gymId}/members/${memberId}/checkin`, {
    method: "POST",
  });
}

export function checkOut(gymId: number, memberId: number) {
  return authFetch<CheckIn>(`/api/gyms/${gymId}/members/${memberId}/checkout`, {
    method: "POST",
  });
}

export function listCheckInHistory(gymId: number, memberId: number) {
  return authFetch<CheckIn[]>(`/api/gyms/${gymId}/members/${memberId}/checkins`);
}

export function listActiveCheckIns(gymId: number) {
  return authFetch<CheckIn[]>(`/api/gyms/${gymId}/checkins/active`);
}

export function listCheckInsSince(gymId: number, since: Date) {
  const iso = new Date(since.getTime() - since.getTimezoneOffset() * 60000).toISOString().slice(0, 19);
  return authFetch<CheckIn[]>(`/api/gyms/${gymId}/checkins?since=${encodeURIComponent(iso)}`);
}

export function forceCheckOut(gymId: number, checkInId: number) {
  return authFetch<CheckIn>(`/api/gyms/${gymId}/checkins/${checkInId}/force-checkout`, {
    method: "POST",
  });
}
