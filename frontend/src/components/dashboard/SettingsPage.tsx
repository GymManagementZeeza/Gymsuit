"use client";

/* Training Ledger page: System settings use explicit domains and grounded configuration summaries. */
import { useEffect, useState } from "react";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, StatusPill } from "@/components/dashboard/ui";
import GymProfileModal from "@/components/dashboard/GymProfileModal";
import UpiIdModal from "@/components/UpiIdModal";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { getGym, type Gym } from "@/lib/gyms";
import { BellRing, Building2, Check, CreditCard, Globe2, Image as ImageIcon, LockKeyhole, Save, UsersRound } from "lucide-react";

const otherSettings = [
  { icon: ImageIcon, title: "Branding", text: "Logo, member portal cover, colors, and email signature", action: "Manage branding", status: "Updated" },
  { icon: UsersRound, title: "Roles & access", text: "Owner, manager, trainer, and member permissions", action: "Review roles", status: "4 roles" },
  { icon: BellRing, title: "Notifications", text: "Session requests, expiring certifications, payment reminders", action: "Edit alerts", status: "8 enabled" },
  { icon: Globe2, title: "Regional format", text: "Currency, language, date and time formatting", action: "Edit regional settings", status: "USD · English" },
];

function gymProfileSummary(gym: Gym) {
  const parts = [gym.addressLine, gym.city, gym.phone, gym.email].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "No address, phone, or contact email on file yet";
}

function gymProfileComplete(gym: Gym) {
  return Boolean(gym.addressLine && gym.phone && gym.email);
}

export default function SettingsPage() {
  const session = useSession();
  const gymId = session?.gymId ?? null;

  const [gym, setGym] = useState<Gym | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [editingUpi, setEditingUpi] = useState(false);

  useEffect(() => {
    if (!gymId) return;
    getGym(gymId)
      .then(setGym)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load the gym profile."))
      .finally(() => setLoading(false));
  }, [gymId]);

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow="System · Settings"
        title={
          <>
            Set the standard
            <br />
            <em className="font-normal">for your gym.</em>
          </>
        }
        description="The details members never have to think about — because you did."
        actions={<ActionButton icon={<Save className="size-4" />}>Save changes</ActionButton>}
      />
      <section className="grid gap-4 xl:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
          <div className="border-b border-[#e7e7e1] p-5">
            <p className="ledger-label">{gym?.name ?? "Your gym"}</p>
            <h2 className="mt-2 text-xl font-bold">Gym setup</h2>
          </div>

          {error && (
            <div className="p-5">
              <ErrorBanner message={error} />
            </div>
          )}

          <div className="divide-y divide-[#e8e8e2]">
            <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
              <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                <Building2 className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold">Gym profile</p>
                  {gym && (
                    <StatusPill
                      label={gymProfileComplete(gym) ? "Complete" : "Incomplete"}
                      tone={gymProfileComplete(gym) ? "blue" : "orange"}
                    />
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-[#72726b]">
                  {loading ? "Loading…" : gym ? gymProfileSummary(gym) : "Could not load gym details"}
                </p>
              </div>
              <button
                onClick={() => setEditing(true)}
                disabled={!gym}
                className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f] disabled:opacity-50"
              >
                Edit profile
              </button>
            </div>

            <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
              <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                <CreditCard className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold">Online payments</p>
                  {gym && (
                    <StatusPill label={gym.upiId ? "Connected" : "Not set"} tone={gym.upiId ? "blue" : "orange"} />
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-[#72726b]">
                  {loading
                    ? "Loading…"
                    : gym?.upiId
                      ? `UPI QR codes for one-time fees use ${gym.upiId}`
                      : "Add a UPI ID to generate payment QR codes for one-time fees"}
                </p>
              </div>
              <button
                onClick={() => setEditingUpi(true)}
                disabled={!gym}
                className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f] disabled:opacity-50"
              >
                {gym?.upiId ? "Edit UPI ID" : "Add UPI ID"}
              </button>
            </div>

            {otherSettings.map((item) => (
              <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center" key={item.title}>
                <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                  <item.icon className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-bold">{item.title}</p>
                    <StatusPill label={item.status} tone={item.status === "Setup" ? "orange" : "blue"} />
                  </div>
                  <p className="mt-1 text-xs leading-relaxed text-[#72726b]">{item.text}</p>
                </div>
                <button className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f]">{item.action}</button>
              </div>
            ))}
          </div>
        </div>
        <aside className="cut-corner bg-[#24241f] p-5 text-white xl:col-span-4">
          <div className="flex items-center justify-between">
            <LockKeyhole className="size-6 text-[#c7f36a]" />
            <StatusPill label="Owner controlled" tone="lime" />
          </div>
          <p className="display-face mt-7 text-3xl leading-[1.05]">
            Good systems
            <br />
            protect the floor.
          </p>
          <p className="mt-4 text-xs leading-relaxed text-[#b8b8af]">
            Access is scoped to each gym. Team members see only the data they need, and owner-level changes remain deliberate.
          </p>
          <button className="mt-7 flex items-center gap-2 border border-white/25 px-3 py-2 text-xs font-bold transition hover:bg-white hover:text-[#24241f]">
            <Check className="size-3.5 text-[#c7f36a]" /> View audit log
          </button>
        </aside>
      </section>

      {editing && gymId && gym && (
        <GymProfileModal
          gymId={gymId}
          gym={gym}
          onClose={() => setEditing(false)}
          onSaved={(updated) => {
            setGym(updated);
            setEditing(false);
          }}
        />
      )}

      {editingUpi && gymId && gym && (
        <UpiIdModal
          gymId={gymId}
          gym={gym}
          onClose={() => setEditingUpi(false)}
          onSaved={(updated) => {
            setGym(updated);
            setEditingUpi(false);
          }}
        />
      )}
    </div>
  );
}
