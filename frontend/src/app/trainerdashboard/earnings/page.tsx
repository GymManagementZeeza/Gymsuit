/* Training Ledger page: Earnings — revenue momentum, next payout, and where the month comes from. */
import { ArrowUpRight } from "lucide-react";

const revenueChart = [42, 58, 45, 72, 67, 88, 62, 80, 91, 75, 96, 83];

const breakdown = [
  { name: "One-to-one coaching", value: "$2,940", pct: "61%", tone: "bg-[#4a72db]" },
  { name: "Small group training", value: "$1,280", pct: "26%", tone: "bg-[#c7f36a]" },
  { name: "Program design", value: "$640", pct: "13%", tone: "bg-[#f47539]" },
];

export default function EarningsPage() {
  return (
    <div className="page-enter space-y-7">
      <section className="grid gap-4 lg:grid-cols-12">
        <div className="cut-corner bg-[#24241f] p-6 text-white lg:col-span-8">
          <p className="ledger-label !text-[#d7d7cf] before:!bg-[#c7f36a]">Total revenue · March</p>
          <div className="mt-4 flex items-end gap-3">
            <p className="display-face text-5xl leading-none">$4,860</p>
            <span className="mb-1 flex items-center gap-1 bg-[#c7f36a] px-2 py-1 text-[10px] font-black text-[#24241f]">
              <ArrowUpRight className="size-3" /> 18.6%
            </span>
          </div>
          <p className="mt-2 text-sm text-white/65">from 48 completed sessions</p>
          <div className="mt-8 flex h-28 items-end gap-2">
            {revenueChart.map((height, index) => (
              <div key={index} className="flex-1 bg-white/20" style={{ height: `${height}%` }} />
            ))}
          </div>
          <div className="mono mt-3 flex justify-between text-[10px] text-[#a6a69d]">
            <span>MAR 1</span>
            <span>MAR 31</span>
          </div>
        </div>

        <div className="border border-[#d8d8d1] bg-white p-6 lg:col-span-4">
          <p className="ledger-label">Next payout</p>
          <p className="display-face mt-4 text-4xl">$1,180</p>
          <p className="mt-2 text-sm text-[#6e6e67]">Available Friday, 14 March</p>
          <button className="mt-6 text-xs font-bold underline decoration-[#c7f36a] decoration-2 underline-offset-4">View payout details →</button>
        </div>
      </section>

      <section className="border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
          <div>
            <p className="ledger-label">Revenue by service</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Where the month is coming from.</h2>
          </div>
          <button className="border border-[#d8d8d1] px-3 py-2 text-xs font-bold">March 2025 ⌄</button>
        </div>
        <div className="divide-y divide-[#ebebe5]">
          {breakdown.map((row) => (
            <div className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:gap-5" key={row.name}>
              <p className="text-sm font-bold sm:w-52">{row.name}</p>
              <div className="h-1.5 flex-1 overflow-hidden bg-[#ececE5]">
                <div className={`h-full ${row.tone}`} style={{ width: row.pct }} />
              </div>
              <span className="text-xs font-semibold text-[#8a8a82] sm:w-12">{row.pct}</span>
              <p className="mono text-sm font-bold sm:w-20 sm:text-right">{row.value}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
