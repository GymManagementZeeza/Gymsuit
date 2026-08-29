"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { updateGym, type Gym } from "@/lib/gyms";

function Field({
  label,
  name,
  required,
  type = "text",
  defaultValue,
}: {
  label: string;
  name: string;
  required?: boolean;
  type?: string;
  defaultValue?: string;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        defaultValue={defaultValue ?? ""}
        className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
      />
    </label>
  );
}

export default function GymProfileModal({
  gymId,
  gym,
  onClose,
  onSaved,
}: {
  gymId: number;
  gym: Gym;
  onClose: () => void;
  onSaved: (gym: Gym) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    const data = new FormData(event.currentTarget);
    try {
      const saved = await updateGym(gymId, {
        name: data.get("name") as string,
        addressLine: (data.get("addressLine") as string) || undefined,
        city: (data.get("city") as string) || undefined,
        state: (data.get("state") as string) || undefined,
        postalCode: (data.get("postalCode") as string) || undefined,
        country: (data.get("country") as string) || undefined,
        phone: (data.get("phone") as string) || undefined,
        email: (data.get("email") as string) || undefined,
        logoUrl: (data.get("logoUrl") as string) || undefined,
      });
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save the gym profile.");
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-xl overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Gym profile</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Edit gym details</h2>
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

        <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Gym name" name="name" required defaultValue={gym.name} />
            <Field label="Phone" name="phone" type="tel" defaultValue={gym.phone ?? ""} />
            <Field label="Contact email" name="email" type="email" defaultValue={gym.email ?? ""} />
            <Field label="Logo URL" name="logoUrl" defaultValue={gym.logoUrl ?? ""} />
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Address" name="addressLine" defaultValue={gym.addressLine ?? ""} />
            <Field label="City" name="city" defaultValue={gym.city ?? ""} />
            <Field label="State" name="state" defaultValue={gym.state ?? ""} />
            <div className="grid grid-cols-2 gap-3">
              <Field label="Postal code" name="postalCode" defaultValue={gym.postalCode ?? ""} />
              <Field label="Country" name="country" defaultValue={gym.country ?? ""} />
            </div>
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
              {loading ? "Saving…" : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
