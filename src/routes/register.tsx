import { createFileRoute } from "@tanstack/react-router";
import { AuthPage } from "@/components/orion/auth-page";
export const Route = createFileRoute("/register")({
  head: () => ({
    meta: [
      { title: "Register — Orion Intelligence" },
      { name: "description", content: "Create an Orion Intelligence workspace account." },
      { property: "og:title", content: "Register — Orion Intelligence" },
      { property: "og:description", content: "Create an Orion Intelligence workspace account." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: () => <AuthPage mode="register" />,
});
