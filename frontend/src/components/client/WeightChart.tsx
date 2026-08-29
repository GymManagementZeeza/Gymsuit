import type { WeightLog } from "@/lib/weightLogs";

export function WeightChart({ history }: { history: WeightLog[] }) {
  if (history.length < 2) {
    return (
      <div className="flex h-[150px] items-center justify-center border border-dashed border-[#d8d8d1] text-center text-xs text-[#8a8a82]">
        Record your weight a couple of times to see your trend here.
      </div>
    );
  }

  const points = history.map((log) => ({
    label: new Date(log.recordedAt).toLocaleDateString(undefined, { month: "short", day: "numeric" }),
    v: log.weightKg,
  }));

  const min = Math.min(...points.map((x) => x.v)) - 1;
  const max = Math.max(...points.map((x) => x.v)) + 1;
  const step = points.length > 1 ? 430 / (points.length - 1) : 0;
  const coords = points.map((d, i) => ({
    x: i * step,
    y: 130 - ((d.v - min) / (max - min)) * 105,
  }));
  const linePoints = coords.map((c) => `${c.x},${c.y}`).join(" ");

  return (
    <div>
      <svg viewBox="-7 0 440 150" preserveAspectRatio="none" className="w-full">
        <defs>
          <linearGradient id="weight-line" x1="0" x2="1">
            <stop stopColor="#8fae2a" />
            <stop offset="1" stopColor="#24241f" />
          </linearGradient>
        </defs>
        <path d="M0 130H430" stroke="#e5e5de" strokeDasharray="4 6" />
        <path d="M0 82H430" stroke="#e5e5de" strokeDasharray="4 6" />
        <polyline
          fill="none"
          stroke="url(#weight-line)"
          strokeWidth="4"
          strokeLinecap="round"
          strokeLinejoin="round"
          points={linePoints}
        />
        {coords.map((c, i) => (
          <circle
            key={points[i].label + i}
            cx={c.x}
            cy={c.y}
            r="5"
            fill={i === coords.length - 1 ? "#24241f" : "#fff"}
            stroke={i === coords.length - 1 ? "#24241f" : "#c7f36a"}
            strokeWidth="3"
          />
        ))}
      </svg>
      <div className="mono mt-2 flex justify-between text-[10px] text-[#8a8a82]">
        {points.map((d, i) => (
          <span key={d.label + i}>{d.label}</span>
        ))}
      </div>
    </div>
  );
}
