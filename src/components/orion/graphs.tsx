import { useCallback } from "react";
import {
  Background,
  Controls,
  Handle,
  MiniMap,
  Position,
  ReactFlow,
  addEdge,
  useEdgesState,
  useNodesState,
  type Connection,
  type Edge,
  type Node,
  type NodeProps,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { Building2, Database, MapPin, UserRound } from "lucide-react";
const icons = { organization: Building2, person: UserRound, location: MapPin, dataset: Database };
function EntityNode({
  data,
}: {
  data: { label: string; kind: keyof typeof icons; subtitle: string };
}) {
  const Icon = icons[data.kind] ?? Database;
  return (
    <div className="min-w-40 rounded-md border border-border bg-card p-3 shadow-lg">
      <Handle type="target" position={Position.Left} />
      <div className="flex gap-2.5">
        <span className="flex size-7 items-center justify-center rounded bg-primary/10 text-primary">
          <Icon className="size-3.5" />
        </span>
        <div>
          <p className="text-xs font-semibold">{data.label}</p>
          <p className="mt-0.5 text-[10px] text-muted-foreground">{data.subtitle}</p>
        </div>
      </div>
      <Handle type="source" position={Position.Right} />
    </div>
  );
}
const nodeTypes = { entity: EntityNode };
const baseNodes: Node[] = [
  {
    id: "1",
    type: "entity",
    position: { x: 40, y: 120 },
    data: { label: "Northstar Logistics", kind: "organization", subtitle: "Organization" },
  },
  {
    id: "2",
    type: "entity",
    position: { x: 330, y: 25 },
    data: { label: "Warehouse D-17", kind: "location", subtitle: "Operates" },
  },
  {
    id: "3",
    type: "entity",
    position: { x: 340, y: 215 },
    data: { label: "Elena Vasquez", kind: "person", subtitle: "Procurement lead" },
  },
  {
    id: "4",
    type: "entity",
    position: { x: 640, y: 120 },
    data: { label: "Q4 Vendor Ledger", kind: "dataset", subtitle: "Referenced by" },
  },
];
const baseEdges: Edge[] = [
  { id: "1-2", source: "1", target: "2", label: "OPERATES", animated: true },
  { id: "1-3", source: "1", target: "3", label: "EMPLOYS" },
  { id: "2-4", source: "2", target: "4", label: "RECORDED_IN" },
];
export function KnowledgeGraph({ editable = false }: { editable?: boolean }) {
  const [nodes, setNodes, onNodesChange] = useNodesState(baseNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(baseEdges);
  const onConnect = useCallback(
    (params: Connection) => setEdges((es) => addEdge({ ...params, animated: true }, es)),
    [setEdges],
  );
  return (
    <div className="h-[430px] w-full bg-graph">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        nodeTypes={nodeTypes}
        fitView
        nodesDraggable={editable}
        nodesConnectable={editable}
        proOptions={{ hideAttribution: true }}
      >
        <Background gap={24} size={1} />
        <Controls />
        <MiniMap zoomable pannable />
      </ReactFlow>
    </div>
  );
}
