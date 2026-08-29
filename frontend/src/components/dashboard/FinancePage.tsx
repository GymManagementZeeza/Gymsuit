"use client";

/* Training Ledger page: Staff payroll — fixed/hourly pay in any currency, tracked from pending to paid. */
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, MetricCard, StatusPill, TableAction } from "@/components/dashboard/ui";
import { CheckCircle2, CreditCard, Plus, WalletCards } from "lucide-react";

export default function FinancePage() {
  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow="Finance · Staff pay"
        title="Pay people with clarity."
        description="Set fixed or hourly pay in any currency and follow every payment from pending to paid."
        actions={<ActionButton icon={<Plus className="size-4" />}>Record payment</ActionButton>}
      />
      <StaffPayPanel />
    </div>
  );
}

function StaffPayPanel() {
  const staff = [
    { person: "Alex Morgan", role: "Trainer", method: "$48 / hour", accrued: "$1,584", state: "Pending" },
    { person: "Naomi Brooks", role: "Trainer", method: "$5,200 / month", accrued: "$2,600", state: "Scheduled" },
    { person: "Sofia Bennett", role: "Manager", method: "$4,800 / month", accrued: "$2,400", state: "Pending" },
    { person: "Jordan Lee", role: "Trainer", method: "$45 / hour", accrued: "$1,260", state: "Paid" },
  ];
  return (
    <>
      <section className="grid gap-4 md:grid-cols-3">
        <MetricCard label="Payroll due" value="$6,584" detail="3 payments pending" tone="lime" icon={<WalletCards className="size-5" />} />
        <MetricCard label="Paid this month" value="$4,310" detail="4 completed payments" tone="paper" icon={<CheckCircle2 className="size-5" />} />
        <MetricCard label="Next pay run" value="MAR 15" detail="in 4 days" tone="orange" icon={<CreditCard className="size-5" />} />
      </section>
      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
          <div>
            <p className="ledger-label">Current payroll</p>
            <h2 className="mt-2 text-xl font-bold">Payment tracker</h2>
          </div>
          <button className="text-xs font-bold underline underline-offset-4">Payment history</button>
        </div>
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
              {staff.map((person, index) => (
                <tr key={person.person}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <span className={`grid size-9 place-items-center rounded-full text-[10px] font-bold ${index % 2 ? "bg-[#d8e4fe]" : "bg-[#f4d1be]"}`}>
                        {person.person.split(" ").map((part) => part[0]).join("")}
                      </span>
                      <div>
                        <p className="text-sm font-bold">{person.person}</p>
                        <p className="mt-1 text-[10px] text-[#777770]">{person.role}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-4 text-xs">{person.method}</td>
                  <td className="mono px-4 py-4 text-sm font-bold">{person.accrued}</td>
                  <td className="px-4 py-4">
                    <StatusPill label={person.state} tone={person.state === "Paid" ? "lime" : person.state === "Scheduled" ? "blue" : "orange"} />
                  </td>
                  <td className="px-5 py-4">
                    <TableAction>{person.state === "Paid" ? "View record" : "Mark paid"}</TableAction>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
