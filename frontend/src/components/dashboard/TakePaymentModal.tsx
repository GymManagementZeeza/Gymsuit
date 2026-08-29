"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import QRCode from "qrcode";
import { X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import { takePlanPayment, type PaymentMethod } from "@/lib/memberPayments";
import type { Member } from "@/lib/members";
import type { MemberSubscription } from "@/lib/memberSubscriptions";
import type { Gym } from "@/lib/gyms";

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CASH", label: "Cash" },
  { value: "UPI", label: "UPI" },
  { value: "BANK_TRANSFER", label: "Bank transfer" },
  { value: "OTHER", label: "Other" },
];

export default function TakePaymentModal({
  gymId,
  member,
  subscription,
  gym,
  onClose,
  onPaid,
}: {
  gymId: number;
  member: Member;
  subscription: MemberSubscription;
  gym: Gym | null;
  onClose: () => void;
  onPaid: () => void;
}) {
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleMethodChange = (value: PaymentMethod) => {
    setMethod(value);
    setQrDataUrl(null);
  };

  const handleGenerateQr = async () => {
    if (!gym?.upiId) return;
    setError(null);
    setGenerating(true);
    try {
      const note = `${subscription.planName} plan payment — ${member.firstName} ${member.lastName}`;
      const upiUri = `upi://pay?pa=${encodeURIComponent(gym.upiId)}&pn=${encodeURIComponent(gym.name)}&am=${subscription.planPrice.toFixed(2)}&cu=${subscription.planCurrency}&tn=${encodeURIComponent(note)}`;
      const dataUrl = await QRCode.toDataURL(upiUri, { width: 240, margin: 1 });
      setQrDataUrl(dataUrl);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not generate the QR code.");
    } finally {
      setGenerating(false);
    }
  };

  const handleConfirmPaid = async () => {
    setConfirmOpen(false);
    setError(null);
    setSaving(true);
    try {
      await takePlanPayment(gymId, member.id, method);
      onPaid();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not record this payment.");
      setSaving(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-md overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Take payment</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {member.firstName} {member.lastName}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-8 place-items-center transition hover:bg-[#efefe9]"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>

        <div className="flex flex-col gap-5 p-5">
          <div className="flex items-center justify-between border border-[#d8d8d1] bg-[#fafaf6] px-4 py-3">
            <span className="text-sm font-semibold">{subscription.planName} plan</span>
            <span className="mono text-sm font-bold">
              {subscription.planCurrency} {subscription.planPrice}
            </span>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
              Payment method
            </span>
            <div className="grid grid-cols-4 gap-2">
              {PAYMENT_METHODS.map((m) => (
                <button
                  key={m.value}
                  type="button"
                  onClick={() => handleMethodChange(m.value)}
                  className={`h-9 border text-xs font-bold transition ${
                    method === m.value ? "border-[#24241f] bg-[#24241f] text-white" : "border-[#d8d8d1] bg-white"
                  }`}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          {method === "UPI" && !gym?.upiId && (
            <p className="rounded-xl border border-dashed border-[#d8d8d1] p-3 text-xs text-[#76766f]">
              No UPI ID is set up for this gym — add one from Settings to generate a QR code, or record this as a
              different method.
            </p>
          )}

          {method === "UPI" && gym?.upiId && !qrDataUrl && (
            <button
              type="button"
              onClick={handleGenerateQr}
              disabled={generating}
              className="h-10 border border-[#24241f] bg-[#24241f] text-xs font-bold text-white transition hover:bg-[#42423a] disabled:opacity-50"
            >
              {generating ? "Generating…" : "Generate QR"}
            </button>
          )}

          {qrDataUrl && (
            <div className="flex flex-col items-center gap-2 border border-[#d8d8d1] bg-[#fafaf6] p-5">
              {/* eslint-disable-next-line @next/next/no-img-element -- generated data: URL, not a static asset */}
              <img src={qrDataUrl} alt="UPI payment QR code" className="size-56" />
              <p className="text-center text-xs text-[#76766f]">
                Scan with any UPI app to pay {subscription.planCurrency} {subscription.planPrice} to {gym?.upiId}
              </p>
            </div>
          )}

          {error && <ErrorBanner message={error} />}

          <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2] disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => setConfirmOpen(true)}
              disabled={saving}
              className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
            >
              {saving ? "Recording…" : "Payment received"}
            </button>
          </div>
        </div>
      </div>

      {confirmOpen && (
        <ConfirmDialog
          title="Payment received?"
          description={`Confirm you've received ${subscription.planCurrency} ${subscription.planPrice} from ${member.firstName} for the ${subscription.planName} plan. This activates the plan.`}
          confirmLabel="Yes, received"
          onConfirm={handleConfirmPaid}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </div>,
    document.body
  );
}
