"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import QRCode from "qrcode";
import { ArrowRight, CheckCircle2, Eraser } from "lucide-react";
import PlanPicker from "@/components/dashboard/PlanPicker";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import {
  getPublicGym,
  listPublicPlans,
  registerPublicMember,
  type PublicGym,
  type PublicMembershipPlan,
  type PublicRegisterInput,
  type PublicRegisterResult,
} from "@/lib/publicRegistration";
import type { BillingCycle, MembershipPlan } from "@/lib/membershipPlans";

const WAIVER_TEXT = [
  "I am voluntarily joining this gym for fitness training and understand that exercise carries a risk of injury.",
  "I am medically fit to exercise, or I have told the gym about any condition that affects my training in the medical notes above.",
  "The details I gave on this form are correct.",
  "I will follow the gym's rules and the trainers' instructions.",
  "The gym may contact me about my membership on the phone number and email I gave.",
];

function Field({
  label,
  name,
  required,
  type = "text",
  placeholder,
}: {
  label: string;
  name: string;
  required?: boolean;
  type?: string;
  placeholder?: string;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">{label}</span>
      <input
        type={type}
        name={name}
        required={required}
        placeholder={placeholder}
        className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
      />
    </label>
  );
}

/** Draw-to-sign pad. Reports the PNG data URL once a stroke is finished, null after clearing. */
function SignaturePad({ onSigned }: { onSigned: (dataUrl: string | null) => void }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const drawingRef = useRef(false);
  const lastRef = useRef<{ x: number; y: number } | null>(null);
  const [hasInk, setHasInk] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = Math.max(1, Math.floor(rect.width * dpr));
    canvas.height = Math.max(1, Math.floor(rect.height * dpr));
    const ctx = canvas.getContext("2d");
    if (ctx) {
      ctx.scale(dpr, dpr);
      ctx.lineWidth = 2;
      ctx.lineCap = "round";
      ctx.strokeStyle = "#24241f";
    }
  }, []);

  const pos = (e: React.PointerEvent) => {
    const rect = canvasRef.current!.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
  };

  const clear = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const rect = canvas.getBoundingClientRect();
    ctx?.clearRect(0, 0, rect.width, rect.height);
    setHasInk(false);
    onSigned(null);
  };

  return (
    <div>
      <canvas
        ref={canvasRef}
        className="h-32 w-full touch-none border border-[#d8d8d1] bg-white"
        onPointerDown={(e) => {
          e.preventDefault();
          drawingRef.current = true;
          lastRef.current = pos(e);
          canvasRef.current?.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          if (!drawingRef.current) return;
          const canvas = canvasRef.current!;
          const ctx = canvas.getContext("2d");
          const p = pos(e);
          const last = lastRef.current;
          if (ctx && last) {
            ctx.beginPath();
            ctx.moveTo(last.x, last.y);
            ctx.lineTo(p.x, p.y);
            ctx.stroke();
          }
          lastRef.current = p;
        }}
        onPointerUp={() => {
          if (!drawingRef.current) return;
          drawingRef.current = false;
          lastRef.current = null;
          setHasInk(true);
          onSigned(canvasRef.current?.toDataURL("image/png") ?? null);
        }}
        onPointerCancel={() => {
          drawingRef.current = false;
          lastRef.current = null;
        }}
      />
      <div className="mt-2 flex items-center justify-between">
        <p className="text-xs text-[#76766f]">{hasInk ? "Signed — tap clear to redo it." : "Sign with your finger or mouse."}</p>
        <button
          type="button"
          onClick={clear}
          className="flex h-8 items-center gap-1.5 border border-[#d8d8d1] px-3 text-xs font-bold transition hover:bg-[#f7f7f2]"
        >
          <Eraser className="size-3.5" /> Clear
        </button>
      </div>
    </div>
  );
}

type Step = "details" | "plan" | "payment" | "done";

