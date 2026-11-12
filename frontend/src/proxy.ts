import { NextResponse, type NextRequest } from "next/server";

/**
 * diet.gymsuit.app is the standalone nutrition expert site, served by this same app.
 * Its home page is the nutrition sign-in / sign-up page instead of the gym marketing page.
 * Works locally too via http://diet.localhost:3000.
 */
export function proxy(request: NextRequest) {
  const host = (request.headers.get("x-forwarded-host") ?? request.headers.get("host") ?? "").toLowerCase();
  if (host.startsWith("diet.")) {
    return NextResponse.rewrite(new URL("/diet", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: "/",
};
