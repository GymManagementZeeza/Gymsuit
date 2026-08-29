import { authFetch } from "@/lib/auth";
import type { PaymentMethod } from "@/lib/memberPayments";

export type TransactionDirection = "INCOME" | "EXPENSE";

export type GymTransactionRecord = {
  id: number;
  gymId: number;
  memberId: number | null;
  trainerId: number | null;
  direction: TransactionDirection;
  description: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  occurredOn: string;
  notes: string | null;
  createdAt: string;
};

export type TransactionInput = {
  direction: TransactionDirection;
  description: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  memberId?: number;
  trainerId?: number;
  occurredOn: string;
  notes?: string;
};

export function listTransactions(gymId: number) {
  return authFetch<GymTransactionRecord[]>(`/api/gyms/${gymId}/transactions`);
}

export function recordTransaction(gymId: number, input: TransactionInput) {
  return authFetch<GymTransactionRecord>(`/api/gyms/${gymId}/transactions`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}
