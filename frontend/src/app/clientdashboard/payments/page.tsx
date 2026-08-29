"use client";

/* Training Ledger page: Payments — this member's full payment history. */
import { useEffect, useState } from "react";
import { StatusPill } from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { listMemberPayments, paymentMethodLabel, type MemberPayment } from "@/lib/memberPayments";

function statusTone(status: MemberPayment["status"]): "lime" | "orange" | "blue" | "ink" {
  switch (status) {
    case "SUCCEEDED":
      return "blue";
    case "PENDING":
      return "orange";
    case "FAILED":
    case "REFUNDED":
      return "ink";
  }
}

function statusLabel(status: MemberPayment["status"]) {
  switch (status) {
    case "SUCCEEDED":
      return "Paid";
    case "PENDING":
      return "Pending";
    case "FAILED":
      return "Failed";
    case "REFUNDED":
      return "Refunded";
  }
}

export default function PaymentsPage() {
  const session = useSession();
  const [payments, setPayments] = useState<MemberPayment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session?.gymId || !session.memberId) return;
    listMemberPayments(session.gymId, session.memberId)
      .then((rows) => setPayments([...rows].sort((a, b) => b.createdAt.localeCompare(a.createdAt))))
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your payments."))
      .finally(() => setLoading(false));
  }, [session?.gymId, session?.memberId]);

  return (
    <div className="page-enter space-y-7">
      {error && <ErrorBanner message={error} />}

      <section className="border border-[#d8d8d1] bg-white">
        <div className="border-b border-[#e7e7e1] p-5">
          <p className="ledger-label">Payment history</p>
          <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">All your payments.</h2>
        </div>

        {loading ? (
          <p className="p-5 text-sm text-[#8a8a82]">Loading…</p>
        ) : payments.length === 0 ? (
          <p className="p-5 text-sm text-[#8a8a82]">No payments recorded yet.</p>
        ) : (
          <div className="divide-y divide-[#ebebe5]">
            {payments.map((payment) => (
              <div className="flex flex-wrap items-center gap-4 px-5 py-4" key={payment.id}>
                <span className="mono w-24 shrink-0 text-xs text-[#8a8a82]">
                  {new Date(payment.paidAt ?? payment.createdAt).toLocaleDateString(undefined, {
                    day: "2-digit",
                    month: "short",
                    year: "numeric",
                  })}
                </span>
                <span className="min-w-0 flex-1 text-sm font-bold">
                  {payment.notes || (payment.subscriptionId ? "Membership plan" : "Payment")}
                </span>
                <span className="text-xs text-[#8a8a82]">{paymentMethodLabel(payment.paymentMethod)}</span>
                <span className="mono text-sm font-bold">
                  {payment.currency} {payment.amount.toFixed(2)}
                </span>
                <StatusPill label={statusLabel(payment.status)} tone={statusTone(payment.status)} />
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
