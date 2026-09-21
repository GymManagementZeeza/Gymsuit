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

function lastNMonthKeys(n: number) {
  const now = new Date();
  const keys: string[] = [];
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    keys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
  }
  return keys;
}

function monthAbbrev(monthKey: string) {
  return new Date(`${monthKey}-01T00:00:00`).toLocaleDateString(undefined, { month: "short" }).toUpperCase();
}

function monthFull(monthKey: string) {
  return new Date(`${monthKey}-01T00:00:00`).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function smoothPath(points: { x: number; y: number }[]) {
  if (points.length < 2) return "";
  let d = `M${points[0].x.toFixed(1)},${points[0].y.toFixed(1)}`;
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i - 1] ?? points[i];
    const p1 = points[i];
    const p2 = points[i + 1];
    const p3 = points[i + 2] ?? p2;
    const c1x = p1.x + (p2.x - p0.x) / 6;
    const c1y = p1.y + (p2.y - p0.y) / 6;
    const c2x = p2.x - (p3.x - p1.x) / 6;
    const c2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C${c1x.toFixed(1)},${c1y.toFixed(1)} ${c2x.toFixed(1)},${c2y.toFixed(1)} ${p2.x.toFixed(1)},${p2.y.toFixed(1)}`;
  }
  return d;
}

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
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

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

  const chartData = useMemo(() => {
    const months = lastNMonthKeys(6);
    const incomeMap = new Map<string, number>();
    const expenseMap = new Map<string, number>();

    for (const m of months) {
      incomeMap.set(m, 0);
      expenseMap.set(m, 0);
    }

    for (const p of payments) {
      if (p.status !== "SUCCEEDED") continue;
      const dateStr = p.paidAt ?? p.createdAt;
      const key = dateStr.slice(0, 7);
      if (incomeMap.has(key)) {
        incomeMap.set(key, (incomeMap.get(key) ?? 0) + Number(p.amount));
      }
    }

    for (const t of transactions) {
      const key = t.occurredOn.slice(0, 7);
      if (t.direction === "INCOME" && incomeMap.has(key)) {
        incomeMap.set(key, (incomeMap.get(key) ?? 0) + Number(t.amount));
      } else if (t.direction === "EXPENSE" && expenseMap.has(key)) {
        expenseMap.set(key, (expenseMap.get(key) ?? 0) + Number(t.amount));
      }
    }

    return months.map((month) => ({
      month,
      income: incomeMap.get(month) ?? 0,
      expense: expenseMap.get(month) ?? 0,
    }));
  }, [payments, transactions]);

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

  const currency = payments[0]?.currency ?? transactions[0]?.currency ?? "INR";

  const CHART_WIDTH = 640;
  const CHART_HEIGHT = 200;
  const PAD_TOP = 24;
  const PAD_BOTTOM = 32;
  const PAD_LEFT = 54;
  const PAD_RIGHT = 24;

  const plotWidth = CHART_WIDTH - PAD_LEFT - PAD_RIGHT;
  const plotHeight = CHART_HEIGHT - PAD_TOP - PAD_BOTTOM;

  const maxVal = Math.max(
    1,
    ...chartData.map((d) => d.income),
    ...chartData.map((d) => d.expense)
  );

  const incomePoints = chartData.map((d, index) => ({
    x: PAD_LEFT + (index / (chartData.length - 1)) * plotWidth,
    y: PAD_TOP + plotHeight - (d.income / maxVal) * plotHeight,
    val: d.income,
    month: d.month,
  }));

  const expensePoints = chartData.map((d, index) => ({
    x: PAD_LEFT + (index / (chartData.length - 1)) * plotWidth,
    y: PAD_TOP + plotHeight - (d.expense / maxVal) * plotHeight,
    val: d.expense,
    month: d.month,
  }));

  const incomePath = smoothPath(incomePoints);
  const expensePath = smoothPath(expensePoints);

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

      <section className="border border-[#d8d8d1] bg-white p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-[#e7e7e1] pb-4 mb-5">
          <div>
            <p className="ledger-label">Income &amp; Expense Trends</p>
            <h2 className="mt-1 text-lg font-bold tracking-[-0.02em]">Financial Overview (Last 6 Months)</h2>
          </div>
          <div className="flex items-center gap-6 text-xs font-bold">
            <div className="flex items-center gap-2">
              <span className="inline-block size-3 rounded-full bg-[#16a34a]" />
              <span className="text-[#16a34a]">Income (Green)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-block size-3 rounded-full bg-[#dc2626]" />
              <span className="text-[#dc2626]">Expense (Red)</span>
            </div>
          </div>
        </div>

        {loading ? (
          <div className="flex h-[200px] items-center justify-center text-sm text-[#8a8a82]">Loading financial trends…</div>
        ) : (
          <div className="relative">
            <svg
              viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
              className="w-full overflow-visible"
              aria-label="Income and expense line graph"
            >
              {[0, 0.5, 1].map((ratio) => {
                const y = PAD_TOP + plotHeight * (1 - ratio);
                const rawVal = maxVal * ratio;
                const formattedVal =
                  rawVal >= 1000000
                    ? `${(rawVal / 1000000).toFixed(1)}M`
                    : rawVal >= 1000
                    ? `${(rawVal / 1000).toFixed(0)}k`
                    : rawVal.toFixed(0);
                return (
                  <g key={ratio}>
                    <line
                      x1={PAD_LEFT}
                      y1={y}
                      x2={CHART_WIDTH - PAD_RIGHT}
                      y2={y}
                      stroke="#e5e5de"
                      strokeDasharray="4 4"
                      strokeWidth={1}
                    />
                    <text
                      x={PAD_LEFT - 6}
                      y={y + 3}
                      textAnchor="end"
                      fontSize={8}
                      className="mono fill-[#8a8a82] font-normal"
                    >
                      {currency} {formattedVal}
                    </text>
                  </g>
                );
              })}

              <path
                d={incomePath}
                fill="none"
                stroke="#16a34a"
                strokeWidth={3}
                strokeLinecap="round"
                strokeLinejoin="round"
              />

              <path
                d={expensePath}
                fill="none"
                stroke="#dc2626"
                strokeWidth={3}
                strokeLinecap="round"
                strokeLinejoin="round"
              />

              {incomePoints.map((p, i) => (
                <g
                  key={`inc-${i}`}
                  className="cursor-pointer"
                  onMouseEnter={() => setHoveredIndex(i)}
                  onMouseLeave={() => setHoveredIndex(null)}
                >
                  <circle cx={p.x} cy={p.y} r={hoveredIndex === i ? 6 : 4} fill="#16a34a" stroke="white" strokeWidth={2} />
                </g>
              ))}

              {expensePoints.map((p, i) => (
                <g
                  key={`exp-${i}`}
                  className="cursor-pointer"
                  onMouseEnter={() => setHoveredIndex(i)}
                  onMouseLeave={() => setHoveredIndex(null)}
                >
                  <circle cx={p.x} cy={p.y} r={hoveredIndex === i ? 6 : 4} fill="#dc2626" stroke="white" strokeWidth={2} />
                </g>
              ))}

              {chartData.map((d, index) => {
                const x = PAD_LEFT + (index / (chartData.length - 1)) * plotWidth;
                return (
                  <text
                    key={d.month}
                    x={x}
                    y={CHART_HEIGHT - 6}
                    textAnchor="middle"
                    fontSize={8.5}
                    className={`mono ${hoveredIndex === index ? "fill-[#24241f] font-bold" : "fill-[#8a8a82] font-normal"}`}
                  >
                    {monthAbbrev(d.month)}
                  </text>
                );
              })}
            </svg>

            {hoveredIndex !== null && chartData[hoveredIndex] && (
              <div
                className="absolute top-2 right-4 border border-[#d8d8d1] bg-[#fafaf6] p-3 shadow-md text-xs space-y-1"
              >
                <p className="font-bold text-[#24241f] border-b border-[#e5e5de] pb-1">
                  {monthFull(chartData[hoveredIndex].month)}
                </p>
                <div className="flex justify-between gap-4 text-[#16a34a] font-semibold">
                  <span>Income:</span>
                  <span>{currency} {chartData[hoveredIndex].income.toFixed(2)}</span>
                </div>
                <div className="flex justify-between gap-4 text-[#dc2626] font-semibold">
                  <span>Expense:</span>
                  <span>{currency} {chartData[hoveredIndex].expense.toFixed(2)}</span>
                </div>
                <div className="flex justify-between gap-4 text-[#24241f] font-bold pt-1 border-t border-[#e5e5de]">
                  <span>Net:</span>
                  <span>{currency} {(chartData[hoveredIndex].income - chartData[hoveredIndex].expense).toFixed(2)}</span>
                </div>
              </div>
            )}
          </div>
        )}
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
