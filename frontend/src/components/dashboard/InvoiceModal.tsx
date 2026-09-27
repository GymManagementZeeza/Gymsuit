"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import Link from "next/link";
import QRCode from "qrcode";
import { PDFDocument, StandardFonts, rgb } from "pdf-lib";
import { Download, MessageCircle, TriangleAlert, X } from "lucide-react";
import { FlexButton } from "@/components/dashboard/ui";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import type { Member } from "@/lib/members";
import type { MemberSubscription } from "@/lib/memberSubscriptions";
import type { Gym } from "@/lib/gyms";
import { notifyMember, whatsAppLink } from "@/lib/notifications";

/** Strip anything the PDF's WinAnsi fonts can't render (e.g. ₹, Malayalam script). */
function pdfSafe(text: string): string {
  return text.replace(/[^\x20-\x7E]/g, "?");
}

type InvoiceModalProps = {
  gymId: number;
  member: Member;
  subscription: MemberSubscription;
  gym: Gym;
  onClose: () => void;
};

export default function InvoiceModal({ gymId, member, subscription, gym, onClose }: InvoiceModalProps) {
  const { t } = useLanguage();
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [qrError, setQrError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const invoiceNo = `INV-${new Date().toISOString().slice(0, 10).replace(/-/g, "")}-${member.id}`;
  const dateStr = new Date().toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
  const amountLabel = `${subscription.planCurrency} ${subscription.planPrice.toFixed(2)}`;
  const memberName = `${member.firstName} ${member.lastName}`.trim();

  useEffect(() => {
    if (!gym.upiId) return;
    let cancelled = false;
    const note = `${subscription.planName} — ${memberName} (${invoiceNo})`;
    const upiUri = `upi://pay?pa=${encodeURIComponent(gym.upiId)}&pn=${encodeURIComponent(gym.name)}&am=${subscription.planPrice.toFixed(2)}&cu=${subscription.planCurrency}&tn=${encodeURIComponent(note)}`;
    QRCode.toDataURL(upiUri, { width: 360, margin: 1 })
      .then((dataUrl) => {
        if (!cancelled) setQrDataUrl(dataUrl);
      })
      .catch((err) => {
        if (!cancelled) setQrError(err instanceof Error ? err.message : "Could not generate the QR code.");
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gym.upiId]);

  async function buildPdf(qrPng: string): Promise<Uint8Array> {
    const doc = await PDFDocument.create();
    const page = doc.addPage([595.28, 841.89]); // A4
    const { width, height } = page.getSize();
    const font = await doc.embedFont(StandardFonts.Helvetica);
    const bold = await doc.embedFont(StandardFonts.HelveticaBold);
    const ink = rgb(0.14, 0.14, 0.12);
    const gray = rgb(0.45, 0.45, 0.42);
    const line = rgb(0.9, 0.9, 0.87);
    const margin = 48;
    let y = height - margin;

    const right = (text: string, size: number, f = font) =>
      width - margin - f.widthOfTextAtSize(text, size);
    const centered = (text: string, size: number, f = font) =>
      (width - f.widthOfTextAtSize(text, size)) / 2;

    page.drawText(pdfSafe(gym.name), { x: margin, y, size: 22, font: bold, color: ink });
    const title = "TAX INVOICE";
    page.drawText(title, { x: right(title, 16, bold), y, size: 16, font: bold, color: ink });
    y -= 18;
    if (gym.city) {
      page.drawText(pdfSafe(gym.city), { x: margin, y, size: 10, font, color: gray });
    }
    const invLine = `${invoiceNo}`;
    page.drawText(invLine, { x: right(invLine, 10), y, size: 10, font, color: gray });
    y -= 14;
    page.drawText(dateStr, { x: right(dateStr, 10), y, size: 10, font, color: gray });

    y -= 26;
    page.drawLine({ start: { x: margin, y }, end: { x: width - margin, y }, thickness: 1, color: line });

    y -= 26;
    page.drawText("Bill to:", { x: margin, y, size: 11, font: bold, color: ink });
    y -= 17;
    page.drawText(pdfSafe(memberName), { x: margin, y, size: 12, font: bold, color: ink });
    y -= 15;
    page.drawText(pdfSafe(member.phone), { x: margin, y, size: 10, font, color: gray });

    y -= 34;
    page.drawText("Description", { x: margin, y, size: 10, font: bold, color: gray });
    const amountHeader = "Amount";
    page.drawText(amountHeader, { x: right(amountHeader, 10, bold), y, size: 10, font: bold, color: gray });
    y -= 8;
    page.drawLine({ start: { x: margin, y }, end: { x: width - margin, y }, thickness: 1, color: line });

    y -= 20;
    const desc = pdfSafe(`${subscription.planName} — Membership renewal`);
    page.drawText(desc, { x: margin, y, size: 11, font, color: ink });
    page.drawText(pdfSafe(amountLabel), { x: right(pdfSafe(amountLabel), 11, font), y, size: 11, font, color: ink });

    y -= 24;
    const totalLabel = "Total";
    page.drawText(totalLabel, { x: margin, y, size: 12, font: bold, color: ink });
    const totalVal = pdfSafe(amountLabel);
    page.drawText(totalVal, { x: right(totalVal, 12, bold), y, size: 12, font: bold, color: ink });
    y -= 10;
    page.drawLine({ start: { x: margin, y }, end: { x: width - margin, y }, thickness: 1, color: line });

    // UPI QR code
    const qrBytes = Uint8Array.from(atob(qrPng.split(",")[1]), (c) => c.charCodeAt(0));
    const qrImage = await doc.embedPng(qrBytes);
    const qrSize = 170;
    y -= qrSize + 24;
    page.drawImage(qrImage, { x: (width - qrSize) / 2, y, width: qrSize, height: qrSize });
    y -= 20;
    const upiLine = `UPI ID: ${gym.upiId}`;
    page.drawText(pdfSafe(upiLine), { x: centered(pdfSafe(upiLine), 11, bold), y, size: 11, font: bold, color: ink });
    y -= 16;
    const scanLine = "Scan with any UPI app to pay";
    page.drawText(scanLine, { x: centered(scanLine, 10), y, size: 10, font, color: gray });

    const thanks = pdfSafe(`Thank you for training with ${gym.name}!`);
    page.drawText(thanks, { x: centered(thanks, 10, bold), y: 64, size: 10, font: bold, color: ink });
    const genLine = `Generated on ${dateStr}`;
    page.drawText(genLine, { x: centered(genLine, 9), y: 48, size: 9, font, color: gray });

    return doc.save();
  }

  async function handleShare() {
    if (!qrDataUrl || busy) return;
    setBusy(true);
    try {
      const pdfBytes = await buildPdf(qrDataUrl);
      const file = new File([pdfBytes.slice().buffer], `${invoiceNo}.pdf`, { type: "application/pdf" });
      const message = t.invoice.whatsappMessage(member.firstName, invoiceNo, subscription.planName, amountLabel, gym.name);
      let delivered = false;
      if (typeof navigator.canShare === "function" && navigator.canShare({ files: [file] })) {
        await navigator.share({ files: [file], title: invoiceNo, text: message });
        delivered = true;
      } else {
        // Desktop fallback: download the PDF and open the member's WhatsApp chat
        // with the invoice text pre-filled so it can be attached manually.
        const url = URL.createObjectURL(file);
        const a = document.createElement("a");
        a.href = url;
        a.download = file.name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 5000);
        if (member.phone) {
          window.open(whatsAppLink(member.phone, message), "_blank", "noopener,noreferrer");
        }
        delivered = true;
      }
      if (delivered) {
        await notifyMember(gymId, member.id, "CUSTOM", "WHATSAPP", `Invoice ${invoiceNo} (${amountLabel}) sent via WhatsApp.`).catch(() => {});
      }
      onClose();
    } catch (err) {
      // User cancelling the share sheet is not an error.
      if (!(err instanceof DOMException && err.name === "AbortError")) {
        setQrError(err instanceof Error ? err.message : "Could not create the invoice.");
      }
    } finally {
      setBusy(false);
    }
  }

  async function handleDownload() {
    if (!qrDataUrl || busy) return;
    setBusy(true);
    try {
      const pdfBytes = await buildPdf(qrDataUrl);
      const blob = new Blob([pdfBytes.slice().buffer], { type: "application/pdf" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${invoiceNo}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 5000);
    } catch (err) {
      setQrError(err instanceof Error ? err.message : "Could not create the invoice.");
    } finally {
      setBusy(false);
    }
  }

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-950/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-[1.75rem] bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={t.invoice.title}
      >
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-black tracking-tight text-stone-900">{t.invoice.title}</h2>
            <p className="mt-0.5 text-sm font-medium text-stone-500">{memberName}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-9 shrink-0 place-items-center rounded-full bg-stone-100 text-stone-500 transition hover:bg-stone-200"
            aria-label={t.common.cancel}
          >
            <X className="size-4" />
          </button>
        </div>

        {!gym.upiId ? (
          <div className="mt-5 rounded-2xl bg-amber-50 p-4">
            <div className="flex items-start gap-3">
              <TriangleAlert className="mt-0.5 size-5 shrink-0 text-amber-600" />
              <div>
                <p className="text-sm font-semibold text-amber-900">{t.invoice.noUpiId}</p>
                <Link
                  href="/dashboard/settings"
                  className="mt-2 inline-flex items-center rounded-full bg-amber-600 px-4 py-2 text-xs font-bold text-white transition hover:bg-amber-700"
                >
                  {t.invoice.openSettings}
                </Link>
              </div>
            </div>
          </div>
        ) : (
          <>
            <div className="mt-5 rounded-2xl border border-stone-100 bg-stone-50 p-4">
              <div className="flex items-baseline justify-between">
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-stone-400">{t.invoice.invoiceTitle}</p>
                <p className="mono text-xs font-semibold text-stone-500">{invoiceNo}</p>
              </div>
              <div className="mt-3 flex items-baseline justify-between border-t border-stone-200/70 pt-3">
                <div>
                  <p className="text-sm font-bold text-stone-900">{subscription.planName}</p>
                  <p className="mt-0.5 text-xs text-stone-500">{t.invoice.membershipRenewal}</p>
                </div>
                <p className="text-base font-black text-stone-900">{amountLabel}</p>
              </div>
              <div className="mt-4 flex flex-col items-center rounded-2xl bg-white p-4">
                {qrError ? (
                  <p className="text-sm font-semibold text-red-600">{qrError}</p>
                ) : qrDataUrl ? (
                  <>
                    {/* eslint-disable-next-line @next/next/no-img-element -- generated data URL, not a static asset */}
                    <img src={qrDataUrl} alt={t.invoice.scanToPay} className="size-44 object-contain" />
                    <p className="mono mt-2 text-xs font-bold text-stone-700">
                      {t.invoice.upiIdLabel}: {gym.upiId}
                    </p>
                    <p className="mt-1 text-xs font-medium text-stone-500">{t.invoice.scanToPay}</p>
                  </>
                ) : (
                  <p className="py-10 text-sm font-semibold text-stone-400">{t.invoice.generating}</p>
                )}
              </div>
            </div>

            {qrError && <p className="mt-3 text-sm font-semibold text-red-600">{qrError}</p>}

            <div className="mt-5 flex flex-col gap-2">
              <FlexButton
                variant="lime"
                onClick={qrDataUrl && !busy ? handleShare : undefined}
                className={`w-full justify-center ${!qrDataUrl || busy ? "pointer-events-none opacity-50" : ""}`}
              >
                <MessageCircle className="size-4" />
                {busy ? t.invoice.generating : t.invoice.sendViaWhatsApp}
              </FlexButton>
              <FlexButton
                variant="ghost"
                onClick={qrDataUrl && !busy ? handleDownload : undefined}
                className={`w-full justify-center ${!qrDataUrl || busy ? "pointer-events-none opacity-50" : ""}`}
              >
                <Download className="size-4" />
                {t.invoice.downloadPdf}
              </FlexButton>
            </div>
          </>
        )}
      </div>
    </div>,
    document.body
  );
}
