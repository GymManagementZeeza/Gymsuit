"use client";

/* Training Ledger page: Staff payroll — fixed/hourly pay in any currency, tracked from pending to paid. */
import { useEffect, useState } from "react";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, StatusPill, TableAction } from "@/components/dashboard/ui";
import { CheckCircle2, CreditCard, Plus, WalletCards } from "lucide-react";
import { useSession } from "@/hooks/useSession";
import { listTrainers, type Trainer } from "@/lib/trainers";
import { listTeamManagers, type TeamManager } from "@/lib/team";
import { getTrainerCompensation, type TrainerCompensation } from "@/lib/trainerCompensation";
import { getManagerCompensation, type ManagerCompensation } from "@/lib/managerCompensation";
import { listTransactions, type GymTransactionRecord } from "@/lib/transactions";
import StaffPayModal, { type PayableStaff } from "@/components/dashboard/StaffPayModal";

type StaffRow = {
  key: string;
  personId: number;
  firstName: string;
  lastName: string;
  role: "Trainer" | "Manager";
  compensation: TrainerCompensation | ManagerCompensation | null;
  accruedThisMonth: number | null;
  paidThisMonth: boolean;
};

function payLabel(compensation: TrainerCompensation | ManagerCompensation | null) {
  if (!compensation) return "No pay rate set";
  if (compensation.payType === "HOURLY") return `${compensation.currency} ${compensation.hourlyRate} / hour`;
  return `${compensation.currency} ${compensation.monthlySalary} / month`;
}

