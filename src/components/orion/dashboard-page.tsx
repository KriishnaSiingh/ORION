import { Link } from "@tanstack/react-router";
import { useMemo } from "react";
import {
  Activity,
  ArrowRight,
  Boxes,
  Database,
  GitBranch,
  Plus,
  Search,
  Workflow,
} from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Button } from "@/components/ui/button";
import { PageHeader, MetricCard, Section } from "./primitives";
import { chartData } from "@/lib/orion-data";
import { useAuth } from "@/lib/auth-store";
import { useOrionStore } from "@/lib/orion-store";

function greeting() {
  const h = new Date().getHours();
  if (h < 5) return "Good evening";
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

export function DashboardPage() {
  const { profile: authProfile } = useAuth();
  const storeProfile = useOrionStore((s) => s.profile);
  const entities = useOrionStore((s) => s.entities);
  const pipelines = useOrionStore((s) => s.pipelines);
  const boards = useOrionStore((s) => s.boards);
  const activities = useOrionStore((s) => s.activities);

  const hello = useMemo(() => {
    const base = greeting();
    const name = storeProfile?.firstName || authProfile?.displayName?.split(/\s+/)[0] || "there";
    return `${base}, ${name}`;
  }, [storeProfile?.firstName, authProfile?.displayName]);

  const activePipes = pipelines.filter(
    (p) => p.status === "Operational" || p.status === "Active",
  ).length;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Overview"
        title={hello}
        description={`Your operational knowledge layer is active with ${entities.length} governed objects across ${pipelines.length} pipelines.`}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/knowledge">
                <Search />
                Explore knowledge
              </Link>
            </Button>
            <Button asChild>
              <Link to="/boards">
                <Plus />
                New investigation
              </Link>
            </Button>
          </>
        }
      />
      <div className="grid border-l border-t border-border sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Total objects"
          value={`${entities.length} active`}
          change="+3 this week"
          icon={Database}
        />
        <MetricCard
          label="Resolved links"
          value="24.7M"
          change="8.1%"
          icon={GitBranch}
        />
        <MetricCard
          label="Active pipelines"
          value={String(activePipes)}
          change="Streaming"
          icon={Workflow}
        />
        <MetricCard
          label="Investigations"
          value={String(boards.length)}
          change="Collaborative"
          icon={Boxes}
          trend="up"
        />
      </div>
      <div className="grid gap-6 xl:grid-cols-[1.55fr_.8fr]">
        <Section
          title="Knowledge activity"
          description="Objects created and searches across the last seven days"
          action={<span className="text-xs text-muted-foreground">Last 7 days</span>}
        >
          <div className="h-72 p-4">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="orionArea" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--primary)" stopOpacity={0.34} />
                    <stop offset="95%" stopColor="var(--primary)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="var(--border)" vertical={false} />
                <XAxis
                  dataKey="day"
                  tick={{ fill: "var(--muted-foreground)", fontSize: 10 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fill: "var(--muted-foreground)", fontSize: 10 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  contentStyle={{
                    background: "var(--popover)",
                    border: "1px solid var(--border)",
                    borderRadius: "6px",
                    fontSize: "12px",
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="objects"
                  stroke="var(--primary)"
                  strokeWidth={2}
                  fill="url(#orionArea)"
                />
                <Area
                  type="monotone"
                  dataKey="searches"
                  stroke="var(--info)"
                  strokeWidth={1.5}
                  fill="transparent"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Section>
        <Section title="Recent activity" description="Live workspace events">
          <div className="divide-y divide-border max-h-[320px] overflow-y-auto">
            {activities.map((a) => (
              <div className="flex gap-3 px-5 py-3.5 hover:bg-accent/40 transition-colors" key={a.id}>
                <span
                  className={`mt-1.5 size-2 shrink-0 rounded-full ${
                    a.tone === "success"
                      ? "bg-emerald-500"
                      : a.tone === "warning"
                        ? "bg-amber-500"
                        : "bg-sky-500"
                  }`}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium">{a.title}</p>
                  <p className="mt-1 truncate text-[11px] text-muted-foreground">{a.detail}</p>
                </div>
                <span className="text-[10px] text-muted-foreground">{a.time}</span>
              </div>
            ))}
          </div>
        </Section>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <Section
          title="Pipeline operations"
          description="Current ingestion health"
          action={
            <Button variant="ghost" size="sm" asChild>
              <Link to="/ingestion">
                View all
                <ArrowRight />
              </Link>
            </Button>
          }
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-border text-muted-foreground">
                <tr>
                  <th className="px-5 py-3 font-medium">Pipeline</th>
                  <th className="px-4 py-3 font-medium">Source</th>
                  <th className="px-4 py-3 font-medium">Records</th>
                  <th className="px-4 py-3 font-medium">Last run</th>
                </tr>
              </thead>
              <tbody>
                {pipelines.map((p) => (
                  <tr key={p.id || p.name} className="border-b border-border/70 last:border-0 hover:bg-accent/30">
                    <td className="px-5 py-3.5 font-medium">{p.name}</td>
                    <td className="px-4 py-3.5 text-muted-foreground">{p.source}</td>
                    <td className="px-4 py-3.5 tabular-nums font-semibold">{p.records}</td>
                    <td className="px-4 py-3.5 text-muted-foreground">{p.last}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Section>
        <Section title="Quick actions" description="Common workspace workflows">
          <div className="grid grid-cols-2 gap-px bg-border">
            {[
              { l: "Search graph", i: Search, to: "/knowledge" },
              { l: "New board", i: Boxes, to: "/boards" },
              { l: "Create pipeline", i: Workflow, to: "/ingestion" },
              { l: "Ask Orion", i: Activity, to: "/assistant/northstar-risk" },
            ].map((x) => (
              <Link
                to={x.to}
                key={x.l}
                className="group flex min-h-28 flex-col justify-between bg-card p-4 transition-colors hover:bg-accent"
              >
                <x.i className="size-4 text-muted-foreground group-hover:text-primary" />
                <div className="flex items-center justify-between text-xs font-medium">
                  {x.l}
                  <ArrowRight className="size-3 text-muted-foreground" />
                </div>
              </Link>
            ))}
          </div>
        </Section>
      </div>
    </div>
  );
}
