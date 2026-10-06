import { createFileRoute } from "@tanstack/react-router";
import { RequireAuth } from "@/lib/require-auth";
import { BoardsPage } from "@/components/orion/workspace-pages";
export const Route = createFileRoute("/boards/")({
  head: () => ({
    meta: [
      { title: "Investigation Boards — Orion Intelligence" },
      { name: "description", content: "Collaborative evidence and investigation workspaces." },
      { property: "og:title", content: "Investigation Boards — Orion Intelligence" },
      {
        property: "og:description",
        content: "Collaborative evidence and investigation workspaces.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => (
    <RequireAuth>
      <BoardsPage />
    </RequireAuth>
  ),
});
