import { useState } from "react";
import type { LucideIcon } from "lucide-react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  AppWindow,
  Boxes,
  CalendarClock,
  Check,
  ChevronRight,
  CircleDot,
  Columns3,
  DatabaseZap,
  FileClock,
  FileText,
  FormInput,
  GitBranch,
  GripVertical,
  KeyRound,
  LayoutGrid,
  ListFilter,
  MessageSquare,
  MoreHorizontal,
  Network,
  PanelRight,
  Play,
  Plus,
  RotateCcw,
  Search,
  Send,
  Settings2,
  Shield,
  Table2,
  Users,
  Workflow,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { toast } from "sonner";
import { type Status } from "@/lib/orion-data";
import { PageHeader, Section, StatusBadge } from "./primitives";
import { KnowledgeGraph } from "./graphs";
import { useOrionStore } from "@/lib/orion-store";

// ============================================================================
// 1. INVESTIGATION BOARDS
// ============================================================================
export function BoardsPage() {
  const [q, setQ] = useState("");
  const [open, setOpen] = useState(false);
  const [boardName, setBoardName] = useState("");
  const [boardDesc, setBoardDesc] = useState("");
  const [boardStatus, setBoardStatus] = useState<Status>("Active");

  const boards = useOrionStore((s) => s.boards);
  const addBoard = useOrionStore((s) => s.addBoard);

  const filteredBoards = boards.filter((b) =>
    (b.name + " " + b.description).toLowerCase().includes(q.toLowerCase()),
  );

  const handleCreateBoard = async () => {
    if (!boardName.trim()) {
      toast.error("Board name is required");
      return;
    }

    try {
      await addBoard({
        name: boardName.trim(),
        description: boardDesc.trim() || "Collaborative investigation workspace",
        status: boardStatus,
      });

      toast.success(`Investigation board "${boardName.trim()}" created`);
      setBoardName("");
      setBoardDesc("");
      setOpen(false);
    } catch (e) {
      toast.error("Failed to create board");
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Investigations"
        title="Investigation Boards"
        description="Collaborative workspaces for hypotheses, evidence, and decisions."
        actions={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus />
                New board
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Create investigation board</DialogTitle>
                <DialogDescription>
                  Start a collaborative workspace for linking hypotheses and evidence.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-3">
                <div className="space-y-2">
                  <Label>Board title</Label>
                  <Input
                    placeholder="e.g. Operation Sovereign Shield"
                    value={boardName}
                    onChange={(e) => setBoardName(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Investigation focus</Label>
                  <Input
                    placeholder="Brief scope, target entities, or hypothesis"
                    value={boardDesc}
                    onChange={(e) => setBoardDesc(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Initial status</Label>
                  <div className="flex gap-2">
                    {(["Active", "Review", "Draft"] as Status[]).map((s) => (
                      <Button
                        key={s}
                        type="button"
                        size="sm"
                        variant={boardStatus === s ? "secondary" : "outline"}
                        onClick={() => setBoardStatus(s)}
                      >
                        {s}
                      </Button>
                    ))}
                  </div>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateBoard}>Create board</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />
      <div className="flex gap-2">
        <div className="relative max-w-sm flex-1">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <Input
            className="pl-9"
            placeholder="Search boards…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <Button variant="outline">
          <ListFilter />
          Filter ({filteredBoards.length})
        </Button>
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        {filteredBoards.map((b) => (
          <Link
            key={b.id || b.name}
            to="/boards/$boardId"
            params={{ boardId: b.id || "operation-northstar" }}
            className="group border border-border bg-card p-5 transition-colors hover:border-primary/50 hover:bg-accent/20"
          >
            <div className="flex justify-between">
              <span className="flex size-9 items-center justify-center rounded-md border border-border bg-muted">
                <Boxes className="size-4 text-primary" />
              </span>
              <StatusBadge status={b.status} />
            </div>
            <h2 className="mt-8 font-display text-base font-semibold">{b.name}</h2>
            <p className="mt-2 min-h-10 text-xs leading-5 text-muted-foreground">{b.description}</p>
            <div className="mt-6 flex items-center justify-between border-t border-border pt-4 text-[10px] text-muted-foreground">
              <span>
                {b.objects} objects · {b.collaborators} collaborators
              </span>
              <span>{b.updated}</span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

export function BoardCanvasPage() {
  const [commentInput, setCommentInput] = useState("");
  const boards = useOrionStore((s) => s.boards);
  const addCommentToBoard = useOrionStore((s) => s.addCommentToBoard);
  const shareBoard = useOrionStore((s) => s.shareBoard);

  const currentBoard = boards[0] || {
    id: "operation-northstar",
    name: "Operation Northstar",
    status: "Active" as Status,
    collaborators: 7,
    comments: [],
  };

  const handleSendComment = () => {
    if (!commentInput.trim()) return;
    addCommentToBoard(currentBoard.id, { text: commentInput.trim() });
    toast.success("Analyst comment added to board");
    setCommentInput("");
  };

  const handleShare = () => {
    shareBoard(currentBoard.id, "team@orion.ai");
    toast.success("Board shared with team workspace");
  };

  return (
    <div className="-m-5 lg:-m-7">
      <div className="flex h-14 items-center gap-3 border-b border-border px-4">
        <Link to="/boards" className="text-xs text-muted-foreground">
          Boards
        </Link>
        <ChevronRight className="size-3 text-muted-foreground" />
        <span className="text-xs font-medium">{currentBoard.name}</span>
        <StatusBadge status={currentBoard.status} />
        <div className="ml-auto flex items-center gap-2">
          <div className="flex -space-x-2">
            {["MC", "AR", "JT", "KS"].slice(0, Math.min(currentBoard.collaborators, 4)).map((x) => (
              <span
                key={x}
                className="flex size-7 items-center justify-center rounded-full border-2 border-background bg-muted text-[9px] font-medium"
              >
                {x}
              </span>
            ))}
          </div>
          <Button variant="outline" size="sm" onClick={() => toast.info(`${currentBoard.comments?.length || 0} comments recorded`)}>
            <MessageSquare />
            Comments ({currentBoard.comments?.length || 0})
          </Button>
          <Button size="sm" onClick={handleShare}>Share</Button>
        </div>
      </div>
      <div className="grid h-[calc(100vh-112px)] grid-cols-[56px_1fr_290px]">
        <aside className="flex flex-col items-center gap-2 border-r border-border p-2">
          {[Search, Boxes, FileText, GitBranch, MessageSquare].map((I, i) => (
            <Button
              key={i}
              size="icon"
              variant={i === 1 ? "secondary" : "ghost"}
              aria-label="Board tool"
              onClick={() => toast.info("Canvas tool active")}
            >
              <I />
            </Button>
          ))}
        </aside>
        <KnowledgeGraph editable />
        <aside className="hidden flex-col border-l border-border bg-card p-4 xl:flex">
          <p className="text-xs font-semibold">Board Activity & Comments</p>
          <div className="mt-4 flex-1 space-y-4 overflow-y-auto">
            {(currentBoard.comments && currentBoard.comments.length > 0) ? (
              currentBoard.comments.map((c, i) => (
                <div className="flex gap-2 text-xs" key={i}>
                  <span className="mt-1 size-1.5 shrink-0 rounded-full bg-primary" />
                  <div>
                    <p className="font-medium text-[11px] text-foreground">{c.author}</p>
                    <p className="text-muted-foreground">{c.text}</p>
                    <p className="mt-0.5 text-[10px] text-muted-foreground">{c.time}</p>
                  </div>
                </div>
              ))
            ) : (
              [
                "Maya pinned Northstar Logistics",
                "Alex connected Warehouse D-17",
                "Jules added an analyst note",
              ].map((x, i) => (
                <div className="flex gap-2 text-xs" key={x}>
                  <span className="mt-1 size-1.5 shrink-0 rounded-full bg-primary" />
                  <div>
                    <p>{x}</p>
                    <p className="mt-1 text-[10px] text-muted-foreground">{i * 12 + 4} min ago</p>
                  </div>
                </div>
              ))
            )}
          </div>
          <div className="mt-3 flex gap-2 border-t border-border pt-3">
            <Input
              placeholder="Add note or comment…"
              className="h-8 text-xs"
              value={commentInput}
              onChange={(e) => setCommentInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSendComment()}
            />
            <Button size="icon-sm" onClick={handleSendComment}>
              <Send className="size-3" />
            </Button>
          </div>
        </aside>
      </div>
    </div>
  );
}

// ============================================================================
// 2. ONTOLOGY MANAGER
// ============================================================================
export function OntologyPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [typeName, setTypeName] = useState("");
  const [typeDesc, setTypeDesc] = useState("");
  const [newPropName, setNewPropName] = useState("");
  const [activeSheetType, setActiveSheetType] = useState<string | null>(null);

  const objectTypes = useOrionStore((s) => s.objectTypes);
  const addObjectType = useOrionStore((s) => s.addObjectType);
  const addPropertyToType = useOrionStore((s) => s.addPropertyToType);

  const handleCreateType = async () => {
    if (!typeName.trim()) {
      toast.error("Object type name is required");
      return;
    }

    try {
      await addObjectType({
        name: typeName.trim(),
        description: typeDesc.trim() || "Domain entity definition",
      });

      toast.success(`Object type "${typeName.trim()}" added to ontology`);
      setTypeName("");
      setTypeDesc("");
      setCreateOpen(false);
    } catch (e) {
      toast.error("Failed to add object type");
    }
  };

  const handleAddProperty = (targetTypeName: string) => {
    if (!newPropName.trim()) {
      toast.error("Property name is required");
      return;
    }

    addPropertyToType(targetTypeName, newPropName.trim(), "Text");
    toast.success(`Property "${newPropName.trim()}" added to ${targetTypeName}`);
    setNewPropName("");
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Semantic layer"
        title="Ontology Manager"
        description="Model the objects, properties, actions, and links that power every Orion workflow."
        actions={
          <Dialog open={createOpen} onOpenChange={setCreateOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus />
                New object type
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Define new object type</DialogTitle>
                <DialogDescription>
                  Specify schema entity definitions and properties for the semantic layer.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-3">
                <div className="space-y-2">
                  <Label>Type name</Label>
                  <Input
                    placeholder="e.g. Vessel, Contract, Account"
                    value={typeName}
                    onChange={(e) => setTypeName(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Description</Label>
                  <Input
                    placeholder="Domain description and usage guidelines"
                    value={typeDesc}
                    onChange={(e) => setTypeDesc(e.target.value)}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setCreateOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateType}>Create type</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />
      <Tabs defaultValue="types">
        <TabsList>
          <TabsTrigger value="types">Object types ({objectTypes.length})</TabsTrigger>
          <TabsTrigger value="links">Link types</TabsTrigger>
          <TabsTrigger value="graph">Visual graph</TabsTrigger>
          <TabsTrigger value="versions">Versions</TabsTrigger>
        </TabsList>
        <TabsContent value="types" className="mt-5">
          <Section title="Object types" description={`${objectTypes.length} types registered in ontology`}>
            <div className="divide-y divide-border">
              {objectTypes.map((o) => (
                <Sheet
                  key={o.id || o.name}
                  open={activeSheetType === o.name}
                  onOpenChange={(isOpen) => setActiveSheetType(isOpen ? o.name : null)}
                >
                  <SheetTrigger asChild>
                    <button className="grid w-full grid-cols-[1fr_100px_100px_100px_24px] items-center px-5 py-4 text-left text-xs hover:bg-accent">
                      <span className="flex items-center gap-3 font-medium">
                        <span className="flex size-8 items-center justify-center rounded border border-border bg-muted">
                          <CircleDot className="size-4 text-primary" />
                        </span>
                        <span>
                          <span className="block font-semibold">{o.name}</span>
                          <span className="block text-[10px] text-muted-foreground">{o.description || "Ontology object"}</span>
                        </span>
                      </span>
                      <span>{o.properties} props</span>
                      <span>{o.links} links</span>
                      <span className="text-muted-foreground">{o.objects}</span>
                      <ChevronRight className="size-4" />
                    </button>
                  </SheetTrigger>
                  <SheetContent className="sm:max-w-xl">
                    <SheetHeader>
                      <SheetTitle>{o.name}</SheetTitle>
                      <SheetDescription>
                        {o.description || "Manage properties and ontology behavior for this entity."}
                      </SheetDescription>
                    </SheetHeader>
                    <div className="mt-8 space-y-4">
                      <div className="flex items-center justify-between">
                        <p className="text-xs font-semibold">Properties ({o.properties})</p>
                        <span className="text-[10px] text-muted-foreground">Version 28</span>
                      </div>
                      <div className="space-y-2">
                        {(o.propertyDefinitions && o.propertyDefinitions.length > 0
                          ? o.propertyDefinitions
                          : [
                              { name: "Legal name", type: "Text" },
                              { name: "External ID", type: "Text" },
                              { name: "Jurisdiction", type: "Text" },
                              { name: "Risk score", type: "Number" },
                              { name: "Last verified", type: "Date" },
                            ]
                        ).map((p, i) => (
                          <div
                            key={p.name + i}
                            className="flex items-center gap-3 rounded border border-border p-3 text-xs"
                          >
                            <GripVertical className="size-4 text-muted-foreground" />
                            <span className="flex-1 font-medium">{p.name}</span>
                            <span className="text-muted-foreground">{p.type}</span>
                            <MoreHorizontal className="size-4 text-muted-foreground" />
                          </div>
                        ))}
                      </div>
                      <div className="flex gap-2 pt-2">
                        <Input
                          placeholder="New property name (e.g. Tax ID)"
                          value={newPropName}
                          onChange={(e) => setNewPropName(e.target.value)}
                          onKeyDown={(e) => e.key === "Enter" && handleAddProperty(o.name)}
                        />
                        <Button onClick={() => handleAddProperty(o.name)}>
                          <Plus />
                          Add
                        </Button>
                      </div>
                    </div>
                  </SheetContent>
                </Sheet>
              ))}
            </div>
          </Section>
        </TabsContent>
        <TabsContent value="links" className="mt-5">
          <Section title="Link types" description="Defined relationship predicates">
            <div className="grid gap-px bg-border sm:grid-cols-2 lg:grid-cols-3">
              {["OWNS", "EMPLOYS", "LOCATED_AT", "SUPPLIES", "REFERENCES", "PART_OF"].map((x) => (
                <div className="bg-card p-5" key={x}>
                  <GitBranch className="size-4 text-primary" />
                  <p className="mt-5 text-sm font-semibold">{x}</p>
                  <p className="mt-1 text-xs text-muted-foreground">Directed · many-to-many</p>
                </div>
              ))}
            </div>
          </Section>
        </TabsContent>
        <TabsContent value="graph" className="mt-5 border border-border">
          <KnowledgeGraph editable />
        </TabsContent>
        <TabsContent value="versions" className="mt-5">
          <Section title="Version history">
            <div className="divide-y divide-border">
              {[
                "v28 · Procurement & Supply Chain ontology",
                "v27 · Entity resolution update",
                "v26 · Location hierarchy and cold chain nodes",
              ].map((x, i) => (
                <div className="flex justify-between px-5 py-4 text-xs" key={x}>
                  <span>{x}</span>
                  <span className="text-muted-foreground">{i === 0 ? "Current" : "Published"}</span>
                </div>
              ))}
            </div>
          </Section>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ============================================================================
// 3. DATA INGESTION
// ============================================================================
export function IngestionPage() {
  const [open, setOpen] = useState(false);
  const [pipeName, setPipeName] = useState("");
  const [pipeSource, setPipeSource] = useState("PostgreSQL");
  const [pipeSchedule, setPipeSchedule] = useState("Every 15 min");

  const pipelines = useOrionStore((s) => s.pipelines);
  const addPipeline = useOrionStore((s) => s.addPipeline);
  const runPipeline = useOrionStore((s) => s.runPipeline);
  const togglePipelineStatus = useOrionStore((s) => s.togglePipelineStatus);

  const activeCount = pipelines.filter((p) => p.status === "Operational" || p.status === "Active").length;
  const totalRecords = pipelines.reduce((sum, p) => sum + (p.numericRecords || 1000000), 0);
  const displayTotal = (totalRecords / 1000000).toFixed(2) + "M";

  const handleSavePipeline = async () => {
    if (!pipeName.trim()) {
      toast.error("Pipeline name is required");
      return;
    }

    try {
      await addPipeline({
        name: pipeName.trim(),
        source: pipeSource,
        schedule: pipeSchedule,
      });

      toast.success(`Ingestion pipeline "${pipeName.trim()}" created`);
      setPipeName("");
      setOpen(false);
    } catch (e) {
      toast.error("Failed to create pipeline");
    }
  };

  const handleRun = async (pipelineId: string, name: string) => {
    toast.info(`Executing pipeline: ${name}…`);
    await runPipeline(pipelineId);
    toast.success(`Pipeline "${name}" successfully processed new records`);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Data operations"
        title="Data Ingestion"
        description="Connect, map, validate, and monitor enterprise data pipelines."
        actions={
          <Button onClick={() => setOpen(true)}>
            <Plus />
            New pipeline
          </Button>
        }
      />
      <div className="grid gap-4 md:grid-cols-3">
        {(
          [
            [String(activeCount), "Active pipelines", Workflow],
            [displayTotal, "Rows processed", DatabaseZap],
            ["99.94%", "Success rate", Check],
          ] as [string, string, LucideIcon][]
        ).map(([v, l, I]) => (
          <div className="border border-border bg-card p-5" key={l as string}>
            <I className="size-4 text-primary" />
            <p className="mt-6 font-display text-2xl font-semibold">{v}</p>
            <p className="mt-1 text-xs text-muted-foreground">{l as string}</p>
          </div>
        ))}
      </div>
      <Section title="Pipelines" description="All sources and transformation jobs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border bg-muted/40 text-muted-foreground">
              <tr>
                {["Pipeline", "Source", "Schedule", "Records", "Last run", "Status", "Actions"].map((x) => (
                  <th key={x} className="px-5 py-3 font-medium">
                    {x}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {pipelines.map((p) => (
                <tr key={p.id || p.name} className="border-b border-border last:border-0 hover:bg-accent">
                  <td className="px-5 py-4 font-medium">{p.name}</td>
                  <td className="px-5 py-4">{p.source}</td>
                  <td className="px-5 py-4 text-muted-foreground">{p.runs}</td>
                  <td className="px-5 py-4 tabular-nums font-semibold">{p.records}</td>
                  <td className="px-5 py-4 text-muted-foreground">{p.last}</td>
                  <td className="px-5 py-4">
                    <button onClick={() => togglePipelineStatus(p.id)}>
                      <StatusBadge status={p.status} />
                    </button>
                  </td>
                  <td className="px-5 py-4">
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-7 text-[11px]"
                      onClick={() => handleRun(p.id, p.name)}
                    >
                      <RotateCcw className="mr-1 size-3" />
                      Run now
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Section>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Create ingestion pipeline</DialogTitle>
            <DialogDescription>
              Configure a source and map incoming fields to the ontology.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-6 py-3 sm:grid-cols-2">
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Pipeline name</Label>
                <Input
                  placeholder="e.g. Finance Warehouse Feed"
                  value={pipeName}
                  onChange={(e) => setPipeName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>Source type</Label>
                <div className="grid grid-cols-2 gap-2">
                  {["PostgreSQL", "Snowflake", "S3", "Kafka"].map((x) => (
                    <button
                      key={x}
                      type="button"
                      onClick={() => setPipeSource(x)}
                      className={`border p-3 text-left text-xs transition-colors ${
                        pipeSource === x ? "border-primary bg-primary/10 font-medium" : "border-border hover:bg-muted"
                      }`}
                    >
                      {x}
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-2">
                <Label>Schedule</Label>
                <div className="flex gap-1">
                  {["Every 15 min", "Hourly", "Daily", "Streaming"].map((s) => (
                    <Button
                      key={s}
                      type="button"
                      size="sm"
                      variant={pipeSchedule === s ? "secondary" : "outline"}
                      className="text-[10px] px-2 h-7"
                      onClick={() => setPipeSchedule(s)}
                    >
                      {s}
                    </Button>
                  ))}
                </div>
              </div>
            </div>
            <div>
              <p className="mb-3 text-xs font-medium">Field mapping preview</p>
              {[
                ["vendor_id", "Organization.external_id"],
                ["legal_name", "Organization.legal_name"],
                ["country", "Location.country"],
                ["amount_eur", "Transaction.amount"],
              ].map((x) => (
                <div className="mb-2 flex items-center gap-2 text-[11px]" key={x[0]}>
                  <span className="flex-1 rounded border border-border p-2">{x[0]}</span>
                  <span>→</span>
                  <span className="flex-1 rounded border border-border p-2 text-primary font-medium">
                    {x[1]}
                  </span>
                </div>
              ))}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSavePipeline}>Save pipeline</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ============================================================================
// 4. APPLICATIONS & BUILDER
// ============================================================================
export function ApplicationsPage() {
  const apps = useOrionStore((s) => s.apps);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Operational apps"
        title="Applications"
        description="Compose role-specific workflows on top of the governed knowledge layer."
        actions={
          <Button asChild>
            <Link to="/applications/$appId" params={{ appId: "new" }}>
              <Plus />
              Create application
            </Link>
          </Button>
        }
      />
      <div className="grid gap-4 lg:grid-cols-3">
        {apps.map((app) => (
          <Link
            to="/applications/$appId"
            params={{ appId: app.id || "app-0" }}
            key={app.id || app.name}
            className="border border-border bg-card p-5 hover:border-primary/50 transition-colors"
          >
            <div className="flex justify-between">
              <AppWindow className="size-5 text-primary" />
              <StatusBadge status={app.status} />
            </div>
            <h2 className="mt-7 font-display text-base font-semibold">{app.name}</h2>
            <p className="mt-2 min-h-10 text-xs leading-5 text-muted-foreground">
              {app.description}
            </p>
            <div className="mt-5 flex justify-between border-t border-border pt-4 text-[10px] text-muted-foreground">
              <span>{app.widgets} widgets</span>
              <span>{app.owner}</span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

export function AppBuilderPage({ appId }: { appId?: string }) {
  const navigate = useNavigate();
  const apps = useOrionStore((s) => s.apps);
  const addApp = useOrionStore((s) => s.addApp);
  const updateApp = useOrionStore((s) => s.updateApp);

  const existingApp = apps.find((a) => a.id === appId);
  const [appName, setAppName] = useState(existingApp?.name || "Transaction Review OS");
  const [appDesc, setAppDesc] = useState(
    existingApp?.description || "Triage anomalous payments with linked evidence",
  );

  const handlePublish = async () => {
    if (!appName.trim()) {
      toast.error("Application name is required");
      return;
    }

    if (existingApp) {
      updateApp(existingApp.id, { name: appName, description: appDesc, status: "Operational" });
      toast.success(`Application "${appName}" updated and published`);
    } else {
      await addApp({
        name: appName,
        description: appDesc,
        widgets: 8,
        status: "Operational",
      });
      toast.success(`Application "${appName}" published successfully`);
    }

    navigate({ to: "/applications" });
  };

  return (
    <div className="-m-5 lg:-m-7">
      <div className="flex h-14 items-center border-b border-border px-4">
        <Link to="/applications" className="text-xs text-muted-foreground">
          Applications
        </Link>
        <ChevronRight className="mx-2 size-3 text-muted-foreground" />
        <span className="text-xs font-medium">{appName}</span>
        <div className="ml-2">
          <StatusBadge status="Draft" />
        </div>
        <div className="ml-auto flex gap-2">
          <Button variant="outline" size="sm" onClick={() => toast.info("Live preview running")}>
            <Play />
            Preview
          </Button>
          <Button size="sm" onClick={handlePublish}>Publish</Button>
        </div>
      </div>
      <div className="grid h-[calc(100vh-112px)] grid-cols-[220px_1fr_280px]">
        <aside className="border-r border-border bg-card p-3">
          <p className="px-2 py-2 text-[10px] font-semibold uppercase text-muted-foreground">
            Widget library
          </p>
          {(
            [
              [Table2, "Table"],
              [PanelRight, "Detail view"],
              [Network, "Graph"],
              [FormInput, "Form"],
              [LayoutGrid, "KPI"],
              [CalendarClock, "Timeline"],
            ] as [LucideIcon, string][]
          ).map(([I, l]) => (
            <button
              key={l as string}
              onClick={() => toast.info(`Widget added: ${l}`)}
              className="mb-1 flex w-full items-center gap-3 rounded p-2.5 text-xs hover:bg-accent transition-colors"
            >
              <I className="size-4 text-muted-foreground" />
              {l as string}
              <GripVertical className="ml-auto size-3 text-muted-foreground" />
            </button>
          ))}
        </aside>
        <div className="bg-graph p-8 overflow-y-auto">
          <div className="mx-auto max-w-4xl border border-border bg-background shadow-xl">
            <div className="flex h-12 items-center border-b border-border px-4">
              <span className="text-sm font-semibold">{appName}</span>
            </div>
            <div className="grid gap-4 p-5 md:grid-cols-3">
              <div className="border border-primary/40 bg-card p-4">
                <p className="text-[10px] text-muted-foreground">Open cases</p>
                <p className="mt-2 text-2xl font-semibold">128</p>
              </div>
              <div className="border border-border bg-card p-4">
                <p className="text-[10px] text-muted-foreground">Flagged value</p>
                <p className="mt-2 text-2xl font-semibold">$4.2M</p>
              </div>
              <div className="border border-border bg-card p-4">
                <p className="text-[10px] text-muted-foreground">SLA compliance</p>
                <p className="mt-2 text-2xl font-semibold">94%</p>
              </div>
              <div className="col-span-full h-64 border border-dashed border-border bg-card p-4">
                <p className="text-xs font-medium">Flagged transactions</p>
                <div className="mt-6 space-y-3">
                  {[1, 2, 3, 4].map((x) => (
                    <div key={x} className="h-8 rounded bg-muted/60 flex items-center px-3 text-[11px] text-muted-foreground">
                      TX-998{x} · Northstar Logistics → Warehouse D-17 · €{x * 350},000
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
        <aside className="border-l border-border bg-card p-4">
          <p className="text-xs font-semibold">Inspector</p>
          <div className="mt-5 space-y-4">
            <div className="space-y-2">
              <Label>Application title</Label>
              <Input value={appName} onChange={(e) => setAppName(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Input value={appDesc} onChange={(e) => setAppDesc(e.target.value)} />
            </div>
            <div className="flex items-center justify-between text-xs pt-2">
              <span>Show trends</span>
              <Switch defaultChecked />
            </div>
            <div className="flex items-center justify-between text-xs">
              <span>Interactive drilldown</span>
              <Switch defaultChecked />
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}

// ============================================================================
// 5. SETTINGS & ADMIN
// ============================================================================
export function SettingsPage({ admin = false }: { admin?: boolean }) {
  const profile = useOrionStore((s) => s.profile);
  const updateProfile = useOrionStore((s) => s.updateProfile);

  const [firstName, setFirstName] = useState(profile.firstName);
  const [lastName, setLastName] = useState(profile.lastName);
  const [email, setEmail] = useState(profile.email);
  const [domain, setDomain] = useState(profile.domain);
  const [mfa, setMfa] = useState(profile.mfa);
  const [weeklySummary, setWeeklySummary] = useState(profile.weeklySummary);

  const handleSave = () => {
    updateProfile({
      firstName,
      lastName,
      email,
      domain,
      mfa,
      weeklySummary,
    });
    toast.success("Workspace and profile settings saved");
  };

  const sections = admin
    ? [
        { t: "User administration", d: "Manage accounts, roles, and access policies", i: Users },
        { t: "Audit log", d: "Review privileged changes and security events", i: FileClock },
        { t: "Platform controls", d: "Configure retention, integrations, and limits", i: Shield },
      ]
    : [
        { t: "Profile", d: "Personal details and notifications", i: Users },
        { t: "Tenant settings", d: "Workspace identity and defaults", i: Settings2 },
        { t: "API keys", d: "Manage programmatic workspace access", i: KeyRound },
        { t: "Team & roles", d: "Invite members and manage permissions", i: Shield },
        { t: "Appearance", d: "Theme and interface density", i: Columns3 },
      ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow={admin ? "Governance" : "Workspace"}
        title={admin ? "Administration" : "Settings"}
        description={
          admin
            ? "Control access, audit activity, and enterprise platform policies."
            : "Manage your profile, organization, access, and interface preferences."
        }
      />
      <div className="grid gap-5 lg:grid-cols-[240px_1fr]">
        <nav className="space-y-1">
          {sections.map((s, i) => (
            <button
              key={s.t}
              className={`flex w-full items-center gap-3 rounded px-3 py-2.5 text-left text-xs ${
                i === 0 ? "bg-accent font-medium text-foreground" : "text-muted-foreground hover:bg-accent"
              }`}
            >
              <s.i className="size-4" />
              {s.t}
            </button>
          ))}
        </nav>
        <Section title={sections[0]?.t ?? ""} description={sections[0]?.d ?? ""}>
          <div className="max-w-xl space-y-6 p-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>First name</Label>
                <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label>Last name</Label>
                <Input value={lastName} onChange={(e) => setLastName(e.target.value)} />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Email address</Label>
              <Input value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Workspace domain</Label>
              <Input value={domain} onChange={(e) => setDomain(e.target.value)} />
            </div>
            <div className="flex items-center justify-between border-t border-border pt-5">
              <div>
                <p className="text-xs font-medium">Require multi-factor authentication</p>
                <p className="mt-1 text-[11px] text-muted-foreground">
                  Enforce hardware keys or authenticator apps for workspace logins.
                </p>
              </div>
              <Switch checked={mfa} onCheckedChange={setMfa} />
            </div>
            <div className="flex items-center justify-between border-t border-border pt-5">
              <div>
                <p className="text-xs font-medium">Weekly intelligence summary</p>
                <p className="mt-1 text-[11px] text-muted-foreground">
                  Receive a weekly overview of pipeline events, new objects, and assigned items.
                </p>
              </div>
              <Switch checked={weeklySummary} onCheckedChange={setWeeklySummary} />
            </div>
            <Button onClick={handleSave}>Save changes</Button>
          </div>
        </Section>
      </div>
    </div>
  );
}
