import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { ApplicationsPage } from "@/components/orion/workspace-pages";
export const Route = createFileRoute("/applications/")({
  head: () => ({
    meta: [
      { title: "Applications — Orion Intelligence" },
      {
        name: "description",
        content: "Create operational applications powered by governed knowledge.",
      },
      { property: "og:title", content: "Applications — Orion Intelligence" },
      {
        property: "og:description",
        content: "Create operational applications powered by governed knowledge.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <ApplicationsPage />
    </RequireAuth>
  ),
});
