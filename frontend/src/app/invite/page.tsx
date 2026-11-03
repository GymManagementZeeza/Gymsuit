"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { acceptTrainerInvite, previewTrainerInvite, type TrainerInvitePreview } from "@/lib/trainerInvites";

/**
 * Public trainer-invite accept page, opened from the emailed link
 * (/invite?token=...). No login and no directory access: the token is the
 * credential. An existing member of the gym accepts with one tap; a new
 * client fills in the few details needed to create their member record,
 * which is then assigned to the inviting trainer.
 */
function InviteAccept() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [preview, setPreview] = useState<TrainerInvitePreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [waiverAccepted, setWaiverAccepted] = useState(false);
  const [accepting, setAccepting] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [acceptMessage, setAcceptMessage] = useState("");

  useEffect(() => {
    if (!token) {
      setError("This invitation link is incomplete — ask your trainer to send it again.");
      setLoading(false);
      return;
    }
    previewTrainerInvite(token)
      .then((data) => {
        setPreview(data);
        setFirstName(data.firstName ?? "");
        setLastName(data.lastName ?? "");
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load this invitation."))
      .finally(() => setLoading(false));
  }, [token]);

  const handleAccept = async () => {
    if (!token || !preview) return;
    if (!preview.existingMember && (!firstName.trim() || !lastName.trim() || !phone.trim())) {
      setError("First name, last name and phone are needed to create your membership.");
      return;
    }
    setAccepting(true);
    setError(null);
    try {
      const result = await acceptTrainerInvite(token, {
        firstName: firstName.trim() || undefined,
        lastName: lastName.trim() || undefined,
        phone: phone.trim() || undefined,
        waiverAccepted,
      });
      setAcceptMessage(result.message);
      setAccepted(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not accept this invitation.");
    } finally {
      setAccepting(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4 py-12">
      <h1 className="text-2xl font-bold tracking-[-0.02em]">Trainer invitation</h1>
      {loading && <p className="mt-6 text-sm text-[#76766f]">Loading invitation…</p>}
      {error && !loading && (
        <div className="mt-6 border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}
      {!loading && preview && preview.status !== "PENDING" && (
        <p className="mt-6 text-sm text-[#76766f]">
          This invitation is {preview.status.toLowerCase()} and can no longer be accepted. Ask your trainer
          to send a new one.
        </p>
      )}
      {!loading && preview && preview.status === "PENDING" && !accepted && (
        <div className="mt-6 space-y-4">
          <p className="text-sm">
            <strong>{preview.trainerName}</strong> from <strong>{preview.gymName}</strong> invited{" "}
            <strong>{preview.email}</strong> to become their client on GymSuit.
          </p>
          {preview.existingMember ? (
            <p className="text-sm text-[#76766f]">
              You already have a membership at this gym — accepting moves you to {preview.trainerName} as
              your trainer.
            </p>
          ) : (
            <>
              <p className="text-sm text-[#76766f]">
                You don&apos;t have a membership at this gym yet, so a few details are needed to create it.
                Your sign-in afterwards is the same email + OTP sign-in as everyone else.
              </p>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">
                    First name
                  </label>
                  <input
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="mt-1 h-10 w-full border border-[#d8d8d1] px-3 text-sm outline-none"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">
                    Last name
                  </label>
                  <input
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="mt-1 h-10 w-full border border-[#d8d8d1] px-3 text-sm outline-none"
                  />
                </div>
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a8a82]">Phone</label>
                <input
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="mt-1 h-10 w-full border border-[#d8d8d1] px-3 text-sm outline-none"
                />
              </div>
              <label className="flex items-start gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={waiverAccepted}
                  onChange={(e) => setWaiverAccepted(e.target.checked)}
                  className="mt-1"
                />
                I accept the gym&apos;s waiver.
              </label>
            </>
          )}
          {error && (
            <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
          )}
          <button
            type="button"
            onClick={handleAccept}
            disabled={accepting}
            className="h-11 w-full bg-[#c7f36a] text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-50"
          >
            {accepting ? "Accepting…" : `Accept — train with ${preview.trainerName}`}
          </button>
          <p className="text-xs text-[#8a8a82]">
            Expires {new Date(preview.expiresAt).toLocaleDateString()}. If you weren&apos;t expecting this,
            just ignore it.
          </p>
        </div>
      )}
      {accepted && (
        <div className="mt-6 space-y-4">
          <p className="text-sm font-bold">{acceptMessage}</p>
          <p className="text-sm text-[#76766f]">
            Sign in to GymSuit with {preview?.email} to see your trainer and sessions.
          </p>
          <Link href="/login" className="inline-block bg-[#c7f36a] px-4 py-2 text-sm font-bold">
            Sign in
          </Link>
        </div>
      )}
    </div>
  );
}

export default function InvitePage() {
  return (
    <Suspense fallback={null}>
      <InviteAccept />
    </Suspense>
  );
}
