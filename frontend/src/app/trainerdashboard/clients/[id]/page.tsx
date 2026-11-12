"use client";

/* Trainer client progress — one client's synced health data from their mobile app. */
import ClientProgressView from "@/components/progress/ClientProgressView";

export default function ClientProgressPage() {
  return <ClientProgressView backHref="/trainerdashboard/clients" backLabel="All clients" />;
}
