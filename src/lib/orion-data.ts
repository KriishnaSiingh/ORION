import type { LucideIcon } from "lucide-react";
import { Building2, Database, FileText, Network, Shield, UserRound } from "lucide-react";

export type Status = "Operational" | "Active" | "Review" | "Draft" | "Paused" | "Warning";
export type Entity = {
  id: string;
  name: string;
  type: string;
  description: string;
  owner: string;
  updated: string;
  confidence: number;
  status: Status;
};
export const entities: Entity[] = [
  {
    id: "OBJ-4921",
    name: "Northstar Logistics",
    type: "Organization",
    description: "Strategic distribution partner in EMEA",
    owner: "Maya Chen",
    updated: "4 min ago",
    confidence: 98,
    status: "Active",
  },
  {
    id: "OBJ-4918",
    name: "Project Sable",
    type: "Investigation",
    description: "Cross-border supply chain anomaly",
    owner: "Alex Rivera",
    updated: "18 min ago",
    confidence: 92,
    status: "Review",
  },
  {
    id: "OBJ-4907",
    name: "Warehouse D-17",
    type: "Location",
    description: "Rotterdam fulfillment and cold storage hub",
    owner: "Operations",
    updated: "43 min ago",
    confidence: 96,
    status: "Operational",
  },
  {
    id: "OBJ-4884",
    name: "Elena Vasquez",
    type: "Person",
    description: "Regional procurement lead",
    owner: "People Ops",
    updated: "2 hr ago",
    confidence: 87,
    status: "Active",
  },
  {
    id: "OBJ-4862",
    name: "Q4 Vendor Ledger",
    type: "Dataset",
    description: "Consolidated procurement transactions",
    owner: "Finance",
    updated: "5 hr ago",
    confidence: 99,
    status: "Operational",
  },
  {
    id: "OBJ-4821",
    name: "Signal Report 228",
    type: "Document",
    description: "Third-party risk assessment",
    owner: "Risk",
    updated: "Yesterday",
    confidence: 78,
    status: "Warning",
  },
];
export const typeIcons: Record<string, LucideIcon> = {
  Organization: Building2,
  Investigation: Network,
  Location: Database,
  Person: UserRound,
  Dataset: Database,
  Document: FileText,
  Policy: Shield,
};
export const chartData = [
  { day: "Sep 13", objects: 1180, searches: 520 },
  { day: "Sep 14", objects: 1340, searches: 690 },
  { day: "Sep 15", objects: 1290, searches: 810 },
  { day: "Sep 16", objects: 1580, searches: 760 },
  { day: "Sep 17", objects: 1710, searches: 920 },
  { day: "Sep 18", objects: 1890, searches: 1100 },
  { day: "Sep 19", objects: 2140, searches: 1280 },
];
export const activities = [
  {
    title: "Pipeline ERP-SAP completed",
    detail: "18,429 objects processed without errors",
    time: "4m",
    tone: "success",
  },
  {
    title: "New link proposed",
    detail: "Northstar Logistics → Warehouse D-17",
    time: "18m",
    tone: "info",
  },
  {
    title: "Ontology version 28 published",
    detail: "3 object types and 8 properties changed",
    time: "41m",
    tone: "info",
  },
  {
    title: "Quality threshold breached",
    detail: "Vendor Ledger missing jurisdiction values",
    time: "1h",
    tone: "warning",
  },
  {
    title: "Investigation shared",
    detail: "Project Sable shared with Risk Operations",
    time: "2h",
    tone: "info",
  },
];
export const pipelines = [
  {
    name: "ERP — SAP S/4HANA",
    source: "PostgreSQL",
    records: "18.4M",
    last: "6 min ago",
    status: "Operational",
    runs: "Every 15 min",
  },
  {
    name: "Vendor Risk Feed",
    source: "S3 / Parquet",
    records: "2.8M",
    last: "34 min ago",
    status: "Operational",
    runs: "Hourly",
  },
  {
    name: "Identity Resolution",
    source: "Kafka",
    records: "928K",
    last: "Live",
    status: "Active",
    runs: "Streaming",
  },
  {
    name: "Legacy CRM",
    source: "Oracle",
    records: "4.1M",
    last: "2 days ago",
    status: "Warning",
    runs: "Daily",
  },
];
export const boards = [
  {
    name: "Operation Northstar",
    description: "Supplier concentration and geopolitical exposure",
    objects: 38,
    collaborators: 7,
    updated: "8 minutes ago",
    status: "Active",
  },
  {
    name: "Sable Transaction Review",
    description: "Payments, beneficial ownership, and linked entities",
    objects: 24,
    collaborators: 4,
    updated: "42 minutes ago",
    status: "Review",
  },
  {
    name: "EMEA Supply Resilience",
    description: "Alternative routes and critical dependencies",
    objects: 61,
    collaborators: 12,
    updated: "Yesterday",
    status: "Active",
  },
];
export const objectTypes = [
  { name: "Organization", properties: 24, links: 18, objects: "48.2K", updated: "Today" },
  { name: "Person", properties: 19, links: 14, objects: "182K", updated: "Today" },
  { name: "Location", properties: 16, links: 21, objects: "12.9K", updated: "Yesterday" },
  { name: "Transaction", properties: 31, links: 11, objects: "4.8M", updated: "Sep 17" },
  { name: "Asset", properties: 22, links: 17, objects: "86.4K", updated: "Sep 15" },
];
export const apps = [
  {
    name: "Supplier Command Center",
    description: "Monitor vendor health, exposure, and delivery risk",
    widgets: 12,
    owner: "Operations",
    status: "Operational",
  },
  {
    name: "Transaction Review",
    description: "Triage anomalous payments with linked evidence",
    widgets: 8,
    owner: "Risk",
    status: "Draft",
  },
  {
    name: "Executive Network View",
    description: "Explore leadership and beneficial ownership",
    widgets: 6,
    owner: "Strategy",
    status: "Operational",
  },
];
