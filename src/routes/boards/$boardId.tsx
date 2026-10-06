import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { BoardCanvasPage } from "@/components/orion/workspace-pages";
export const Route = createFileRoute("/boards/$boardId")({
  head: () => ({
    meta: [
      { title: "Investigation Canvas — Orion Intelligence" },
      { name: "description", content: "Analyze and connect evidence on an investigation canvas." },
      { property: "og:title", content: "Investigation Canvas — Orion Intelligence" },
      {
        property: "og:description",
        content: "Analyze and connect evidence on an investigation canvas.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <BoardCanvasPage />
    </RequireAuth>
  ),
});
