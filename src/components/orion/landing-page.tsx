import { Link } from "@tanstack/react-router";
import {
  ArrowRight,
  Bot,
  Boxes,
  DatabaseZap,
  GitBranch,
  Layers,
  Lock,
  Network,
  Search,
  ShieldCheck,
  Sparkles,
  Workflow,
} from "lucide-react";
import { Button } from "@/components/ui/button";

const features = [
  {
    icon: Network,
    title: "Knowledge Graph",
    desc: "Unified object-and-link model across every dataset, document, and department.",
  },
  {
    icon: Sparkles,
    title: "AI Reasoning",
    desc: "Multi-agent orchestration that searches the graph, cites evidence, and answers complex questions.",
  },
  {
    icon: Layers,
    title: "Ontology Layer",
    desc: "Define your business vocabulary once — Person, Company, Transaction, Alert — and reuse everywhere.",
  },
  {
    icon: Boxes,
    title: "Investigation Boards",
    desc: "Collaborative, real-time canvases where analysts pin evidence and connect the dots.",
  },
  {
    icon: Workflow,
    title: "No-Code Apps",
    desc: "Ship operational tools on top of the live graph in minutes, not quarters.",
  },
  {
    icon: DatabaseZap,
    title: "Hybrid Search",
    desc: "Keyword + vector + graph traversal, fused into one query that always finds the signal.",
  },
];

const stats = [
  { value: "Unified", label: "Data across silos" },
  { value: "Sub-second", label: "Graph traversals" },
  { value: "SOC 2", label: "Enterprise-ready" },
  { value: "No backend", label: "For app builders" },
];

