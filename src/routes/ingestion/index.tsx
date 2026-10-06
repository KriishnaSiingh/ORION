import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { IngestionPage } from "@/components/orion/workspace-pages";
export const Route = createFileRoute("/ingestion/")({
  head: () => ({
    meta: [
      { title: "Data Ingestion — Orion Intelligence" },
      { name: "description", content: "Configure and monitor enterprise data pipelines." },
      { property: "og:title", content: "Data Ingestion — Orion Intelligence" },
      { property: "og:description", content: "Configure and monitor enterprise data pipelines." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <IngestionPage />
    </RequireAuth>
  ),
});
