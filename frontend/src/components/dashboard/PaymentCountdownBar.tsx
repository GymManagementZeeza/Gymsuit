function daysBetween(a: Date, b: Date) {
  const msPerDay = 24 * 60 * 60 * 1000;
  const utcA = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate());
  const utcB = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate());
  return Math.round((utcB - utcA) / msPerDay);
}

export default function PaymentCountdownBar({
  periodStart,
  periodEnd,
}: {
  periodStart: string;
  periodEnd: string;
}) {
  const today = new Date();
  const start = new Date(`${periodStart}T00:00:00`);
  const end = new Date(`${periodEnd}T00:00:00`);

  const totalDays = Math.max(1, daysBetween(start, end));
  const daysLeft = daysBetween(today, end);
  const elapsedDays = Math.min(totalDays, Math.max(0, totalDays - daysLeft));
  const remainingFraction = Math.min(1, Math.max(0, 1 - elapsedDays / totalDays));

  const label = daysLeft > 0 ? `${daysLeft}d left` : daysLeft === 0 ? "Due today" : `${Math.abs(daysLeft)}d overdue`;
  const labelTone = daysLeft <= 0 ? "text-red-700" : daysLeft <= 3 ? "text-[#b5590f]" : "text-[#4a4a44]";

  return (
    <div className="flex flex-col gap-1">
      <div className="h-1.5 w-20 overflow-hidden rounded-full bg-red-200">
        <div
          className="h-full rounded-full bg-[#7fb84a] transition-all"
          style={{ width: `${remainingFraction * 100}%` }}
        />
      </div>
      <span className={`text-[10px] font-semibold ${labelTone}`}>{label}</span>
    </div>
  );
}