export function LandingPage() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-40 border-b border-border/60 bg-background/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="flex size-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <span className="font-display text-sm font-bold">O</span>
            </div>
            <div className="leading-tight">
              <p className="font-display text-sm font-semibold">Orion Intelligence</p>
              <p className="text-[10px] text-muted-foreground">
                by Cognara · Enterprise Decision OS
              </p>
            </div>
          </Link>
          <nav className="hidden items-center gap-8 text-sm text-muted-foreground md:flex">
            <a href="#features" className="transition-colors hover:text-foreground">
              Platform
            </a>
            <a href="#how" className="transition-colors hover:text-foreground">
              How it works
            </a>
            <a href="#security" className="transition-colors hover:text-foreground">
              Security
            </a>
            <a href="#pricing" className="transition-colors hover:text-foreground">
              Pricing
            </a>
          </nav>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" asChild>
              <Link to="/login">Sign in</Link>
            </Button>
            <Button size="sm" asChild>
              <Link to="/register">
                Start free
                <ArrowRight />
              </Link>
            </Button>
          </div>
        </div>
      </header>

      <main>
        <section className="relative overflow-hidden">
          <div className="absolute inset-0 bg-auth-grid opacity-40 [mask-image:radial-gradient(ellipse_at_center,black_40%,transparent_75%)]" />
          <div className="pointer-events-none absolute left-1/2 top-0 -z-0 -translate-x-1/2 overflow-hidden">
            <div className="h-80 w-[1000px] rounded-full bg-primary/20 blur-3xl" />
          </div>
          <div className="relative mx-auto max-w-7xl px-6 pb-28 pt-24 text-center">
            <div className="mx-auto inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3.5 py-1 text-[11px] font-medium text-primary">
              <Sparkles className="size-3" />
              Built for intelligence teams, analysts, and operators.
            </div>
            <h1 className="mx-auto mt-6 max-w-4xl font-display text-5xl font-semibold leading-[1.05] tracking-tight sm:text-6xl">
              Turn fragmented data into
              <span className="bg-gradient-to-br from-primary via-primary to-indigo-300 bg-clip-text text-transparent">
                {" "}
                operational decisions.
              </span>
            </h1>
            <p className="mx-auto mt-6 max-w-2xl text-base leading-8 text-muted-foreground sm:text-lg">
              Orion is a governed knowledge layer for complex investigations, trusted AI, and
              mission-critical applications. Connect every source, ask the hard questions, and ship
              tools that run on live data — without writing backend code.
            </p>
            <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
              <Button size="lg" asChild>
                <Link to="/register">
                  Create your workspace
                  <ArrowRight />
                </Link>
              </Button>
              <Button size="lg" variant="outline" asChild>
                <Link to="/login">Sign in to workspace</Link>
              </Button>
            </div>
            <p className="mt-5 text-xs text-muted-foreground">
              Free for up to 5 users · No credit card required · Cancel anytime
            </p>

            <div className="mt-20 grid border-t border-l border-border sm:grid-cols-2 xl:grid-cols-4">
              {stats.map((s, i) => (
                <div key={i} className="border-b border-r border-border px-6 py-7 text-left">
                  <p className="font-display text-2xl font-semibold text-foreground">{s.value}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{s.label}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="features" className="mx-auto max-w-7xl px-6 py-24">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-[11px] font-semibold uppercase tracking-[.2em] text-primary">
              Platform
            </p>
            <h2 className="mt-3 font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              Everything an intelligence team needs.
            </h2>
            <p className="mt-4 text-sm leading-7 text-muted-foreground">
              One platform replaces your search engine, graph database, BI tool, collaboration tool,
              and internal app framework.
            </p>
          </div>

          <div className="mt-16 grid gap-px rounded-xl border border-border bg-border sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="group bg-card p-6 transition-colors hover:bg-accent/40">
                <div className="flex size-10 items-center justify-center rounded-lg border border-border bg-background group-hover:border-primary/40 group-hover:bg-primary/10">
                  <f.icon className="size-4 text-primary" />
                </div>
                <h3 className="mt-4 font-display text-base font-semibold">{f.title}</h3>
                <p className="mt-1.5 text-sm leading-6 text-muted-foreground">{f.desc}</p>
              </div>
            ))}
          </div>
        </section>

        <section id="how" className="border-t border-border bg-muted/20">
          <div className="mx-auto max-w-7xl px-6 py-24">
            <div className="mx-auto max-w-2xl text-center">
              <p className="text-[11px] font-semibold uppercase tracking-[.2em] text-primary">
                How it works
              </p>
              <h2 className="mt-3 font-display text-3xl font-semibold tracking-tight sm:text-4xl">
                From raw data to decisions in four steps.
              </h2>
            </div>

            <div className="mt-16 grid gap-10 lg:grid-cols-4">
              {[
                {
                  step: "01",
                  icon: DatabaseZap,
                  title: "Connect any source",
                  body: "Drop files, plug in databases, or stream from Kafka. Orion normalizes, cleans, and lands everything into a raw zone.",
                },
                {
                  step: "02",
                  icon: GitBranch,
                  title: "Build the graph",
                  body: "Entity resolution merges duplicates automatically. Objects and links are written into a knowledge graph with full lineage.",
                },
                {
                  step: "03",
                  icon: Bot,
                  title: "Ask anything",
                  body: "Natural-language questions become graph queries. Multi-agent reasoning returns answers with sources you can trust.",
                },
                {
                  step: "04",
                  icon: Layers,
                  title: "Ship apps & boards",
                  body: "Drag-and-drop your ontology onto a canvas. Publish tools that run directly against the graph for daily ops.",
                },
              ].map((s) => (
                <div key={s.step} className="relative">
                  <div className="flex items-center gap-3">
                    <p className="font-display text-xs font-semibold text-primary">{s.step}</p>
                    <div className="h-px flex-1 bg-border" />
                  </div>
                  <div className="mt-4 flex size-11 items-center justify-center rounded-lg border border-border bg-background">
                    <s.icon className="size-4 text-primary" />
                  </div>
                  <h3 className="mt-5 font-display text-lg font-semibold">{s.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">{s.body}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="security" className="mx-auto max-w-7xl px-6 py-24">
          <div className="grid gap-10 rounded-2xl border border-border p-10 lg:grid-cols-[1fr_1.1fr] lg:items-center">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-[11px] font-medium text-primary">
                <ShieldCheck className="size-3" />
                Trusted by regulated teams
              </div>
              <h2 className="mt-5 font-display text-3xl font-semibold tracking-tight">
                Security and governance, built in from day one.
              </h2>
              <p className="mt-4 text-sm leading-7 text-muted-foreground">
                Orion is designed for multi-tenant isolation, row-level security, and auditability.
                Every object carries its lineage — you can always see exactly where a piece of
                information came from.
              </p>
              <div className="mt-7 grid grid-cols-2 gap-4 text-sm">
                <div className="flex items-start gap-2.5">
                  <Lock className="mt-0.5 size-4 text-primary" />
                  <div>
                    <p className="font-medium">RBAC + ABAC</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      Role & attribute-based access.
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-2.5">
                  <GitBranch className="mt-0.5 size-4 text-primary" />
                  <div>
                    <p className="font-medium">Full lineage</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      Source → transform → object.
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-2.5">
                  <Search className="mt-0.5 size-4 text-primary" />
                  <div>
                    <p className="font-medium">Immutable audit</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">Every action is logged.</p>
                  </div>
                </div>
                <div className="flex items-start gap-2.5">
                  <ShieldCheck className="mt-0.5 size-4 text-primary" />
                  <div>
                    <p className="font-medium">E2E controls</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      Encryption at rest + in transit.
                    </p>
                  </div>
                </div>
              </div>
            </div>
            <div className="rounded-xl border border-border bg-muted/40 p-5 font-mono text-[11px] leading-6 text-muted-foreground">
              <p className="text-foreground/70">// object provenance, first-class</p>
              <p>{`const node = await graph.get("OBJ-4921");`}</p>
              <p>{`node.lineage // { `}</p>
              <p>{`  dataset: "Q4 Vendor Ledger",`}</p>
              <p>{`  pipeline: "ERP — SAP S/4HANA",`}</p>
              <p>{`  ingested_by: "maya@cognara.ai",`}</p>
              <p>{`  ingested_at: "2026-09-19T08:14:22Z",`}</p>
              <p>{`  hash: "sha256:4a9f…e1a2"`}</p>
              <p>{`}`}</p>
            </div>
          </div>
        </section>

        <section id="pricing" className="border-t border-border">
          <div className="mx-auto max-w-7xl px-6 py-24 text-center">
            <p className="text-[11px] font-semibold uppercase tracking-[.2em] text-primary">
              Pricing
            </p>
            <h2 className="mt-3 font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              Start small, scale to the enterprise.
            </h2>
            <div className="mx-auto mt-14 grid max-w-4xl gap-6 lg:grid-cols-3">
              {[
                {
                  name: "Team",
                  price: "Free",
                  cta: "Start building",
                  popular: false,
                  bullets: [
                    "Up to 5 users",
                    "1M objects",
                    "Standard ontology",
                    "Community support",
                  ],
                },
                {
                  name: "Business",
                  price: "$49/user",
                  cta: "Start trial",
                  popular: true,
                  bullets: [
                    "Unlimited users",
                    "100M objects",
                    "SSO + audit logs",
                    "AI reasoning credits",
                    "Priority support",
                  ],
                },
                {
                  name: "Enterprise",
                  price: "Custom",
                  cta: "Talk to sales",
                  popular: false,
                  bullets: [
                    "Dedicated tenancy",
                    "On-prem option",
                    "Custom SLAs",
                    "Professional services",
                  ],
                },
              ].map((p) => (
                <div
                  key={p.name}
                  className={`relative flex flex-col rounded-2xl border p-7 text-left ${p.popular ? "border-primary/40 shadow-[0_0_0_1px_rgba(99,102,241,0.2)]" : "border-border"}`}
                >
                  {p.popular && (
                    <div className="absolute -top-2.5 left-1/2 -translate-x-1/2 rounded-full bg-primary px-3 py-0.5 text-[10px] font-semibold text-primary-foreground">
                      Most popular
                    </div>
                  )}
                  <p className="font-display text-sm font-semibold text-muted-foreground">
                    {p.name}
                  </p>
                  <p className="mt-2 font-display text-4xl font-semibold tracking-tight">
                    {p.price}
                  </p>
                  <ul className="mt-6 space-y-2.5 text-sm">
                    {p.bullets.map((b) => (
                      <li key={b} className="flex items-start gap-2">
                        <span className="mt-1 size-1.5 rounded-full bg-primary" />
                        <span className="text-sm">{b}</span>
                      </li>
                    ))}
                  </ul>
                  <Button className="mt-8" variant={p.popular ? "default" : "outline"} asChild>
                    <Link to="/register">{p.cta}</Link>
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="border-t border-border">
          <div className="mx-auto max-w-4xl px-6 py-24 text-center">
            <h2 className="font-display text-4xl font-semibold tracking-tight">
              Your data. Your ontology. Your OS.
            </h2>
            <p className="mx-auto mt-4 max-w-xl text-sm leading-7 text-muted-foreground">
              Stop stitching together BI tools, spreadsheets, and dashboards. Build once on Orion,
              and every future question gets cheaper.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <Button size="lg" asChild>
                <Link to="/register">
                  Create your workspace
                  <ArrowRight />
                </Link>
              </Button>
              <Button variant="outline" size="lg" asChild>
                <Link to="/login">Sign in</Link>
              </Button>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-10 text-xs text-muted-foreground md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex size-7 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <span className="font-display text-[11px] font-bold">O</span>
            </div>
            <span className="font-display text-sm font-semibold text-foreground">
              Orion Intelligence
            </span>
            <span>· © {new Date().getFullYear()} Cognara, Inc.</span>
          </div>
          <div className="flex flex-wrap gap-5">
            <a href="#" className="hover:text-foreground">
              Privacy
            </a>
            <a href="#" className="hover:text-foreground">
              Terms
            </a>
            <a href="#" className="hover:text-foreground">
              Security
            </a>
            <a href="#" className="hover:text-foreground">
              Docs
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
