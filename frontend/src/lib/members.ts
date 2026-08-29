import { authFetch, type Gender } from "@/lib/auth";

export type Member = {
  id: number;
  gymId: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string;
  dateOfBirth: string | null;
  gender: Gender | null;
  heightCm: number | null;
  weightKg: number | null;
  goalWeightKg: number | null;
  goalTargetDate: string | null;
  bloodGroup: string | null;
  medicalNotes: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelationship: string | null;
  waiverAccepted: boolean;
  joinDate: string;
  trainerId: number | null;
  trainerFirstName: string | null;
  trainerLastName: string | null;
};

export type MemberInput = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth?: string;
  gender?: Gender;
  heightCm?: number;
  weightKg?: number;
  bloodGroup?: string;
  medicalNotes?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  waiverAccepted: boolean;
  joinDate: string;
};

export function listMembers(gymId: number) {
  return authFetch<Member[]>(`/api/gyms/${gymId}/members`);
}

export function getMember(gymId: number, id: number) {
  return authFetch<Member>(`/api/gyms/${gymId}/members/${id}`);
}

export function createMember(gymId: number, input: MemberInput) {
  return authFetch<Member>(`/api/gyms/${gymId}/members`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateMember(gymId: number, id: number, input: MemberInput) {
  return authFetch<Member>(`/api/gyms/${gymId}/members/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteMember(gymId: number, id: number) {
  return authFetch<void>(`/api/gyms/${gymId}/members/${id}`, {
    method: "DELETE",
  });
}

export function updateMemberWeight(gymId: number, id: number, weightKg: number) {
  return authFetch<Member>(`/api/gyms/${gymId}/members/${id}/weight`, {
    method: "PUT",
    body: JSON.stringify({ weightKg }),
  });
}

export function updateMemberGoal(gymId: number, id: number, goalWeightKg: number, goalTargetDate: string) {
  return authFetch<Member>(`/api/gyms/${gymId}/members/${id}/goal`, {
    method: "PUT",
    body: JSON.stringify({ goalWeightKg, goalTargetDate }),
  });
}

export function assignMemberTrainer(gymId: number, id: number, trainerId: number | null) {
  return authFetch<Member>(`/api/gyms/${gymId}/members/${id}/trainer`, {
    method: "PUT",
    body: JSON.stringify({ trainerId }),
  });
}
