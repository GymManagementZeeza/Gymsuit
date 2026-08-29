"use client";

/* Training Ledger page: membership payment history. */
import { useCallback, useEffect, useMemo, useState } from "react";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, StatusPill } from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import RecordTransactionModal from "@/components/dashboard/RecordTransactionModal";
import MonthlyReportModal from "@/components/dashboard/MonthlyReportModal";
import { useSession } from "@/hooks/useSession";
import { listMembers, type Member } from "@/lib/members";
import { listGymPayments, paymentMethodLabel, type MemberPayment } from "@/lib/memberPayments";
import { listTransactions, type GymTransactionRecord } from "@/lib/transactions";
import { getGym, type Gym } from "@/lib/gyms";
import { ArrowUpRight, Banknote, Clock3, Plus, Printer, TrendingDown } from "lucide-react";

function statusTone(status: MemberPayment["status"]): "lime" | "orange" | "blue" {
  if (status === "SUCCEEDED") return "lime";
  if (status === "PENDING") return "orange";
  return "blue";
}

function statusLabel(status: MemberPayment["status"]) {
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export default function PaymentsPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [payments, setPayments] = useState<MemberPayment[]>([]);
  const [transactions, setTransactions] = useState<GymTransactionRecord[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [gym, setGym] = useState<Gym | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [recording, setRecording] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);

  const loadAll = useCallback(() => {
    if (!gymId) return;
    Promise.all([listGymPayments(gymId), listTransactions(gymId), listMembers(gymId), getGym(gymId)])
      .then(([paymentData, transactionData, memberData, gymData]) => {
        setPayments(paymentData);
        setTransactions(transactionData);
        setMembers(memberData);
        setGym(gymData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load payments."))
      .finally(() => setLoading(false));
  }, [gymId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const memberNames = useMemo(() => {
    const map: Record<number, string> = {};
    for (const m of members) map[m.id] = `${m.firstName} ${m.lastName}`;
    return map;
  }, [members]);

  const metrics = useMemo(() => {
    const now = new Date();
    const isThisMonth = (dateStr: string | null) => {
      if (!dateStr) return false;
      const d = new Date(dateStr);
      return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
    };

    const collectedThisMonth = payments.filter((p) => p.status === "SUCCEEDED" && isThisMonth(p.paidAt));
    const cashThisMonth = collectedThisMonth.filter((p) => p.paymentMethod === "CASH");
    const pending = payments.filter((p) => p.status === "PENDING");

    const sum = (list: MemberPayment[]) => list.reduce((total, p) => total + Number(p.amount), 0);
    const currency = payments[0]?.currency ?? "INR";

    return {
      collectedThisMonth: { total: sum(collectedThisMonth), count: collectedThisMonth.length, currency },
      cashThisMonth: { total: sum(cashThisMonth), count: cashThisMonth.length, currency },
      pending: { total: sum(pending), count: pending.length, currency },
    };
  }, [payments]);

  const sortedPayments = useMemo(
    () =>
      [...payments].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      ),
    [payments]
  );

  const sortedTransactions = useMemo(
    () =>
      [...transactions].sort(
        (a, b) => new Date(b.occurredOn).getTime() - new Date(a.occurredOn).getTime()
      ),
    [transactions]
  );

  const transactionMetrics = useMemo(() => {
    const now = new Date();
    const isThisMonth = (dateStr: string) => {
      const d = new Date(dateStr);
      return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
    };
    const thisMonth = transactions.filter((t) => isThisMonth(t.occurredOn));
    const income = thisMonth.filter((t) => t.direction === "INCOME");
    const expense = thisMonth.filter((t) => t.direction === "EXPENSE");
    const sum = (list: GymTransactionRecord[]) => list.reduce((total, t) => total + Number(t.amount), 0);
    const currency = transactions[0]?.currency ?? "INR";
    return {
      income: { total: sum(income), count: income.length, currency },
      expense: { total: sum(expense), count: expense.length, currency },
    };
  }, [transactions]);

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow="Finance · Payments"
        title="Every payment, accounted for."
        description="Collected membership payments and what's still outstanding."
        actions={
          <>
            <button
              type="button"
              onClick={() => setReportOpen(true)}
              className="flex items-center gap-2 border border-[#d8d8d1] bg-white px-3.5 py-2.5 text-sm font-bold text-[#24241f] transition hover:border-[#24241f]"
            >
              <Printer className="size-4" /> Print report
            </button>
            <ActionButton icon={<Plus className="size-4" />} onClick={() => setRecording(true)}>
              Record payment
            </ActionButton>
          </>
        }
      />

      <section className="grid gap-4 md:grid-cols-3">
        <MetricCard
          label="Collected this month"
          value={`${metrics.collectedThisMonth.currency} ${metrics.collectedThisMonth.total.toFixed(2)}`}
          detail={`${metrics.collectedThisMonth.count} payment${metrics.collectedThisMonth.count === 1 ? "" : "s"}`}
          tone="lime"
          icon={<ArrowUpRight className="size-5" />}
        />
        <MetricCard
          label="Cash this month"
          value={`${metrics.cashThisMonth.currency} ${metrics.cashThisMonth.total.toFixed(2)}`}
          detail={`${metrics.cashThisMonth.count} payment${metrics.cashThisMonth.count === 1 ? "" : "s"}`}
          tone="paper"
          icon={<Banknote className="size-5" />}
        />
        <MetricCard
          label="Pending"
          value={`${metrics.pending.currency} ${metrics.pending.total.toFixed(2)}`}
          detail={`${metrics.pending.count} payment${metrics.pending.count === 1 ? "" : "s"}`}
          tone="orange"
          icon={<Clock3 className="size-5" />}
        />
      </section>

      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
          <div>
            <p className="ledger-label">Payment history</p>
            <h2 className="mt-2 text-xl font-bold">
              {loading ? "Loading payments…" : `${payments.length} payment${payments.length === 1 ? "" : "s"}`}
            </h2>
          </div>
        </div>

        {error && (
          <div className="p-5">
            <ErrorBanner message={error} />
          </div>
        )}

        {!loading && !error && payments.length === 0 && (
          <p className="p-8 text-center text-sm text-[#76766f]">
            No payments recorded yet — they&apos;ll show up here once collected.
          </p>
        )}

        {sortedPayments.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[670px] text-left">
              <thead className="border-b border-[#e7e7e1] bg-[#fafaf6]">
                <tr className="text-[10px] uppercase tracking-[0.12em] text-[#75756e]">
                  <th className="px-5 py-3">Member</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Method</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#ededE7]">
                {sortedPayments.map((payment) => (
                  <tr key={payment.id}>
                    <td className="px-5 py-4">
                      <p className="text-sm font-bold">{memberNames[payment.memberId] ?? `Member #${payment.memberId}`}</p>
                      {payment.notes && <p className="mt-1 text-[10px] text-[#7a7a73]">{payment.notes}</p>}
                    </td>
                    <td className="mono px-4 py-4 text-sm font-bold">
                      {payment.currency} {Number(payment.amount).toFixed(2)}
                    </td>
                    <td className="px-4 py-4 text-xs text-[#696962]">{paymentMethodLabel(payment.paymentMethod)}</td>
                    <td className="px-4 py-4">
                      <StatusPill label={statusLabel(payment.status)} tone={statusTone(payment.status)} />
                    </td>
                    <td className="px-4 py-4 text-xs text-[#696962]">
                      {new Date(payment.paidAt ?? payment.createdAt).toLocaleDateString(undefined, {
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        <MetricCard
          label="Other income this month"
          value={`${transactionMetrics.income.currency} ${transactionMetrics.income.total.toFixed(2)}`}
          detail={`${transactionMetrics.income.count} entr${transactionMetrics.income.count === 1 ? "y" : "ies"}`}
          tone="lime"
          icon={<ArrowUpRight className="size-5" />}
        />
        <MetricCard
          label="Expenses this month"
          value={`${transactionMetrics.expense.currency} ${transactionMetrics.expense.total.toFixed(2)}`}
          detail={`${transactionMetrics.expense.count} entr${transactionMetrics.expense.count === 1 ? "y" : "ies"}`}
          tone="orange"
          icon={<TrendingDown className="size-5" />}
        />
      </section>

      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
          <div>
            <p className="ledger-label">Other income &amp; expenses</p>
            <h2 className="mt-2 text-xl font-bold">
              {loading
                ? "Loading…"
                : `${transactions.length} entr${transactions.length === 1 ? "y" : "ies"}`}
            </h2>
          </div>
        </div>

        {!loading && !error && transactions.length === 0 && (
          <p className="p-8 text-center text-sm text-[#76766f]">
            No miscellaneous income or expenses recorded yet — rent, repairs, parking, anything outside membership
            plans.
          </p>
        )}

        {sortedTransactions.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[670px] text-left">
              <thead className="border-b border-[#e7e7e1] bg-[#fafaf6]">
                <tr className="text-[10px] uppercase tracking-[0.12em] text-[#75756e]">
                  <th className="px-5 py-3">Description</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Method</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#ededE7]">
                {sortedTransactions.map((transaction) => (
                  <tr key={transaction.id}>
                    <td className="px-5 py-4">
                      <p className="text-sm font-bold">{transaction.description}</p>
                      {(transaction.memberId || transaction.notes) && (
                        <p className="mt-1 text-[10px] text-[#7a7a73]">
                          {transaction.memberId ? memberNames[transaction.memberId] ?? `Member #${transaction.memberId}` : ""}
                          {transaction.memberId && transaction.notes ? " · " : ""}
                          {transaction.notes ?? ""}
                        </p>
                      )}
                    </td>
                    <td
                      className={`mono px-4 py-4 text-sm font-bold ${
                        transaction.direction === "EXPENSE" ? "text-red-700" : "text-[#3f7a1f]"
                      }`}
                    >
                      {transaction.direction === "EXPENSE" ? "−" : "+"}
                      {transaction.currency} {Number(transaction.amount).toFixed(2)}
                    </td>
                    <td className="px-4 py-4 text-xs text-[#696962]">{paymentMethodLabel(transaction.paymentMethod)}</td>
                    <td className="px-4 py-4">
                      <StatusPill
                        label={transaction.direction === "INCOME" ? "Income" : "Expense"}
                        tone={transaction.direction === "INCOME" ? "lime" : "orange"}
                      />
                    </td>
                    <td className="px-4 py-4 text-xs text-[#696962]">
                      {new Date(transaction.occurredOn).toLocaleDateString(undefined, {
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {recording && gymId && (
        <RecordTransactionModal
          gymId={gymId}
          members={members}
          onClose={() => setRecording(false)}
          onSaved={() => {
            setRecording(false);
            loadAll();
          }}
        />
      )}

      {reportOpen && (
        <MonthlyReportModal
          gym={gym}
          payments={payments}
          transactions={transactions}
          memberNames={memberNames}
          onClose={() => setReportOpen(false)}
        />
      )}
    </div>
  );
}
