import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { SettingsPage } from "@/components/orion/workspace-pages";
export const Route = createFileRoute("/settings")({
  head: () => ({
    meta: [
      { title: "Settings — Orion Intelligence" },
      { name: "description", content: "Manage your Orion Intelligence workspace settings." },
      { property: "og:title", content: "Settings — Orion Intelligence" },
      { property: "og:description", content: "Manage your Orion Intelligence workspace settings." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <SettingsPage />
    </RequireAuth>
  ),
});