function formatAmount(amount: number, currency: string) {
  return `${currency} ${amount.toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
}

function isThisMonth(dateStr: string) {
  const d = new Date(dateStr);
  const now = new Date();
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
}

export default function FinancePage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [rows, setRows] = useState<StaffRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [payTarget, setPayTarget] = useState<PayableStaff | "all" | null>(null);

  const fetchStaffPay = () => {
    if (!gymId) return;
    Promise.all([listTrainers(gymId), listTeamManagers(gymId), listTransactions(gymId)])
      .then(async ([trainers, managers, transactions]) => {
        const trainerRows = await Promise.all(
          trainers.map(async (trainer: Trainer) => {
            const compensation = await getTrainerCompensation(gymId, trainer.id).catch(() => null);
            const monthTx = transactions.filter(
              (t: GymTransactionRecord) => t.trainerId === trainer.id && t.direction === "EXPENSE" && isThisMonth(t.occurredOn)
            );
            return {
              key: `trainer-${trainer.id}`,
              personId: trainer.id,
              firstName: trainer.firstName,
              lastName: trainer.lastName,
              role: "Trainer" as const,
              compensation,
              accruedThisMonth: monthTx.length ? monthTx.reduce((sum, t) => sum + t.amount, 0) : null,
              paidThisMonth: monthTx.length > 0,
            };
          })
        );
        const managerRows = await Promise.all(
          managers.map(async (manager: TeamManager) => {
            const compensation = await getManagerCompensation(gymId, manager.id).catch(() => null);
            const monthTx = transactions.filter(
              (t: GymTransactionRecord) => t.managerId === manager.id && t.direction === "EXPENSE" && isThisMonth(t.occurredOn)
            );
            return {
              key: `manager-${manager.id}`,
              personId: manager.id,
              firstName: manager.firstName,
              lastName: manager.lastName,
              role: "Manager" as const,
              compensation,
              accruedThisMonth: monthTx.length ? monthTx.reduce((sum, t) => sum + t.amount, 0) : null,
              paidThisMonth: monthTx.length > 0,
            };
          })
        );
        setRows([...trainerRows, ...managerRows]);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load staff pay."))
      .finally(() => setLoading(false));
  };

  useEffect(fetchStaffPay, [gymId]);

  const reload = () => {
    setLoading(true);
    fetchStaffPay();
  };

  const withPay = rows.filter((r) => r.compensation !== null);
  const paidCount = withPay.filter((r) => r.paidThisMonth).length;
  const pendingCount = withPay.length - paidCount;
  const paidTotal = withPay
    .filter((r) => r.paidThisMonth && r.accruedThisMonth !== null)
    .reduce((sum, r) => sum + (r.accruedThisMonth ?? 0), 0);
  const currency = withPay[0]?.compensation?.currency ?? "INR";

  const staffOptions: PayableStaff[] = rows.map((row) => ({
    id: row.personId,
    firstName: row.firstName,
    lastName: row.lastName,
    role: row.role,
    subtitle: payLabel(row.compensation),
  }));

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow="Finance · Staff pay"
        title="Pay people with clarity."
        description="Set fixed or hourly pay in any currency and follow every payment from pending to paid."
        actions={
          <ActionButton icon={<Plus className="size-4" />} onClick={() => setPayTarget("all")}>
            Record payment
          </ActionButton>
        }
      />

      <section className="grid gap-4 md:grid-cols-3">
        <MetricCard
          label="Payroll due"
          value={String(pendingCount)}
          detail={`${pendingCount} payment${pendingCount === 1 ? "" : "s"} pending`}
          tone="lime"
          icon={<WalletCards className="size-5" />}
        />
        <MetricCard
          label="Paid this month"
          value={formatAmount(paidTotal, currency)}
          detail={`${paidCount} completed payment${paidCount === 1 ? "" : "s"}`}
          tone="paper"
          icon={<CheckCircle2 className="size-5" />}
        />
        <MetricCard
          label="Staff with pay set"
          value={`${withPay.length} / ${rows.length}`}
          detail="trainers & managers"
          tone="orange"
          icon={<CreditCard className="size-5" />}
        />
      </section>

      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
          <div>
            <p className="ledger-label">Current payroll</p>
            <h2 className="mt-2 text-xl font-bold">Payment tracker</h2>
          </div>
        </div>
        {error && <p className="p-5 text-sm text-red-600">{error}</p>}
        {!loading && rows.length === 0 && !error && (
          <p className="p-8 text-center text-sm text-[#76766f]">No trainers or managers yet.</p>
        )}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left">
            <thead className="border-b border-[#e7e7e1] bg-[#fafaf6]">
              <tr className="text-[10px] uppercase tracking-[0.12em] text-[#75756e]">
                <th className="px-5 py-3">Staff member</th>
                <th className="px-4 py-3">Pay setup</th>
                <th className="px-4 py-3">Accrued</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-5 py-3">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#ededE7]">
              {loading &&
                [0, 1, 2].map((i) => (
                  <tr key={i}>
                    <td className="px-5 py-4" colSpan={5}>
                      <div className="h-4 w-1/3 animate-pulse rounded bg-[#ededE7]" />
                    </td>
                  </tr>
                ))}
              {rows.map((row, index) => (
                <tr key={row.key}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <span
                        className={`grid size-9 place-items-center rounded-full text-[10px] font-bold ${index % 2 ? "bg-[#d8e4fe]" : "bg-[#f4d1be]"}`}
                      >
                        {row.firstName[0]}
                        {row.lastName[0]}
                      </span>
                      <div>
                        <p className="text-sm font-bold">
                          {row.firstName} {row.lastName}
                        </p>
                        <p className="mt-1 text-[10px] text-[#777770]">{row.role}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-4 text-xs">{payLabel(row.compensation)}</td>
                  <td className="mono px-4 py-4 text-sm font-bold">
                    {row.accruedThisMonth !== null
                      ? formatAmount(row.accruedThisMonth, row.compensation?.currency ?? currency)
                      : "—"}
                  </td>
                  <td className="px-4 py-4">
                    <StatusPill
                      label={!row.compensation ? "No pay rate" : row.paidThisMonth ? "Paid" : "Pending"}
                      tone={!row.compensation ? "orange" : row.paidThisMonth ? "lime" : "orange"}
                    />
                  </td>
                  <td className="px-5 py-4">
                    <TableAction
                      onClick={() =>
                        setPayTarget({
                          id: row.personId,
                          firstName: row.firstName,
                          lastName: row.lastName,
                          role: row.role,
                          subtitle: payLabel(row.compensation),
                        })
                      }
                    >
                      Record payment
                    </TableAction>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {payTarget && gymId && (
        <StaffPayModal
          gymId={gymId}
          staff={staffOptions}
          initialSelected={payTarget === "all" ? undefined : payTarget}
          onClose={() => setPayTarget(null)}
          onPaid={reload}
        />
      )}
    </div>
  );
}
