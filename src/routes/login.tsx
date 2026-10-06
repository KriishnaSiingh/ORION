import { createFileRoute } from "@tanstack/react-router";
import { AuthPage } from "@/components/orion/auth-page";
export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Sign in — Orion Intelligence" },
      { name: "description", content: "Secure sign in for Orion Intelligence." },
      { property: "og:title", content: "Sign in — Orion Intelligence" },
      { property: "og:description", content: "Secure sign in for Orion Intelligence." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: () => <AuthPage mode="login" />,
});
