"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { dashboardPathForRole, getSession } from "@/lib/auth";

/**
 * The PWA opens at "/" (manifest start_url). When a signed-in user reopens
 * the app, send them straight to their dashboard instead of showing the
 * marketing landing page. Session validity itself is enforced by the
 * dashboard's auth layer, which bounces truly expired sessions to /login.
 */
export default function HomeSessionRedirect() {
  const router = useRouter();

  useEffect(() => {
    const session = getSession();
    if (session) {
      router.replace(dashboardPathForRole(session.role));
    }
  }, [router]);

  return null;
}
