import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { KnowledgePage } from "@/components/orion/knowledge-pages";
export const Route = createFileRoute("/knowledge/")({
  head: () => ({
    meta: [
      { title: "Knowledge Explorer — Orion Intelligence" },
      { name: "description", content: "Search governed objects and relationships." },
      { property: "og:title", content: "Knowledge Explorer — Orion Intelligence" },
      { property: "og:description", content: "Search governed objects and relationships." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <KnowledgePage />
    </RequireAuth>
  ),
});
