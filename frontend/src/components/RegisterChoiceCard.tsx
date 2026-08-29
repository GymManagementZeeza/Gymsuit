"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ArrowRight, Dumbbell, UserRound } from "lucide-react";
import AuthShell from "@/components/AuthShell";

export default function RegisterChoiceCard() {
  const searchParams = useSearchParams();
  const email = searchParams.get("email") ?? "";
  const suffix = email ? `?email=${encodeURIComponent(email)}` : "";

  return (
    <AuthShell
      eyebrow="Create your account"
      headline="You're new here."
      description={
        email
          ? `No account found for ${email}. Tell us who you are to get set up.`
          : "Tell us who you are to get set up."
      }
    >
      <div className="mt-9 flex flex-col gap-4">
        <Link
          href={`/register/owner${suffix}`}
          className="group flex items-center justify-between gap-4 rounded-2xl border border-ink/15 bg-white px-5 py-5 shadow-sm transition-colors hover:border-saffron"
        >
          <span className="flex items-center gap-4">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-saffron/10 text-saffron">
              <Dumbbell className="h-5 w-5" />
            </span>
            <span>
              <span className="block font-display text-base font-bold tracking-[-0.02em] text-ink">
                I run a gym
              </span>
              <span className="block text-sm text-ink/55">
                Set up your gym and start managing members, staff and payments.
              </span>
            </span>
          </span>
          <ArrowRight className="h-4 w-4 shrink-0 text-ink/30 transition-transform group-hover:translate-x-0.5 group-hover:text-saffron" />
        </Link>

        <Link
          href={`/register/member${suffix}`}
          className="group flex items-center justify-between gap-4 rounded-2xl border border-ink/15 bg-white px-5 py-5 shadow-sm transition-colors hover:border-saffron"
        >
          <span className="flex items-center gap-4">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-sage/10 text-sage">
              <UserRound className="h-5 w-5" />
            </span>
            <span>
              <span className="block font-display text-base font-bold tracking-[-0.02em] text-ink">
                I&apos;m a member
              </span>
              <span className="block text-sm text-ink/55">
                Join your gym to check in, book classes and track payments.
              </span>
            </span>
          </span>
          <ArrowRight className="h-4 w-4 shrink-0 text-ink/30 transition-transform group-hover:translate-x-0.5 group-hover:text-saffron" />
        </Link>
      </div>

      <div className="my-8 h-px w-full bg-ink/10" />

      <p className="text-sm leading-6 text-ink/60">
        Already have an account?{" "}
        <Link href="/login" className="font-semibold text-ink hover:text-saffron">
          Log in
        </Link>
      </p>
    </AuthShell>
  );
}
