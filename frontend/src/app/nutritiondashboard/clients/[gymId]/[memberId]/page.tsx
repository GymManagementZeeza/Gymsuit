"use client";

/* One client's food log, macros, weight and activity (read-only), plus sending them meal plans. */
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import ClientProgressView from "@/components/progress/ClientProgressView";
import MealPlanPanel from "@/components/nutrition/MealPlanPanel";
import { listNutritionClients } from "@/lib/nutritionist";

export default function NutritionClientPage() {
  const params = useParams<{ gymId: string; memberId: string }>();
  const memberId = Number(params.memberId);
  const [clientName, setClientName] = useState("");

  useEffect(() => {
    listNutritionClients()
      .then((clients) => {
        const client = clients.find((c) => c.memberId === memberId);
        if (client) setClientName(client.firstName);
      })
      .catch(() => {});
  }, [memberId]);

  return (
    <ClientProgressView
      backHref="/nutritiondashboard"
      backLabel="All clients"
      gymId={Number(params.gymId)}
      memberId={memberId}
      loginPath="/diet"
    >
      <MealPlanPanel memberId={memberId} clientName={clientName} />
    </ClientProgressView>
  );
}
