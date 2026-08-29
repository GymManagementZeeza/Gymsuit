"use client";

/* Training Ledger page: Member profile — your details, your membership, your way. */
import { useEffect, useState } from "react";
import { Avatar, StatusPill } from "@/components/dashboard/ui";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { getMember, type Member } from "@/lib/members";
import { getGym, type Gym } from "@/lib/gyms";
import { getCurrentSubscription, type MemberSubscription } from "@/lib/memberSubscriptions";

function memberSince(joinDate: string) {
  return new Date(`${joinDate}T00:00:00`).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

export default function ProfilePage() {
  const session = useSession();
  const [member, setMember] = useState<Member | null>(null);
  const [gym, setGym] = useState<Gym | null>(null);
  const [subscription, setSubscription] = useState<MemberSubscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session?.gymId || !session.memberId) return;
    Promise.all([
      getMember(session.gymId, session.memberId),
      getGym(session.gymId),
      getCurrentSubscription(session.gymId, session.memberId),
    ])
      .then(([memberData, gymData, subscriptionData]) => {
        setMember(memberData);
        setGym(gymData);
        setSubscription(subscriptionData);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load your profile."))
      .finally(() => setLoading(false));
  }, [session?.gymId, session?.memberId]);

  if (loading) {
    return <div className="page-enter p-8 text-center text-sm text-[#76766f]">Loading your profile…</div>;
  }

  if (error || !member) {
    return (
      <div className="page-enter">
        <ErrorBanner message={error ?? "Could not load your profile."} />
      </div>
    );
  }

  const initials = `${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase();
  const emergencyContact = member.emergencyContactName
    ? `${member.emergencyContactName}${member.emergencyContactPhone ? ` · ${member.emergencyContactPhone}` : ""}`
    : "Not on file";

  const details = [
    { label: "Email", value: member.email ?? "Not on file" },
    { label: "Phone", value: member.phone },
    { label: "Emergency contact", value: emergencyContact },
    { label: "Home gym", value: gym?.name ?? "—" },
  ];

  return (
    <div className="page-enter space-y-7">
      <section className="grid gap-4 lg:grid-cols-12">
        <div className="flex flex-col items-center gap-4 border border-[#d8d8d1] bg-white p-6 text-center lg:col-span-4">
          <Avatar initials={initials} tone="peach" size="lg" />
          <h2 className="display-face text-2xl">
            {member.firstName} {member.lastName}
          </h2>
          <p className="text-sm text-[#6e6e67]">Member since {memberSince(member.joinDate)}</p>
          <StatusPill
            label={subscription ? `Active · ${subscription.planName}` : "No active plan"}
            tone={subscription ? "blue" : "orange"}
          />
        </div>

        <div className="border border-[#d8d8d1] bg-white lg:col-span-8">
          <div className="flex items-center justify-between border-b border-[#e7e7e1] p-5">
            <div>
              <p className="ledger-label">My details</p>
              <h3 className="mt-2 text-lg font-bold tracking-[-0.02em]">Your member information</h3>
            </div>
          </div>
          <div className="grid gap-5 p-5 sm:grid-cols-2">
            {details.map((item) => (
              <div key={item.label}>
                <p className="text-[10px] font-bold uppercase tracking-[0.13em] text-[#8a8a82]">{item.label}</p>
                <p className="mt-1 text-sm font-bold">{item.value}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
