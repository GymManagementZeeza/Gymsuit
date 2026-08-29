"use client";

import dynamic from "next/dynamic";

const ScannerExperience = dynamic(
  () => import("@/components/scanner/ScannerExperience"),
  { ssr: false }
);

export default function ScannerClient() {
  return <ScannerExperience />;
}
