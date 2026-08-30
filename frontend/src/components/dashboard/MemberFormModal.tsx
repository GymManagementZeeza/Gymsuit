"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import QRCode from "qrcode";
import { ArrowRight, X } from "lucide-react";
import PlanPicker from "@/components/dashboard/PlanPicker";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import { createMember, updateMember, payJoiningFee, type Member, type MemberInput } from "@/lib/members";
import { listMembershipPlans, type MembershipPlan } from "@/lib/membershipPlans";
import { subscribeMember } from "@/lib/memberSubscriptions";
import { paymentMethodLabel, type PaymentMethod } from "@/lib/memberPayments";
import { getGym, type Gym } from "@/lib/gyms";
import type { Gender } from "@/lib/auth";

const JOINING_FEE_METHODS: PaymentMethod[] = ["CASH", "UPI", "BANK_TRANSFER", "OTHER"];

function Field({
  label,
  name,
  required,
  type = "text",
  defaultValue,
  placeholder,
}: {
  label: string;
  name: string;
  required?: boolean;
  type?: string;
  defaultValue?: string;
  placeholder?: string;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        defaultValue={defaultValue}
        placeholder={placeholder}
        className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
      />
    </label>
  );
}

function DetailsStep({
  gymId,
  member,
  onSaved,
  onClose,
}: {
  gymId: number;
  member: Member | null;
  onSaved: (member: Member) => void;
  onClose: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isEdit = member !== null;

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const data = new FormData(event.currentTarget);
    const gender = data.get("gender") as string;
    const heightCm = data.get("heightCm") as string;
    const weightKg = data.get("weightKg") as string;

    const input: MemberInput = {
      firstName: data.get("firstName") as string,
      lastName: data.get("lastName") as string,
      email: data.get("email") as string,
      phone: data.get("phone") as string,
      dateOfBirth: (data.get("dateOfBirth") as string) || undefined,
      gender: gender ? (gender as Gender) : undefined,
      heightCm: heightCm ? Number(heightCm) : undefined,
      weightKg: weightKg ? Number(weightKg) : undefined,
      bloodGroup: (data.get("bloodGroup") as string) || undefined,
      medicalNotes: (data.get("medicalNotes") as string) || undefined,
      emergencyContactName: (data.get("emergencyContactName") as string) || undefined,
      emergencyContactPhone: (data.get("emergencyContactPhone") as string) || undefined,
      emergencyContactRelationship: (data.get("emergencyContactRelationship") as string) || undefined,
      waiverAccepted: data.get("waiverAccepted") === "on",
      joinDate: data.get("joinDate") as string,
    };

    try {
      const saved = isEdit ? await updateMember(gymId, member!.id, input) : await createMember(gymId, input);
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save this member.");
      setLoading(false);
    }
  };

  return (
    <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="First name" name="firstName" required defaultValue={member?.firstName} />
        <Field label="Last name" name="lastName" required defaultValue={member?.lastName} />
        <Field label="Email" name="email" type="email" required defaultValue={member?.email ?? ""} />
        <Field label="Phone" name="phone" type="tel" required defaultValue={member?.phone} />
        <Field
          label="Join date"
          name="joinDate"
          type="date"
          required
          defaultValue={member?.joinDate ?? new Date().toISOString().slice(0, 10)}
        />
        <Field label="Date of birth" name="dateOfBirth" type="date" defaultValue={member?.dateOfBirth ?? ""} />
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Gender</span>
          <select
            name="gender"
            defaultValue={member?.gender ?? ""}
            className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
          >
            <option value="">Prefer not to say</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </label>
        <Field label="Blood group" name="bloodGroup" defaultValue={member?.bloodGroup ?? ""} />
      </div>

      {!isEdit && (
        <p className="-mt-3 text-[11px] text-[#76766f]">
          This email becomes their login for the member dashboard — no password needed, just this email and a code.
        </p>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Height (cm)" name="heightCm" type="number" defaultValue={member?.heightCm?.toString() ?? ""} />
        <Field label="Weight (kg)" name="weightKg" type="number" defaultValue={member?.weightKg?.toString() ?? ""} />
      </div>

      <label className="flex flex-col gap-1.5">
        <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Medical notes</span>
        <textarea
          name="medicalNotes"
          rows={2}
          defaultValue={member?.medicalNotes ?? ""}
          className="border border-[#d8d8d1] bg-white p-3 text-sm outline-none transition-colors focus:border-[#24241f]"
        />
      </label>

      <div className="grid gap-3 sm:grid-cols-3">
        <Field label="Emergency contact" name="emergencyContactName" defaultValue={member?.emergencyContactName ?? ""} />
        <Field
          label="Contact phone"
          name="emergencyContactPhone"
          type="tel"
          defaultValue={member?.emergencyContactPhone ?? ""}
        />
        <Field
          label="Relationship"
          name="emergencyContactRelationship"
          defaultValue={member?.emergencyContactRelationship ?? ""}
        />
      </div>

      <label className="flex items-start gap-2.5 text-sm text-[#4a4a44]">
        <input
          type="checkbox"
          name="waiverAccepted"
          defaultChecked={member?.waiverAccepted ?? false}
          className="mt-0.5 h-4 w-4 accent-[#24241f]"
        />
        Waiver accepted
      </label>

      {error && <ErrorBanner message={error} />}

      <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
        <button
          type="button"
          onClick={onClose}
          className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
        >
          {loading ? "Saving…" : isEdit ? "Save changes" : "Continue"}
          {!isEdit && <ArrowRight className="size-3.5" />}
        </button>
      </div>
    </form>
  );
}

function PlanStep({
  gymId,
  member,
  plans,
  onDone,
}: {
  gymId: number;
  member: Member;
  plans: MembershipPlan[];
  onDone: () => void;
}) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConfirm = async () => {
    if (!selectedId) return;
    setSaving(true);
    setError(null);
    try {
      await subscribeMember(gymId, member.id, selectedId);
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not assign this plan.");
      setSaving(false);
    }
  };

  return (
    <div className="flex flex-col gap-5 p-5">
      <p className="text-sm text-[#5a5a54]">
        {member.firstName} {member.lastName} has been added. Every member needs a plan — choose one to finish.
        It won&apos;t become their active plan until you take payment for it from the member list.
      </p>

      <PlanPicker plans={plans} selectedId={selectedId} onSelect={setSelectedId} />

      {error && <ErrorBanner message={error} />}

      <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
        <button
          type="button"
          onClick={handleConfirm}
          disabled={!selectedId || saving}
          className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
        >
          {saving ? "Assigning…" : "Assign plan & finish"}
        </button>
      </div>
    </div>
  );
}

function FeeStep({
  gymId,
  member,
  onDone,
}: {
  gymId: number;
  member: Member;
  onDone: () => void;
}) {
  const [gym, setGym] = useState<Gym | null>(null);
  const [loadingGym, setLoadingGym] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [recording, setRecording] = useState(false);

  const fee = gym?.joiningFee ?? null;
  const currency = gym?.joiningFeeCurrency ?? "INR";

  useEffect(() => {
    getGym(gymId)
      .then(setGym)
      .catch((err) => setLoadError(err instanceof Error ? err.message : "Could not load gym payment details."))
      .finally(() => setLoadingGym(false));
  }, [gymId]);

  const handlePaymentDone = async () => {
    setConfirmOpen(false);
    setError(null);
    setRecording(true);
    try {
      await payJoiningFee(gymId, member.id, method, `Joining fee for ${member.firstName} ${member.lastName}`);
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not record this payment.");
      setRecording(false);
    }
  };

  const handleGenerateQr = async () => {
    if (!gym?.upiId || fee == null) return;
    setError(null);
    setGenerating(true);
    try {
      const note = `Joining fee for ${member.firstName} ${member.lastName}`;
      const upiUri = `upi://pay?pa=${encodeURIComponent(gym.upiId)}&pn=${encodeURIComponent(gym.name)}&am=${fee.toFixed(2)}&cu=${currency}&tn=${encodeURIComponent(note)}`;
      const dataUrl = await QRCode.toDataURL(upiUri, { width: 240, margin: 1 });
      setQrDataUrl(dataUrl);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not generate the QR code.");
    } finally {
      setGenerating(false);
    }
  };

  if (loadingGym) {
    return <p className="p-5 text-sm text-[#76766f]">Loading…</p>;
  }

  if (loadError || fee == null) {
    return (
      <div className="flex flex-col gap-4 p-5">
        {loadError && <ErrorBanner message={loadError} />}
        <p className="rounded-xl border border-dashed border-[#d8d8d1] p-4 text-sm text-[#76766f]">
          {loadError
            ? "Couldn't confirm this gym's joining fee, so it can't be charged right now."
            : "This gym hasn't set a joining fee yet — add one from Settings, then come back to charge new members."}
        </p>
        <div className="flex justify-end border-t border-[#e5e5de] pt-5">
          <button
            type="button"
            onClick={onDone}
            className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
          >
            Finish
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5 p-5">
      <p className="text-sm text-[#5a5a54]">
        {member.firstName} owes a one-time joining fee of <strong>{currency} {fee}</strong> to finish registration.
        This isn&apos;t optional — collect it now to complete adding this member.
      </p>

      <div className="flex items-end gap-3">
        <label className="flex flex-1 flex-col gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Payment method</span>
          <select
            value={method}
            onChange={(event) => {
              setMethod(event.target.value as PaymentMethod);
              setQrDataUrl(null);
            }}
            className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
          >
            {JOINING_FEE_METHODS.map((m) => (
              <option key={m} value={m}>
                {paymentMethodLabel(m)}
              </option>
            ))}
          </select>
        </label>
        {method === "UPI" && gym?.upiId && (
          <button
            type="button"
            onClick={handleGenerateQr}
            disabled={generating}
            className="h-10 shrink-0 border border-[#24241f] bg-[#24241f] px-4 text-xs font-bold text-white transition hover:bg-[#42423a] disabled:opacity-50"
          >
            {generating ? "Generating…" : "Generate QR"}
          </button>
        )}
      </div>

      {method === "UPI" && !gym?.upiId && (
        <p className="text-xs text-[#8a5a1c]">No UPI ID set up for this gym — collect payment another way, or add one in Settings.</p>
      )}

      {qrDataUrl && (
        <div className="flex flex-col items-center gap-2 border border-[#d8d8d1] bg-[#fafaf6] p-5">
          {/* eslint-disable-next-line @next/next/no-img-element -- generated data: URL, not a static asset */}
          <img src={qrDataUrl} alt={`UPI payment QR code for ${currency} ${fee}`} className="size-56" />
          <p className="text-center text-xs text-[#76766f]">
            Scan with any UPI app to pay {currency} {fee} to {gym?.upiId}
          </p>
        </div>
      )}

      {error && <ErrorBanner message={error} />}

      <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
        <button
          type="button"
          onClick={() => setConfirmOpen(true)}
          disabled={recording}
          className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
        >
          {recording ? "Recording…" : "Payment received & finish"}
        </button>
      </div>

      {confirmOpen && (
        <ConfirmDialog
          title="Payment received?"
          description={`Confirm you've received ${currency} ${fee} from ${member.firstName} via ${paymentMethodLabel(method)}. This will be recorded as a payment and finish their registration.`}
          confirmLabel="Yes, received"
          onConfirm={handlePaymentDone}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </div>
  );
}

export default function MemberFormModal({
  gymId,
  member,
  onClose,
  onSaved,
}: {
  gymId: number;
  member: Member | null;
  onClose: () => void;
  onSaved: (member: Member) => void;
}) {
  const isEdit = member !== null;
  const [step, setStep] = useState<"details" | "plan" | "fee">("details");
  const [createdMember, setCreatedMember] = useState<Member | null>(null);

  // Every member must be on a plan, so for a new member we check plans exist before letting them in.
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [plansLoading, setPlansLoading] = useState(!isEdit);
  const [plansError, setPlansError] = useState<string | null>(null);

  useEffect(() => {
    if (isEdit) return;
    listMembershipPlans(gymId)
      .then((data) => setPlans(data.filter((p) => p.active)))
      .catch((err) => setPlansError(err instanceof Error ? err.message : "Could not load membership plans."))
      .finally(() => setPlansLoading(false));
  }, [gymId, isEdit]);

  const handleDetailsSaved = (saved: Member) => {
    if (isEdit) {
      onSaved(saved);
      return;
    }
    setCreatedMember(saved);
    setStep("plan");
  };

  const handlePlanDone = () => {
    setStep("fee");
  };

  const handleFeeDone = () => {
    if (createdMember) onSaved(createdMember);
  };

  const blockedNoPlans = !isEdit && !plansLoading && !plansError && plans.length === 0;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-xl overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">
              {isEdit
                ? "Edit member"
                : step === "details"
                  ? "New member · 1 of 3"
                  : step === "plan"
                    ? "New member · 2 of 3"
                    : "New member · 3 of 3"}
            </p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {isEdit
                ? `${member!.firstName} ${member!.lastName}`
                : step === "details"
                  ? "Add a member"
                  : step === "plan"
                    ? "Choose a plan"
                    : "One-time fee"}
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

        {!isEdit && plansLoading && (
          <p className="p-5 text-sm text-[#76766f]">Checking membership plans…</p>
        )}

        {!isEdit && plansError && (
          <div className="p-5">
            <ErrorBanner message={plansError} />
          </div>
        )}

        {blockedNoPlans && (
          <div className="flex flex-col gap-4 p-5">
            <p className="rounded-xl border border-dashed border-[#d8d8d1] p-4 text-sm text-[#76766f]">
              This gym doesn&apos;t have any active membership plans yet. Every member needs to be on a plan, so add
              a plan first, then come back to add members.
            </p>
            <div className="flex justify-end border-t border-[#e5e5de] pt-5">
              <button
                type="button"
                onClick={onClose}
                className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]"
              >
                Close
              </button>
            </div>
          </div>
        )}

        {(isEdit || (!plansLoading && !plansError && plans.length > 0)) &&
          (step === "details" || isEdit ? (
            <DetailsStep gymId={gymId} member={member} onSaved={handleDetailsSaved} onClose={onClose} />
          ) : step === "plan" ? (
            createdMember && <PlanStep gymId={gymId} member={createdMember} plans={plans} onDone={handlePlanDone} />
          ) : (
            createdMember && <FeeStep gymId={gymId} member={createdMember} onDone={handleFeeDone} />
          ))}
      </div>
    </div>,
    document.body
  );
}
