import { signIn } from "@/auth";

export default async function SignIn({
  searchParams,
}: {
  searchParams: Promise<{ callbackUrl?: string; error?: string }>;
}) {
  const { callbackUrl, error } = await searchParams;
  // Only allow same-site redirects after sign-in.
  const redirectTo = callbackUrl?.startsWith("/") && !callbackUrl.startsWith("//") ? callbackUrl : "/";

  return (
    <main className="grid min-h-dvh place-items-center px-4">
      <div className="w-full max-w-sm rounded-xl border border-[var(--border)] bg-[var(--panel)] p-6 text-center">
        <h1 className="mb-1 text-xl font-semibold">Private LLM</h1>
        <p className="mb-6 text-sm text-[var(--muted)]">Models run on your own machine.</p>
        {error && (
          <p className="mb-4 text-sm text-red-600">
            {error === "AccessDenied" ? "This Google account isn't on the allowed list." : "Sign-in failed. Try again."}
          </p>
        )}
        <form
          action={async () => {
            "use server";
            await signIn("google", { redirectTo });
          }}
        >
          <button className="w-full rounded-lg bg-[var(--accent)] px-4 py-2 font-medium text-white">
            Sign in with Google
          </button>
        </form>
      </div>
    </main>
  );
}
