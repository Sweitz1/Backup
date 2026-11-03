import { auth, signOut } from "@/auth";
import { redirect } from "next/navigation";
import Chat from "@/components/Chat";

export default async function Home() {
  const session = await auth();
  if (!session?.user) redirect("/signin");

  return (
    <main className="mx-auto flex h-dvh max-w-3xl flex-col px-4">
      <header className="flex items-center justify-between border-b border-[var(--border)] py-3">
        <h1 className="font-semibold">Private LLM</h1>
        <form
          action={async () => {
            "use server";
            await signOut({ redirectTo: "/signin" });
          }}
        >
          <span className="mr-3 text-sm text-[var(--muted)]">{session.user.email}</span>
          <button className="text-sm underline">Sign out</button>
        </form>
      </header>
      <Chat />
    </main>
  );
}
