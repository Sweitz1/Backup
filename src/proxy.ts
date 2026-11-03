// Next.js 16 "proxy" (formerly middleware): every page and API route requires sign-in.
import { NextResponse } from "next/server";
import { auth } from "@/auth";

export default auth((req) => {
  if (req.auth) return NextResponse.next();

  if (req.nextUrl.pathname.startsWith("/api/")) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const url = new URL("/signin", req.nextUrl.origin);
  url.searchParams.set("callbackUrl", req.nextUrl.pathname);
  return NextResponse.redirect(url);
});

export const config = {
  // Everything except the sign-in page, Auth.js routes and static assets.
  matcher: ["/((?!signin|api/auth|_next/static|_next/image|favicon.ico).*)"],
};
