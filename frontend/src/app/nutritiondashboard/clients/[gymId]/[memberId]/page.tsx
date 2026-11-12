"use client";

/* One client's food log, macros, weight and activity — read-only for the nutrition expert. */
import { useParams } from "next/navigation";
import ClientProgressView from "@/components/progress/ClientProgressView";

export default function NutritionClientPage() {
  const params = useParams<{ gymId: string; memberId: string }>();
  return (
    <ClientProgressView
      backHref="/nutritiondashboard"
      backLabel="All clients"
      gymId={Number(params.gymId)}
      memberId={Number(params.memberId)}
      loginPath="/diet"
    />
  );
}
