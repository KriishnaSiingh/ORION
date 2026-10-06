import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { ObjectDetailPage } from "@/components/orion/knowledge-pages";
export const Route = createFileRoute("/knowledge/$objectId")({
  head: () => ({
    meta: [
      { title: "Object Detail — Orion Intelligence" },
      { name: "description", content: "Inspect object properties, links, lineage, and history." },
      { property: "og:title", content: "Object Detail — Orion Intelligence" },
      {
        property: "og:description",
        content: "Inspect object properties, links, lineage, and history.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <Page />
    </RequireAuth>
  ),
});
function Page() {
  const { objectId } = Route.useParams();
  return <ObjectDetailPage objectId={objectId} />;
}
