import { createFileRoute, Outlet } from "@tanstack/react-router";
import { useEffect } from "react";
import { DashboardPage } from "@/components/orion/dashboard-page";
import { LandingPage } from "@/components/orion/landing-page";
import { useAuth } from "@/lib/auth-store";
import { Spinner } from "@/components/ui/spinner";

function IndexSwitch() {
  const { initialize, status, initialized } = useAuth();

  useEffect(() => {
    void initialize();
  }, [initialize]);

  if (!initialized || status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <div className="flex size-11 items-center justify-center rounded-lg bg-primary">
            <span className="font-display text-sm font-bold text-primary-foreground">O</span>
          </div>
          <Spinner size="sm" />
          <p className="text-xs text-muted-foreground">Starting Orion…</p>
        </div>
      </div>
    );
  }

  const isAuthed = status === "authenticated";
  return isAuthed ? <DashboardPage /> : <LandingPage />;
}

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Orion Intelligence — Enterprise Decision OS" },
      {
        name: "description",
        content:
          "A governed knowledge layer for complex investigations, trusted AI, and mission-critical applications.",
      },
      { property: "og:title", content: "Orion Intelligence — Enterprise Decision OS" },
      {
        property: "og:description",
        content:
          "A governed knowledge layer for complex investigations, trusted AI, and mission-critical applications.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: IndexSwitch,
});
