import type { Metadata } from "next";
import ScannerClient from "./ScannerClient";

export const metadata: Metadata = {
  title: "GymSuite | Biometric Entry",
  description:
    "An interactive first-person biometric scanner demo built with Three.js.",
};

export default function ScannerPage() {
  return (
    <div style={{ position: "fixed", inset: 0, background: "#0a0b0d" }}>
      <a
        href="/"
        style={{
          position: "absolute",
          top: "1.25rem",
          left: "1.25rem",
          zIndex: 10,
          padding: "0.5rem 1rem",
          borderRadius: "999px",
          background: "rgba(255,255,255,0.08)",
          color: "#e8e8e8",
          fontSize: "0.85rem",
          fontFamily: "sans-serif",
          textDecoration: "none",
        }}
      >
        &larr; Back to site
      </a>
      <ScannerClient />
    </div>
  );
}
