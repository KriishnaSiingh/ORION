import { createFileRoute, Navigate } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
export const Route = createFileRoute("/assistant/")({
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
      <Navigate to="/assistant/$threadId" params={{ threadId: "northstar-risk" }} replace />
    </RequireAuth>
  ),
});
