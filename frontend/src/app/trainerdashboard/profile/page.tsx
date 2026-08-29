"use client";

/* Training Ledger page: Coach profile — the trainer's own real record, editable in place. */
import { useEffect, useState } from "react";
import { useSession } from "@/hooks/useSession";
import { getTrainer, updateTrainer, type Trainer, type TrainerInput } from "@/lib/trainers";
import { getTrainerCompensation, type TrainerCompensation } from "@/lib/trainerCompensation";
import ErrorBanner from "@/components/dashboard/ErrorBanner";

function Field({
  label,
  name,
  type = "text",
  defaultValue,
  placeholder,
  required,
}: {
  label: string;
  name: string;
  type?: string;
  defaultValue?: string;
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-[10px] font-bold uppercase tracking-[0.13em] text-[#8a8a82]">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        defaultValue={defaultValue}
        placeholder={placeholder}
        className="border border-[#d8d8d1] bg-[#fafaf6] p-3 text-sm text-[#24241f] outline-none focus:border-[#24241f]"
      />
    </label>
  );
}

function initialsOf(trainer: Trainer) {
  return `${trainer.firstName[0] ?? ""}${trainer.lastName[0] ?? ""}`.toUpperCase();
}

function payLabel(compensation: TrainerCompensation | null) {
  if (!compensation) return "Not set yet";
  if (compensation.payType === "HOURLY") {
    return `${compensation.currency} ${compensation.hourlyRate} / hour`;
  }
  return `${compensation.currency} ${compensation.monthlySalary} / month`;
}

export default function ProfilePage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;
  const trainerId = session?.trainerId ?? null;

  const [trainer, setTrainer] = useState<Trainer | null>(null);
  const [compensation, setCompensation] = useState<TrainerCompensation | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!gymId || !trainerId) return;
    Promise.all([getTrainer(gymId, trainerId), getTrainerCompensation(gymId, trainerId)])
      .then(([trainerData, compensationData]) => {
        setTrainer(trainerData);
        setCompensation(compensationData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your profile."))
      .finally(() => setLoading(false));
  }, [gymId, trainerId]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!gymId || !trainerId || !trainer) return;
    setError(null);
    setSaved(false);
    setSaving(true);

    const data = new FormData(event.currentTarget);
    const input: TrainerInput = {
      firstName: data.get("firstName") as string,
      lastName: data.get("lastName") as string,
      email: data.get("email") as string,
      phone: data.get("phone") as string,
      specialization: (data.get("specialization") as string) || undefined,
      bio: (data.get("bio") as string) || undefined,
      hireDate: trainer.hireDate ?? undefined,
      imageUrl: (data.get("imageUrl") as string) || undefined,
      certificateUrl: (data.get("certificateUrl") as string) || undefined,
    };

    try {
      const updated = await updateTrainer(gymId, trainerId, input);
      setTrainer(updated);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save your profile.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <p className="page-enter p-8 text-center text-sm text-[#76766f]">Loading your profile…</p>;
  }

  if (!trainer) {
    return (
      <div className="page-enter">
        <ErrorBanner message={error ?? "Could not load your profile."} />
      </div>
    );
  }

  return (
    <div className="page-enter space-y-7">
      <section className="grid gap-4 lg:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white lg:col-span-4">
          <div className="relative flex h-64 w-full items-center justify-center bg-[#24241f]">
            {trainer.imageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element -- gym-provided URL, not a static asset
              <img
                src={trainer.imageUrl}
                alt={`${trainer.firstName} ${trainer.lastName}`}
                className="h-full w-full object-cover"
              />
            ) : (
              <span className="grid size-24 place-items-center rounded-full bg-[#4b4b42] text-2xl font-bold text-[#c7f36a]">
                {initialsOf(trainer)}
              </span>
            )}
          </div>
          <div className="p-5">
            <h2 className="display-face mt-1 text-2xl">
              {trainer.firstName} {trainer.lastName}
            </h2>
            <p className="mt-1 text-sm text-[#6e6e67]">{trainer.specialization || "General coaching"}</p>
            <div className="mt-4 border-t border-[#e7e7e1] pt-4">
              <p className="text-[10px] font-bold uppercase tracking-[0.13em] text-[#8a8a82]">Your pay</p>
              <p className="mt-1 text-sm font-bold">{payLabel(compensation)}</p>
            </div>
          </div>
        </div>

        <div className="border border-[#d8d8d1] bg-white lg:col-span-8">
          <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
            <div>
              <p className="ledger-label">Coaching profile</p>
              <h3 className="mt-2 text-lg font-bold tracking-[-0.02em]">Your details</h3>
            </div>
          </div>
          <form className="grid gap-5 p-5 sm:grid-cols-2" onSubmit={handleSubmit}>
            <Field label="First name" name="firstName" required defaultValue={trainer.firstName} />
            <Field label="Last name" name="lastName" required defaultValue={trainer.lastName} />
            <Field label="Email" name="email" type="email" required defaultValue={trainer.email ?? ""} />
            <Field label="Phone" name="phone" type="tel" required defaultValue={trainer.phone} />
            <Field
              label="Specialization"
              name="specialization"
              defaultValue={trainer.specialization ?? ""}
              placeholder="Strength conditioning"
            />
            <Field label="Photo URL" name="imageUrl" defaultValue={trainer.imageUrl ?? ""} />
            <Field label="Certificate URL" name="certificateUrl" defaultValue={trainer.certificateUrl ?? ""} />

            <label className="flex flex-col gap-2 sm:col-span-2">
              <span className="text-[10px] font-bold uppercase tracking-[0.13em] text-[#8a8a82]">Bio</span>
              <textarea
                name="bio"
                defaultValue={trainer.bio ?? ""}
                rows={3}
                className="border border-[#d8d8d1] bg-[#fafaf6] p-3 text-sm text-[#24241f] outline-none focus:border-[#24241f]"
              />
            </label>

            {error && (
              <div className="sm:col-span-2">
                <ErrorBanner message={error} />
              </div>
            )}

            <div className="flex items-center gap-3 sm:col-span-2">
              <button
                type="submit"
                disabled={saving}
                className="bg-[#c7f36a] px-3.5 py-2.5 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
              >
                {saving ? "Saving…" : "Save changes"}
              </button>
              {saved && <span className="text-xs font-semibold text-[#4c592e]">Saved.</span>}
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}
