"use client";

import { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { Printer, X } from "lucide-react";
import type { MemberPayment } from "@/lib/memberPayments";
import type { GymTransactionRecord } from "@/lib/transactions";
import type { Gym } from "@/lib/gyms";

function monthValue(date: Date) {
  return date.toISOString().slice(0, 7);
}

function monthLabel(value: string) {
  return new Date(`${value}-01T00:00:00`).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function inMonth(dateStr: string, month: string) {
  return dateStr.startsWith(month);
}

export default function MonthlyReportModal({
  gym,
  payments,
  transactions,
  memberNames,
  onClose,
}: {
  gym: Gym | null;
  payments: MemberPayment[];
  transactions: GymTransactionRecord[];
  memberNames: Record<number, string>;
  onClose: () => void;
}) {
  const [month, setMonth] = useState(() => monthValue(new Date()));

  const report = useMemo(() => {
    const membershipIncome = payments.filter(
      (p) => p.status === "SUCCEEDED" && inMonth(p.paidAt ?? p.createdAt, month)
    );
    const otherIncome = transactions.filter((t) => t.direction === "INCOME" && inMonth(t.occurredOn, month));
    const expenses = transactions.filter((t) => t.direction === "EXPENSE" && inMonth(t.occurredOn, month));

    const currency = membershipIncome[0]?.currency ?? otherIncome[0]?.currency ?? expenses[0]?.currency ?? "INR";
    const sum = (list: { amount: number | string }[]) => list.reduce((total, item) => total + Number(item.amount), 0);

    const membershipTotal = sum(membershipIncome);
    const otherIncomeTotal = sum(otherIncome);
    const totalIncome = membershipTotal + otherIncomeTotal;
    const totalExpenses = sum(expenses);
    const netAfterExpenses = totalIncome - totalExpenses;

    const incomeRows = [
      ...membershipIncome.map((p) => ({
        date: p.paidAt ?? p.createdAt,
        description: `Membership payment — ${memberNames[p.memberId] ?? `Member #${p.memberId}`}`,
        amount: Number(p.amount),
      })),
      ...otherIncome.map((t) => ({
        date: t.occurredOn,
        description: t.description,
        amount: Number(t.amount),
      })),
    ].sort((a, b) => a.date.localeCompare(b.date));

    const expenseRows = expenses
      .map((t) => ({ date: t.occurredOn, description: t.description, amount: Number(t.amount) }))
      .sort((a, b) => a.date.localeCompare(b.date));

    return {
      currency,
      membershipTotal,
      otherIncomeTotal,
      totalIncome,
      totalExpenses,
      netAfterExpenses,
      incomeRows,
      expenseRows,
    };
  }, [payments, transactions, memberNames, month]);

  const maxBar = Math.max(1, report.totalIncome, report.totalExpenses);
  const money = (value: number) => `${report.currency} ${value.toFixed(2)}`;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-[#141410]/50 p-4 print:static print:bg-transparent print:p-0">
      <div
        id="monthly-report"
        className="my-6 w-full max-w-3xl border border-[#d8d8d1] bg-white print:my-0 print:max-w-none print:border-0"
      >
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5 print:hidden">
          <div>
            <p className="ledger-label">Monthly report</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Print financial report</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-8 place-items-center transition hover:bg-[#efefe9]"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>

        <div className="flex flex-col gap-3 border-b border-[#e5e5de] p-5 sm:flex-row sm:items-center sm:justify-between print:hidden">
          <label className="flex items-center gap-2">
            <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Month</span>
            <input
              type="month"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
              className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
            />
          </label>
          <button
            type="button"
            onClick={() => window.print()}
            className="flex h-10 items-center gap-2 bg-[#c7f36a] px-4 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
          >
            <Printer className="size-4" /> Print
          </button>
        </div>

        <div className="p-6 sm:p-8">
          <div className="flex items-start justify-between border-b border-[#24241f] pb-4">
            <div>
              <p className="text-lg font-bold tracking-[-0.02em]">{gym?.name ?? "Gym"} — Financial report</p>
              <p className="mt-1 text-sm text-[#6e6e67]">{monthLabel(month)}</p>
            </div>
            <p className="text-xs text-[#8a8a82]">
              Generated {new Date().toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}
            </p>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-4">
            <div className="border border-[#d8d8d1] p-4">
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Total income</p>
              <p className="mt-1 text-xl font-bold text-[#3f7a1f]">{money(report.totalIncome)}</p>
            </div>
            <div className="border border-[#d8d8d1] p-4">
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Total expenses</p>
              <p className="mt-1 text-xl font-bold text-red-700">{money(report.totalExpenses)}</p>
            </div>
            <div className="border border-[#24241f] bg-[#fafaf6] p-4">
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Net after expenses</p>
              <p className={`mt-1 text-xl font-bold ${report.netAfterExpenses < 0 ? "text-red-700" : "text-[#24241f]"}`}>
                {money(report.netAfterExpenses)}
              </p>
            </div>
          </div>

          <div className="mt-6">
            <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Income vs. expenses</p>
            <div className="mt-3 space-y-2">
              <div className="flex items-center gap-3">
                <span className="w-16 shrink-0 text-xs font-semibold text-[#6e6e67]">Income</span>
                <div className="h-5 flex-1 bg-[#ececE5]">
                  <div className="h-full bg-[#8bb92f]" style={{ width: `${(report.totalIncome / maxBar) * 100}%` }} />
                </div>
                <span className="mono w-28 shrink-0 text-right text-xs font-bold">{money(report.totalIncome)}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="w-16 shrink-0 text-xs font-semibold text-[#6e6e67]">Expenses</span>
                <div className="h-5 flex-1 bg-[#ececE5]">
                  <div className="h-full bg-[#e0704a]" style={{ width: `${(report.totalExpenses / maxBar) * 100}%` }} />
                </div>
                <span className="mono w-28 shrink-0 text-right text-xs font-bold">{money(report.totalExpenses)}</span>
              </div>
            </div>
          </div>

          <div className="mt-8">
            <p className="text-sm font-bold">Income — {report.incomeRows.length} entries</p>
            {report.incomeRows.length === 0 ? (
              <p className="mt-2 text-xs text-[#8a8a82]">No income recorded this month.</p>
            ) : (
              <table className="mt-2 w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-[#d8d8d1] text-[9px] uppercase tracking-[0.1em] text-[#8a8a82]">
                    <th className="py-2 pr-3 font-bold">Date</th>
                    <th className="py-2 pr-3 font-bold">Description</th>
                    <th className="py-2 text-right font-bold">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#ededE7]">
                  {report.incomeRows.map((row, i) => (
                    <tr key={i}>
                      <td className="py-2 pr-3 text-[#696962]">
                        {new Date(row.date).toLocaleDateString(undefined, { month: "short", day: "numeric" })}
                      </td>
                      <td className="py-2 pr-3">{row.description}</td>
                      <td className="mono py-2 text-right font-semibold text-[#3f7a1f]">{money(row.amount)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="border-t border-[#24241f]">
                    <td className="py-2 pr-3 font-bold" colSpan={2}>
                      Total income
                    </td>
                    <td className="mono py-2 text-right font-bold text-[#3f7a1f]">{money(report.totalIncome)}</td>
                  </tr>
                </tfoot>
              </table>
            )}
          </div>

          <div className="mt-8">
            <p className="text-sm font-bold">Expenses — {report.expenseRows.length} entries</p>
            {report.expenseRows.length === 0 ? (
              <p className="mt-2 text-xs text-[#8a8a82]">No expenses recorded this month.</p>
            ) : (
              <table className="mt-2 w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-[#d8d8d1] text-[9px] uppercase tracking-[0.1em] text-[#8a8a82]">
                    <th className="py-2 pr-3 font-bold">Date</th>
                    <th className="py-2 pr-3 font-bold">Description</th>
                    <th className="py-2 text-right font-bold">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#ededE7]">
                  {report.expenseRows.map((row, i) => (
                    <tr key={i}>
                      <td className="py-2 pr-3 text-[#696962]">
                        {new Date(row.date).toLocaleDateString(undefined, { month: "short", day: "numeric" })}
                      </td>
                      <td className="py-2 pr-3">{row.description}</td>
                      <td className="mono py-2 text-right font-semibold text-red-700">{money(row.amount)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="border-t border-[#24241f]">
                    <td className="py-2 pr-3 font-bold" colSpan={2}>
                      Total expenses
                    </td>
                    <td className="mono py-2 text-right font-bold text-red-700">{money(report.totalExpenses)}</td>
                  </tr>
                </tfoot>
              </table>
            )}
          </div>

          <div className="mt-8 flex items-center justify-between border-t-2 border-[#24241f] pt-4">
            <p className="text-sm font-bold">Net amount after expenses</p>
            <p className={`mono text-lg font-bold ${report.netAfterExpenses < 0 ? "text-red-700" : "text-[#24241f]"}`}>
              {money(report.netAfterExpenses)}
            </p>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}
