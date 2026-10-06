import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { AssistantPage } from "@/components/orion/assistant-page";
export const Route = createFileRoute("/assistant/$threadId")({
  head: () => ({
    meta: [
      { title: "AI Assistant — Orion Intelligence" },
      { name: "description", content: "Evidence-grounded enterprise intelligence assistant." },
      { property: "og:title", content: "AI Assistant — Orion Intelligence" },
      {
        property: "og:description",
        content: "Evidence-grounded enterprise intelligence assistant.",
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
  const { threadId } = Route.useParams();
  return <AssistantPage threadId={threadId} />;
}
