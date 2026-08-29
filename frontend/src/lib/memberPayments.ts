import { authFetch } from "@/lib/auth";

export type PaymentMethod = "CASH" | "STRIPE" | "RAZORPAY" | "UPI" | "BANK_TRANSFER" | "OTHER";
export type MemberPaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED" | "REFUNDED";

export type MemberPayment = {
  id: number;
  gymId: number;
  memberId: number;
  subscriptionId: number | null;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: MemberPaymentStatus;
  periodStart: string | null;
  periodEnd: string | null;
  gatewayReference: string | null;
  paidAt: string | null;
  notes: string | null;
  createdAt: string;
};

export type ManualPaymentInput = {
  subscriptionId?: number;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  notes?: string;
};

export function recordManualPayment(gymId: number, memberId: number, input: ManualPaymentInput) {
  return authFetch<MemberPayment>(`/api/gyms/${gymId}/members/${memberId}/payments/manual`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listGymPayments(gymId: number) {
  return authFetch<MemberPayment[]>(`/api/gyms/${gymId}/member-payments`);
}

export function listMemberPayments(gymId: number, memberId: number) {
  return authFetch<MemberPayment[]>(`/api/gyms/${gymId}/members/${memberId}/payments`);
}

/** Takes payment for a member's pending plan — activates it in the same call. Amount comes from the plan itself. */
export function takePlanPayment(gymId: number, memberId: number, paymentMethod: PaymentMethod) {
  return authFetch<MemberPayment>(`/api/gyms/${gymId}/members/${memberId}/payments/plan`, {
    method: "POST",
    body: JSON.stringify({ paymentMethod }),
  });
}

export function paymentMethodLabel(method: PaymentMethod) {
  switch (method) {
    case "CASH":
      return "Cash";
    case "UPI":
      return "UPI";
    case "STRIPE":
      return "Card (Stripe)";
    case "RAZORPAY":
      return "Razorpay";
    case "BANK_TRANSFER":
      return "Bank transfer";
    case "OTHER":
      return "Other";
  }
}
