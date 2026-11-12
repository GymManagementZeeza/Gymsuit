"use client";

/* Nutrition desk: the clients who accepted this expert's invites, plus sending and tracking invites. */
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Avatar } from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import ConfirmDialog from "@/components/dashboard/ConfirmDialog";
import { useSession } from "@/hooks/useSession";
import {
  listNutritionClients,
  listNutritionInvites,
  removeNutritionClient,
  revokeNutritionInvite,
  sendNutritionInvite,
  type NutritionClient,
  type NutritionInvite,
} from "@/lib/nutritionist";
import { ChevronRight, MailPlus, Search, X } from "lucide-react";

const AVATAR_TONES = ["peach", "sky", "mint", "lavender"] as const;
const fieldClass =
  "h-10 w-full border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]";

function initialsOf(client: NutritionClient) {
  return `${client.firstName[0] ?? ""}${client.lastName[0] ?? ""}`.toUpperCase();
}

function weightLine(client: NutritionClient) {
  if (client.weightKg == null) return "No weight recorded yet";
  const goal = client.goalWeightKg != null ? ` → goal ${client.goalWeightKg} kg` : "";
  return `${client.weightKg} kg${goal}`;
}

export default function NutritionClientsPage() {
  const session = useSession("/diet");

  const [clients, setClients] = useState<NutritionClient[]>([]);
  const [invites, setInvites] = useState<NutritionInvite[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteFirstName, setInviteFirstName] = useState("");
  const [inviteLastName, setInviteLastName] = useState("");
  const [inviteSending, setInviteSending] = useState(false);
  const [inviteSentTo, setInviteSentTo] = useState<string | null>(null);
  const [inviteError, setInviteError] = useState<string | null>(null);

  const [removing, setRemoving] = useState<NutritionClient | null>(null);
  const [removeBusy, setRemoveBusy] = useState(false);

  useEffect(() => {
    if (session?.role !== "NUTRITIONIST") return;
    Promise.all([listNutritionClients(), listNutritionInvites()])
      .then(([c, i]) => {
        setClients(c);
        setInvites(i);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your clients."))
      .finally(() => setLoaded(true));
  }, [session?.role]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const sorted = [...clients].sort((a, b) => a.firstName.localeCompare(b.firstName));
    if (!q) return sorted;
    return sorted.filter((c) => `${c.firstName} ${c.lastName} ${c.email ?? ""}`.toLowerCase().includes(q));
  }, [clients, query]);

  const pendingInvites = invites.filter((i) => i.status === "PENDING");

  const handleInvite = async (event: React.FormEvent) => {
    event.preventDefault();
    if (inviteSending) return;
    setInviteError(null);
    setInviteSending(true);
    try {
      const invite = await sendNutritionInvite({
        email: inviteEmail.trim(),
        firstName: inviteFirstName.trim() || undefined,
        lastName: inviteLastName.trim() || undefined,
      });
      setInvites((prev) => [invite, ...prev.filter((i) => i.id !== invite.id)]);
      setInviteSentTo(invite.email);
      setInviteEmail("");
      setInviteFirstName("");
      setInviteLastName("");
    } catch (err) {
      setInviteError(err instanceof Error ? err.message : "Could not send the invite.");
    } finally {
      setInviteSending(false);
    }
  };

  const handleRevoke = async (invite: NutritionInvite) => {
    try {
      await revokeNutritionInvite(invite.id);
      setInvites((prev) => prev.map((i) => (i.id === invite.id ? { ...i, status: "REVOKED" } : i)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not cancel the invite.");
    }
  };

  const handleRemove = async () => {
    if (!removing) return;
    setRemoveBusy(true);
    try {
      await removeNutritionClient(removing.memberId);
      setClients((prev) => prev.filter((c) => c.memberId !== removing.memberId));
      setRemoving(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not remove this client.");
    } finally {
      setRemoveBusy(false);
    }
  };

  const closeInvite = () => {
    setInviteOpen(false);
    setInviteSentTo(null);
    setInviteError(null);
  };

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <p className="display-face text-2xl leading-[1.05] tracking-[-0.03em] sm:text-3xl">Every plate tells a story.</p>
        <button
          type="button"
          onClick={() => setInviteOpen(true)}
          className="flex h-10 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
        >
          <MailPlus className="size-4" /> Invite a client
        </button>
      </div>

      {error && (
        <div className="mb-4">
          <ErrorBanner message={error} />
        </div>
      )}

      <div className="border border-[#e1e1da] bg-white">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e1e1da] p-4">
          <div>
            <p className="ledger-label">Clients</p>
            <p className="mt-1 text-sm text-[#76766f]">
              {!loaded ? "Loading…" : `${clients.length} client${clients.length === 1 ? "" : "s"}`}
            </p>
          </div>
          <label className="flex h-10 w-full items-center gap-2 border border-[#d8d8d1] bg-white px-3 sm:w-64">
            <Search className="size-4 shrink-0 text-[#76766f]" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search clients"
              aria-label="Search clients"
              className="min-w-0 flex-1 bg-transparent text-sm outline-none"
            />
          </label>
        </div>

        <div className="divide-y divide-[#efefe9]">
          {!loaded &&
            [0, 1, 2].map((i) => (
              <div className="flex items-center gap-3 p-4" key={i}>
                <div className="size-10 shrink-0 animate-pulse rounded-full bg-[#efefe9]" />
                <div className="h-3 w-1/3 animate-pulse rounded-full bg-[#efefe9]" />
              </div>
            ))}

          {loaded && clients.length === 0 && (
            <div className="p-6 text-sm leading-6 text-[#76766f]">
              <p className="font-bold text-[#24241f]">No clients yet.</p>
              <p className="mt-1">
                Invite a client by email. Once they accept, you&apos;ll see the meals, macros and weight they log in the
                GymSuit app.
              </p>
            </div>
          )}

          {loaded && clients.length > 0 && filtered.length === 0 && (
            <p className="p-6 text-sm text-[#76766f]">No clients match your search.</p>
          )}

          {filtered.map((client, index) => (
            <div key={client.memberId} className="flex items-center gap-1 pr-3 transition hover:bg-[#f7f7f2]">
              {client.gymId != null ? (
                <Link
                  href={`/nutritiondashboard/clients/${client.gymId}/${client.memberId}`}
                  className="flex min-w-0 flex-1 items-center gap-3 p-4"
                >
                  <Avatar initials={initialsOf(client)} tone={AVATAR_TONES[index % AVATAR_TONES.length]} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold">
                      {client.firstName} {client.lastName}
                    </p>
                    <p className="mt-0.5 truncate text-xs text-[#76766f]">{weightLine(client)}</p>
                  </div>
                  <ChevronRight className="size-4 shrink-0 text-[#a3a39a]" />
                </Link>
              ) : (
                <div className="flex min-w-0 flex-1 items-center gap-3 p-4">
                  <Avatar initials={initialsOf(client)} tone={AVATAR_TONES[index % AVATAR_TONES.length]} />
                  <p className="truncate text-sm font-bold">
                    {client.firstName} {client.lastName}
                  </p>
                </div>
              )}
              <button
                type="button"
                onClick={() => setRemoving(client)}
                className="grid size-8 shrink-0 place-items-center text-[#a3a39a] transition hover:bg-[#efefe9] hover:text-[#24241f]"
                aria-label={`Remove ${client.firstName} ${client.lastName}`}
                title="Remove client"
              >
                <X className="size-4" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {pendingInvites.length > 0 && (
        <div className="mt-6 border border-[#e1e1da] bg-white">
          <div className="border-b border-[#e1e1da] p-4">
            <p className="ledger-label">Waiting to accept</p>
          </div>
          <div className="divide-y divide-[#efefe9]">
            {pendingInvites.map((invite) => (
              <div key={invite.id} className="flex flex-wrap items-center gap-3 p-4">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold">
                    {[invite.firstName, invite.lastName].filter(Boolean).join(" ") || invite.email}
                  </p>
                  <p className="mt-0.5 truncate text-xs text-[#76766f]">
                    {invite.email} · expires {new Date(invite.expiresAt).toLocaleDateString()}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleRevoke(invite)}
                  className="h-8 border border-[#d8d8d1] px-3 text-xs font-bold transition hover:bg-[#f7f7f2]"
                >
                  Cancel invite
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {inviteOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
          <div className="w-full max-w-md border border-[#d8d8d1] bg-white" role="dialog" aria-modal="true" aria-label="Invite a client">
            <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
              <div>
                <p className="ledger-label">New client</p>
                <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">Invite by email</h2>
              </div>
              <button type="button" onClick={closeInvite} className="grid size-8 place-items-center transition hover:bg-[#efefe9]" aria-label="Close">
                <X className="size-4" />
              </button>
            </div>
            <form className="flex flex-col gap-4 p-5" onSubmit={handleInvite}>
              {inviteSentTo && (
                <p className="border border-[#c7f36a] bg-[#f4fde3] p-3 text-xs text-[#24241f]">
                  Invite sent to <strong>{inviteSentTo}</strong>. Send another, or close this window.
                </p>
              )}
              <div className="grid gap-3 sm:grid-cols-2">
                <input value={inviteFirstName} onChange={(e) => setInviteFirstName(e.target.value)} placeholder="First name (optional)" aria-label="First name" className={fieldClass} />
                <input value={inviteLastName} onChange={(e) => setInviteLastName(e.target.value)} placeholder="Last name (optional)" aria-label="Last name" className={fieldClass} />
              </div>
              <input
                type="email"
                required
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                placeholder="client@email.com"
                aria-label="Client email"
                className={fieldClass}
              />
              <p className="text-[11px] leading-5 text-[#76766f]">
                They&apos;ll get a link to accept. They need the GymSuit app with this email — you&apos;ll see what they
                log there, read-only.
              </p>
              {inviteError && <ErrorBanner message={inviteError} />}
              <div className="flex justify-end gap-2 border-t border-[#e5e5de] pt-4">
                <button type="button" onClick={closeInvite} className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]">
                  Close
                </button>
                <button
                  type="submit"
                  disabled={inviteSending}
                  className="h-9 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
                >
                  {inviteSending ? "Sending…" : "Send invite"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {removing && (
        <ConfirmDialog
          title="Remove client?"
          description={`You'll stop seeing ${removing.firstName} ${removing.lastName}'s nutrition data. You can invite them again later.`}
          confirmLabel={removeBusy ? "Removing…" : "Remove"}
          onConfirm={handleRemove}
          onCancel={() => setRemoving(null)}
          disabled={removeBusy}
        />
      )}
    </div>
  );
}
