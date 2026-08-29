"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { ArrowRight, X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { createTrainer, updateTrainer, type Trainer, type TrainerInput } from "@/lib/trainers";
import { setTrainerCompensation, type PayType } from "@/lib/trainerCompensation";

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
  trainer,
  nextStepLabel,
  onSaved,
  onClose,
}: {
  gymId: number;
  trainer: Trainer | null;
  nextStepLabel: string;
  onSaved: (trainer: Trainer) => void;
  onClose: () => void;
}) {
  const isEdit = trainer !== null;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const data = new FormData(event.currentTarget);
    const input: TrainerInput = {
      firstName: data.get("firstName") as string,
      lastName: data.get("lastName") as string,
      email: data.get("email") as string,
      phone: data.get("phone") as string,
      specialization: (data.get("specialization") as string) || undefined,
      bio: (data.get("bio") as string) || undefined,
      hireDate: (data.get("hireDate") as string) || undefined,
      imageUrl: (data.get("imageUrl") as string) || undefined,
      certificateUrl: (data.get("certificateUrl") as string) || undefined,
    };

    try {
      const saved = isEdit ? await updateTrainer(gymId, trainer!.id, input) : await createTrainer(gymId, input);
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save this trainer.");
      setLoading(false);
    }
  };

  return (
    <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="First name" name="firstName" required defaultValue={trainer?.firstName} />
        <Field label="Last name" name="lastName" required defaultValue={trainer?.lastName} />
        <Field label="Email" name="email" type="email" required defaultValue={trainer?.email ?? ""} />
        <Field label="Phone" name="phone" type="tel" required defaultValue={trainer?.phone} />
        <Field label="Specialization" name="specialization" defaultValue={trainer?.specialization ?? ""} placeholder="Strength conditioning" />
        <Field label="Hire date" name="hireDate" type="date" defaultValue={trainer?.hireDate ?? ""} />
      </div>

      {!isEdit && (
        <p className="-mt-3 text-[11px] text-[#76766f]">
          This email becomes their login for the trainer dashboard — no password needed, just this email and a
          code (or Google sign-in).
        </p>
      )}

      <label className="flex flex-col gap-1.5">
        <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Bio</span>
        <textarea
          name="bio"
          rows={3}
          defaultValue={trainer?.bio ?? ""}
          className="border border-[#d8d8d1] bg-white p-3 text-sm outline-none transition-colors focus:border-[#24241f]"
        />
      </label>

      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Photo URL" name="imageUrl" defaultValue={trainer?.imageUrl ?? ""} />
        <Field label="Certificate URL" name="certificateUrl" defaultValue={trainer?.certificateUrl ?? ""} />
      </div>

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
          {loading ? "Saving…" : isEdit ? "Save changes" : nextStepLabel}
          {!isEdit && <ArrowRight className="size-3.5" />}
        </button>
      </div>
    </form>
  );
}

function PayStep({
  gymId,
  trainer,
  onDone,
  onSkip,
}: {
  gymId: number;
  trainer: Trainer;
  onDone: () => void;
  onSkip: () => void;
}) {
  const [payType, setPayType] = useState<PayType>("HOURLY");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSaving(true);

    const data = new FormData(event.currentTarget);
    const currency = data.get("currency") as string;
    const rate = data.get("rate") as string;

    try {
      await setTrainerCompensation(gymId, trainer.id, {
        payType,
        currency,
        hourlyRate: payType === "HOURLY" ? Number(rate) : undefined,
        monthlySalary: payType === "SALARY" ? Number(rate) : undefined,
      });
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save their pay.");
      setSaving(false);
    }
  };

  return (
    <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
      <p className="text-sm text-[#5a5a54]">
        {trainer.firstName} has been added. Set how they&apos;re paid — you can change this any time from staff pay.
      </p>

      <div className="grid grid-cols-2 gap-3">
        <button
          type="button"
          onClick={() => setPayType("HOURLY")}
          className={`border p-3 text-left transition ${
            payType === "HOURLY" ? "border-[#24241f] bg-[#fafaf6]" : "border-[#d8d8d1] hover:border-[#24241f]"
          }`}
        >
          <span className="block text-sm font-bold">Hourly rate</span>
          <span className="mt-0.5 block text-xs text-[#76766f]">Paid per hour trained</span>
        </button>
        <button
          type="button"
          onClick={() => setPayType("SALARY")}
          className={`border p-3 text-left transition ${
            payType === "SALARY" ? "border-[#24241f] bg-[#fafaf6]" : "border-[#d8d8d1] hover:border-[#24241f]"
          }`}
        >
          <span className="block text-sm font-bold">Fixed monthly salary</span>
          <span className="mt-0.5 block text-xs text-[#76766f]">A flat amount every month</span>
        </button>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <Field
          label={payType === "HOURLY" ? "Hourly rate" : "Monthly salary"}
          name="rate"
          type="number"
          required
          placeholder={payType === "HOURLY" ? "500" : "40000"}
        />
        <Field label="Currency" name="currency" required defaultValue="INR" placeholder="INR" />
      </div>

      {error && <ErrorBanner message={error} />}

      <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
        <button
          type="button"
          onClick={onSkip}
          disabled={saving}
          className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2] disabled:opacity-50"
        >
          Skip for now
        </button>
        <button
          type="submit"
          disabled={saving}
          className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
        >
          {saving ? "Saving…" : "Save pay & finish"}
        </button>
      </div>
    </form>
  );
}

export default function TrainerFormModal({
  gymId,
  trainer,
  canSetPay,
  onClose,
  onSaved,
}: {
  gymId: number;
  trainer: Trainer | null;
  canSetPay: boolean;
  onClose: () => void;
  onSaved: (trainer: Trainer) => void;
}) {
  const isEdit = trainer !== null;
  const [step, setStep] = useState<"details" | "pay">("details");
  const [createdTrainer, setCreatedTrainer] = useState<Trainer | null>(null);

  const handleDetailsSaved = (saved: Trainer) => {
    if (isEdit) {
      onSaved(saved);
      return;
    }
    if (!canSetPay) {
      onSaved(saved);
      return;
    }
    setCreatedTrainer(saved);
    setStep("pay");
  };

  const handlePayDone = () => {
    if (createdTrainer) onSaved(createdTrainer);
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-xl overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">
              {isEdit ? "Edit trainer" : step === "details" ? "New trainer · 1 of 2" : "New trainer · 2 of 2"}
            </p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {isEdit
                ? `${trainer!.firstName} ${trainer!.lastName}`
                : step === "details"
                  ? "Add a trainer"
                  : "Set their pay"}
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

        {step === "details" || isEdit ? (
          <DetailsStep
            gymId={gymId}
            trainer={trainer}
            nextStepLabel={canSetPay ? "Continue" : "Add trainer"}
            onSaved={handleDetailsSaved}
            onClose={onClose}
          />
        ) : (
          createdTrainer && (
            <PayStep gymId={gymId} trainer={createdTrainer} onDone={handlePayDone} onSkip={handlePayDone} />
          )
        )}
      </div>
    </div>,
    document.body
  );
}
