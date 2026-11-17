"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import QRCode from "qrcode";
import { Check, Copy, Download, Link2, QrCode, X } from "lucide-react";
import { FlexButton } from "@/components/dashboard/ui";

/**
 * The gym's member-registration QR: anyone who scans it lands on the public
 * sign-up form for this gym (details + waiver signature + plan + UPI pay).
 * Print it, stick it on the wall, or send the link — no login needed to register.
 */
export default function RegistrationQrModal({
  gymId,
  gymName,
  onClose,
}: {
  gymId: number;
  gymName: string;
  onClose: () => void;
}) {
  const [url, setUrl] = useState("");
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [qrError, setQrError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const link = `${window.location.origin}/register/${gymId}`;
    setUrl(link);
    let cancelled = false;
    QRCode.toDataURL(link, { width: 360, margin: 1 })
      .then((dataUrl) => {
        if (!cancelled) setQrDataUrl(dataUrl);
      })
      .catch((err) => {
        if (!cancelled) setQrError(err instanceof Error ? err.message : "Could not generate the QR code.");
      });
    return () => {
      cancelled = true;
    };
  }, [gymId]);

  const handleCopy = async () => {
    if (!url) return;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard unavailable — the link is visible to copy by hand.
    }
  };

  const handleDownload = () => {
    if (!qrDataUrl) return;
    const a = document.createElement("a");
    a.href = qrDataUrl;
    a.download = `gymsuit-register-${gymId}.png`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-950/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-[1.75rem] bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={`Member registration QR code for ${gymName}`}
      >
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-black tracking-tight text-stone-900">Member registration QR</h2>
            <p className="mt-0.5 text-sm font-medium text-stone-500">{gymName}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-9 shrink-0 place-items-center rounded-full bg-stone-100 text-stone-500 transition hover:bg-stone-200"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>

        <p className="mt-4 text-sm leading-relaxed text-stone-600">
          Anyone who scans this code can register themselves — details, waiver signature, plan and UPI payment — no
          login needed. Their plan stays awaiting payment until you confirm it from the member list.
        </p>

        <div className="mt-5 flex flex-col items-center rounded-2xl bg-stone-50 p-5">
          {qrError ? (
            <p className="text-sm font-semibold text-red-600">{qrError}</p>
          ) : qrDataUrl ? (
            <>
              {/* eslint-disable-next-line @next/next/no-img-element -- generated data URL, not a static asset */}
              <img src={qrDataUrl} alt={`Registration QR code for ${gymName}`} className="size-56 object-contain" />
              <p className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-stone-500">
                <QrCode className="size-3.5" /> Scan to join {gymName}
              </p>
            </>
          ) : (
            <p className="py-10 text-sm font-semibold text-stone-400">Generating…</p>
          )}
        </div>

        {url && (
          <button
            type="button"
            onClick={handleCopy}
            className="mt-4 flex w-full items-center gap-2 rounded-2xl border border-stone-200 bg-white px-4 py-3 text-left text-xs font-semibold text-stone-600 transition hover:bg-stone-50"
            title="Copy registration link"
          >
            <Link2 className="size-4 shrink-0 text-stone-400" />
            <span className="min-w-0 flex-1 truncate">{url}</span>
            {copied ? <Check className="size-4 shrink-0 text-emerald-600" /> : <Copy className="size-4 shrink-0 text-stone-400" />}
          </button>
        )}

        <div className="mt-5 flex flex-col gap-2">
          <FlexButton variant="lime" onClick={qrDataUrl ? handleDownload : undefined} className="w-full justify-center">
            <Download className="size-4" />
            Download QR code
          </FlexButton>
          <FlexButton variant="ghost" onClick={handleCopy} className="w-full justify-center">
            {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
            {copied ? "Link copied" : "Copy registration link"}
          </FlexButton>
        </div>
      </div>
    </div>,
    document.body
  );
}
