import { apiRequest, listPublicPlans, type PublicMembershipPlan } from "@/lib/auth";

export type { PublicMembershipPlan };
export { listPublicPlans };

export type PublicGym = {
  id: number;
  name: string;
  city: string | null;
  upiId: string | null;
  joiningFee: number | null;
  joiningFeeCurrency: string | null;
};

export type PublicRegisterInput = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth?: string;
  gender?: string;
  heightCm?: number;
  weightKg?: number;
  bloodGroup?: string;
  medicalNotes?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  waiverAccepted: boolean;
  waiverSignature: string;
  planId: number;
};

export type PublicRegisterResult = {
  memberId: number;
  subscriptionId: number;
  status: string;
  planName: string;
};

export function getPublicGym(gymId: number) {
  return apiRequest<PublicGym>(`/api/public/gyms/${gymId}`);
}

export function registerPublicMember(gymId: number, input: PublicRegisterInput) {
  return apiRequest<PublicRegisterResult>(`/api/public/gyms/${gymId}/register`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}