function RegisterForm() {
  const params = useParams();
  const gymId = Number(params.gymId);
  const [gym, setGym] = useState<PublicGym | null>(null);
  const [plans, setPlans] = useState<PublicMembershipPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [step, setStep] = useState<Step>("details");
  const [details, setDetails] = useState<Omit<PublicRegisterInput, "waiverAccepted" | "waiverSignature" | "planId"> | null>(null);
  const [signature, setSignature] = useState<string | null>(null);
  const [signatureError, setSignatureError] = useState(false);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [registering, setRegistering] = useState(false);
  const [registerError, setRegisterError] = useState<string | null>(null);
  const [result, setResult] = useState<PublicRegisterResult | null>(null);

  const [upiQr, setUpiQr] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(gymId) || gymId <= 0) {
      setLoadError("This registration link is incomplete.");
      setLoading(false);
      return;
    }
    Promise.all([getPublicGym(gymId), listPublicPlans(gymId)])
      .then(([gymData, planData]) => {
        setGym(gymData);
        setPlans(planData.filter((p) => p.active));
      })
      .catch((err) => setLoadError(err instanceof Error ? err.message : "Could not load this gym."))
      .finally(() => setLoading(false));
  }, [gymId]);

  const selectedPlan = plans.find((p) => p.id === selectedPlanId) ?? null;
  const upiUri =
    gym?.upiId && selectedPlan
      ? `upi://pay?pa=${encodeURIComponent(gym.upiId)}&pn=${encodeURIComponent(gym.name)}&am=${selectedPlan.price.toFixed(2)}&cu=${selectedPlan.currency}&tn=${encodeURIComponent(`${selectedPlan.name} — ${details?.firstName ?? ""} ${details?.lastName ?? ""}`.trim())}`
      : null;

  useEffect(() => {
    if (step !== "payment" || !upiUri) return;
    let cancelled = false;
    QRCode.toDataURL(upiUri, { width: 280, margin: 1 })
      .then((dataUrl) => {
        if (!cancelled) setUpiQr(dataUrl);
      })
      .catch(() => {
        if (!cancelled) setUpiQr(null);
      });
    return () => {
      cancelled = true;
    };
  }, [step, upiUri]);

  const handleDetailsSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!signature) {
      setSignatureError(true);
      return;
    }
    setSignatureError(false);
    const data = new FormData(event.currentTarget);
    const text = (name: string) => {
      const v = (data.get(name) as string | null)?.trim();
      return v ? v : undefined;
    };
    const num = (name: string) => {
      const v = data.get(name) as string;
      return v ? Number(v) : undefined;
    };
    setDetails({
      firstName: text("firstName") ?? "",
      lastName: text("lastName") ?? "",
      email: text("email") ?? "",
      phone: text("phone") ?? "",
      dateOfBirth: text("dateOfBirth"),
      gender: text("gender"),
      heightCm: num("heightCm"),
      weightKg: num("weightKg"),
      bloodGroup: text("bloodGroup"),
      medicalNotes: text("medicalNotes"),
      emergencyContactName: text("emergencyContactName"),
      emergencyContactPhone: text("emergencyContactPhone"),
      emergencyContactRelationship: text("emergencyContactRelationship"),
    });
    setStep("plan");
  };

  const handlePlanConfirm = async () => {
    if (!details || !signature || !selectedPlanId) return;
    setRegistering(true);
    setRegisterError(null);
    try {
      const res = await registerPublicMember(gymId, {
        ...details,
        waiverAccepted: true,
        waiverSignature: signature,
        planId: selectedPlanId,
      });
      setResult(res);
      setStep("payment");
    } catch (err) {
      setRegisterError(err instanceof Error ? err.message : "Could not register. Please try again.");
    } finally {
      setRegistering(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f7f7f2] p-4">
        <p className="text-sm text-[#76766f]">Loading…</p>
      </div>
    );
  }

  if (loadError || !gym) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f7f7f2] p-4">
        <div className="w-full max-w-md border border-[#d8d8d1] bg-white p-6">
          <ErrorBanner message={loadError ?? "Could not load this gym."} />
        </div>
      </div>
    );
  }

  const pickerPlans: MembershipPlan[] = plans.map((p) => ({
    ...p,
    billingCycle: p.billingCycle as BillingCycle,
  }));

  return (
    <div className="flex min-h-screen items-start justify-center bg-[#f7f7f2] p-4 py-8">
      <div className="w-full max-w-xl border border-[#d8d8d1] bg-white">
        <div className="border-b border-[#e5e5de] p-5">
          <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#76766f]">
            {step === "done" ? "Registration complete" : `Join ${gym.name} · Step ${step === "details" ? "1" : step === "plan" ? "2" : "3"} of 3`}
          </p>
          <h1 className="mt-2 text-xl font-bold tracking-[-0.02em] text-[#24241f]">
            {step === "details"
              ? `Join ${gym.name}`
              : step === "plan"
                ? "Choose your plan"
                : step === "payment"
                  ? "Pay for your plan"
                  : `Welcome to ${gym.name}!`}
          </h1>
          {gym.city && <p className="mt-1 text-sm text-[#76766f]">{gym.city}</p>}
        </div>

        {step === "details" && (
          <form className="flex flex-col gap-6 p-5" onSubmit={handleDetailsSubmit}>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="First name" name="firstName" required />
              <Field label="Last name" name="lastName" required />
              <Field label="Email" name="email" type="email" required />
              <Field label="Phone" name="phone" type="tel" required />
              <Field label="Date of birth" name="dateOfBirth" type="date" />
              <label className="flex flex-col gap-1.5">
                <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Gender</span>
                <select
                  name="gender"
                  defaultValue=""
                  className="h-10 border border-[#d8d8d1] bg-white px-3 text-sm outline-none transition-colors focus:border-[#24241f]"
                >
                  <option value="">Prefer not to say</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>
              <Field label="Blood group" name="bloodGroup" />
              <Field label="Height (cm)" name="heightCm" type="number" />
              <Field label="Weight (kg)" name="weightKg" type="number" />
            </div>

            <p className="-mt-3 text-[11px] text-[#76766f]">
              This email becomes your login for the member dashboard — no password needed, just this email and a code.
            </p>

            <label className="flex flex-col gap-1.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Medical notes</span>
              <textarea
                name="medicalNotes"
                rows={2}
                className="border border-[#d8d8d1] bg-white p-3 text-sm outline-none transition-colors focus:border-[#24241f]"
              />
            </label>

            <div className="grid gap-3 sm:grid-cols-3">
              <Field label="Emergency contact" name="emergencyContactName" />
              <Field label="Contact phone" name="emergencyContactPhone" type="tel" />
              <Field label="Relationship" name="emergencyContactRelationship" />
            </div>

            <div>
              <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">
                Waiver — please read and sign
              </span>
              <div className="mt-2 max-h-32 overflow-y-auto rounded-xl border border-[#d8d8d1] bg-[#fafaf6] p-4 text-sm leading-relaxed text-[#4a4a44]">
                <ul className="list-disc space-y-1.5 pl-4">
                  {WAIVER_TEXT.map((line) => (
                    <li key={line}>{line}</li>
                  ))}
                </ul>
              </div>
              <div className="mt-3">
                <SignaturePad onSigned={setSignature} />
              </div>
              {signatureError && (
                <p className="mt-2 text-sm font-semibold text-red-600">Please sign the waiver to continue.</p>
              )}
            </div>

            <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
              <button
                type="submit"
                className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
              >
                Continue <ArrowRight className="size-3.5" />
              </button>
            </div>
          </form>
        )}

        {step === "plan" && (
          <div className="flex flex-col gap-5 p-5">
            {plans.length === 0 ? (
              <p className="rounded-xl border border-dashed border-[#d8d8d1] p-4 text-sm text-[#76766f]">
                {gym.name} doesn&apos;t have any active membership plans right now — please check with the gym and try again later.
              </p>
            ) : (
              <>
                <p className="text-sm text-[#5a5a54]">
                  Pick a plan. It stays awaiting payment until the gym confirms your payment.
                </p>
                <PlanPicker plans={pickerPlans} selectedId={selectedPlanId} onSelect={setSelectedPlanId} />
              </>
            )}
            {registerError && <ErrorBanner message={registerError} />}
            <div className="flex items-center justify-between border-t border-[#e5e5de] pt-5">
              <button
                type="button"
                onClick={() => setStep("details")}
                className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]"
              >
                Back
              </button>
              <button
                type="button"
                onClick={handlePlanConfirm}
                disabled={!selectedPlanId || registering}
                className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
              >
                {registering ? "Registering…" : "Register & continue"} <ArrowRight className="size-3.5" />
              </button>
            </div>
          </div>
        )}

        {step === "payment" && selectedPlan && (
          <div className="flex flex-col gap-5 p-5">
            <div className="rounded-2xl border border-[#d8d8d1] bg-[#fafaf6] p-4">
              <div className="flex items-baseline justify-between">
                <p className="text-sm font-bold text-[#24241f]">{selectedPlan.name}</p>
                <p className="text-base font-black text-[#24241f]">
                  {selectedPlan.currency} {selectedPlan.price}
                </p>
              </div>
              <p className="mt-1 text-xs text-[#76766f]">
                {selectedPlan.billingCycle.replace("_", " ").toLowerCase()} plan
                {gym.joiningFee != null
                  ? ` · Joining fee of ${gym.joiningFeeCurrency ?? ""} ${gym.joiningFee} is payable at the gym.`
                  : ""}
              </p>
            </div>

            {upiUri ? (
              <>
                <p className="text-sm text-[#5a5a54]">
                  Tap the button below — your UPI app will open with {selectedPlan.currency} {selectedPlan.price} filled in for {gym.name}. Pay, then come back here and tap “I&apos;ve paid”.
                </p>
                <button
                  type="button"
                  onClick={() => {
                    window.location.href = upiUri;
                  }}
                  className="flex h-12 items-center justify-center gap-2 bg-[#c7f36a] px-4 text-sm font-bold text-[#25251f] transition hover:bg-[#d8ff8a]"
                >
                  Pay {selectedPlan.currency} {selectedPlan.price} via UPI
                </button>
                {upiQr && (
                  <div className="flex flex-col items-center gap-2 border border-[#d8d8d1] bg-white p-5">
                    {/* eslint-disable-next-line @next/next/no-img-element -- generated data URL, not a static asset */}
                    <img src={upiQr} alt={`UPI payment QR code for ${selectedPlan.currency} ${selectedPlan.price}`} className="size-56" />
                    <p className="text-center text-xs text-[#76766f]">
                      Or scan with any UPI app to pay {selectedPlan.currency} {selectedPlan.price} to {gym.upiId}
                    </p>
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => setStep("done")}
                  className="flex h-11 items-center justify-center gap-2 border border-[#24241f] bg-[#24241f] px-4 text-sm font-bold text-white transition hover:bg-[#42423a]"
                >
                  <CheckCircle2 className="size-4" /> I&apos;ve paid
                </button>
              </>
            ) : (
              <>
                <p className="rounded-xl border border-dashed border-[#d8d8d1] p-4 text-sm text-[#76766f]">
                  This gym hasn&apos;t set up UPI payments yet — pay {selectedPlan.currency} {selectedPlan.price} at the gym and show them this registration.
                </p>
                <button
                  type="button"
                  onClick={() => setStep("done")}
                  className="flex h-11 items-center justify-center gap-2 border border-[#24241f] bg-[#24241f] px-4 text-sm font-bold text-white transition hover:bg-[#42423a]"
                >
                  Continue
                </button>
              </>
            )}
          </div>
        )}

        {step === "done" && selectedPlan && (
          <div className="flex flex-col gap-5 p-5">
            <div className="flex flex-col items-center py-4 text-center">
              <span className="grid size-14 place-items-center rounded-full bg-[#c7f36a]/40 text-[#5a7a1a]">
                <CheckCircle2 className="size-7" />
              </span>
              <h2 className="mt-4 text-lg font-bold text-[#24241f]">Registration received</h2>
              <p className="mt-2 max-w-sm text-sm leading-relaxed text-[#5a5a54]">
                {details?.firstName}, you&apos;re registered for the {selectedPlan.name} plan at {gym.name}. Show this
                screen at the gym — they&apos;ll confirm your payment and activate your membership.
              </p>
            </div>
            <div className="rounded-2xl border border-[#d8d8d1] bg-[#fafaf6] p-4 text-sm">
              <div className="flex justify-between py-1">
                <span className="text-[#76766f]">Plan</span>
                <span className="font-bold text-[#24241f]">{selectedPlan.name}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-[#76766f]">Amount</span>
                <span className="font-bold text-[#24241f]">
                  {selectedPlan.currency} {selectedPlan.price}
                </span>
              </div>
              {result && (
                <div className="flex justify-between py-1">
                  <span className="text-[#76766f]">Status</span>
                  <span className="font-bold text-[#24241f]">Awaiting payment confirmation</span>
                </div>
              )}
            </div>
            <p className="text-center text-xs text-[#76766f]">
              Your email ({details?.email}) is your login for the member dashboard — sign in with a code once your membership is active.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default function PublicRegisterPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-[#f7f7f2] p-4">
          <p className="text-sm text-[#76766f]">Loading…</p>
        </div>
      }
    >
      <RegisterForm />
    </Suspense>
  );
}
