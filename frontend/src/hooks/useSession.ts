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
  const [session] = useState<LoginResponse | null>(readSession);

  useEffect(() => {
    if (!session) {
      router.replace("/login");
    }
  }, [session, router]);

  return session;
}
