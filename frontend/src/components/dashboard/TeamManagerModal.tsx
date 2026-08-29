"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import {
  inviteManager,
  updateManagerAccess,
  type ManagerAccessScope,
  type TeamManager,
} from "@/lib/team";

const SCOPES: { value: ManagerAccessScope; label: string; description: string }[] = [
  { value: "FINANCE", label: "Finance & payroll", description: "Pay rates, payroll runs, and compensation for the whole team." },
  { value: "SETTINGS", label: "Team & settings", description: "Gym profile, and inviting or removing other managers." },
  { value: "TRAINERS", label: "Trainers", description: "Add, edit, and remove trainers on the roster." },
];

function Field({
  label,
  name,
  type = "text",
  required,
}: {
  label: string;
  name: string;
  type?: string;
  required?: boolean;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
      />
    </label>
  );
}

export default function TeamManagerModal({
  gymId,
  manager,
  onClose,
  onSaved,
}: {
  gymId: number;
  manager: TeamManager | null;
  onClose: () => void;
  onSaved: (manager: TeamManager) => void;
}) {
  const isEdit = manager !== null;
  const [scopes, setScopes] = useState<Set<ManagerAccessScope>>(new Set(manager?.scopes ?? []));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggleScope = (scope: ManagerAccessScope) => {
    setScopes((prev) => {
      const next = new Set(prev);
      if (next.has(scope)) next.delete(scope);
      else next.add(scope);
      return next;
    });
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isEdit) {
        const saved = await updateManagerAccess(gymId, manager!.id, Array.from(scopes));
        onSaved(saved);
        return;
      }
      const data = new FormData(event.currentTarget);
      const saved = await inviteManager(gymId, {
        email: data.get("email") as string,
        firstName: data.get("firstName") as string,
        lastName: data.get("lastName") as string,
        phone: (data.get("phone") as string) || undefined,
        scopes: Array.from(scopes),
      });
      onSaved(saved);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save this manager.");
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="w-full max-w-lg border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">{isEdit ? "Edit access" : "Team access"}</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {isEdit ? `${manager!.firstName} ${manager!.lastName}` : "Add a manager"}
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

        <form className="flex flex-col gap-6 p-5" onSubmit={handleSubmit}>
          {!isEdit && (
            <>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="First name" name="firstName" required />
                <Field label="Last name" name="lastName" required />
                <Field label="Email" name="email" type="email" required />
                <Field label="Phone" name="phone" type="tel" />
              </div>
              <p className="-mt-3 text-[11px] text-[#76766f]">
                This email becomes their login for the manager dashboard — no password needed, just this email and a
                code (or Google sign-in).
              </p>
            </>
          )}

          <div className="flex flex-col gap-3">
            <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
              Access — beyond the default staff &amp; member tools
            </span>
            {SCOPES.map((scope) => (
              <label
                key={scope.value}
                className="flex cursor-pointer items-start gap-3 border border-[#d8d8d1] p-3 transition hover:border-[#24241f]"
              >
                <input
                  type="checkbox"
                  checked={scopes.has(scope.value)}
                  onChange={() => toggleScope(scope.value)}
                  className="mt-0.5 h-4 w-4 accent-[#24241f]"
                />
                <span>
                  <span className="block text-sm font-bold">{scope.label}</span>
                  <span className="mt-0.5 block text-xs text-[#76766f]">{scope.description}</span>
                </span>
              </label>
            ))}
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
              {loading ? "Saving…" : isEdit ? "Save access" : "Send invite"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
