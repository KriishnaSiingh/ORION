import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { AppBuilderPage } from "@/components/orion/workspace-pages";
export const Route = createFileRoute("/applications/$appId")({
  head: () => ({
    meta: [
      { title: "Application Builder — Orion Intelligence" },
      { name: "description", content: "Compose operational workflows and application views." },
      { property: "og:title", content: "Application Builder — Orion Intelligence" },
      {
        property: "og:description",
        content: "Compose operational workflows and application views.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: function AppBuilderRoute() {
    const { appId } = Route.useParams();
    return (
      <RequireAuth>
        <AppBuilderPage appId={appId} />
      </RequireAuth>
    );
  },
});
