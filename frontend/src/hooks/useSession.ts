"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSession, type LoginResponse } from "@/lib/auth";

function readSession(): LoginResponse | null {
  if (typeof window === "undefined") return null;
  return getSession();
}

/** Client-side session guard: redirects to /login if there's no saved session. */
export function useSession(): LoginResponse | null {
  const router = useRouter();
  // Start as null on both server and client to avoid a hydration mismatch
  // (readSession() can only see localStorage once mounted in the browser).
  const [session, setSession] = useState<LoginResponse | null>(null);
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    setSession(readSession());
    setChecked(true);
  }, []);

  useEffect(() => {
    if (checked && !session) {
      router.replace("/login");
    }
  }, [checked, session, router]);

  return session;
}
