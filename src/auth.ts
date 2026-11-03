import NextAuth from "next-auth";
import Google from "next-auth/providers/google";

/** Google accounts allowed in. An empty list locks everyone out (fail closed). */
function allowedEmails(): string[] {
  return (process.env.ALLOWED_EMAILS ?? "")
    .split(",")
    .map((e) => e.trim().toLowerCase())
    .filter(Boolean);
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  // Reads AUTH_GOOGLE_ID / AUTH_GOOGLE_SECRET from the environment.
  providers: [Google],
  pages: { signIn: "/signin", error: "/signin" },
  session: { strategy: "jwt" }, // no database; nothing about the user is stored server-side
  callbacks: {
    signIn({ profile }) {
      const email = profile?.email?.toLowerCase();
      const verified = profile?.email_verified === true;
      return Boolean(email && verified && allowedEmails().includes(email));
    },
  },
});
