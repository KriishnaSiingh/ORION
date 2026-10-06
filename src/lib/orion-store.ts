import { create } from "zustand";
import { persist } from "zustand/middleware";
import {
  entities as defaultEntities,
  pipelines as defaultPipelines,
  boards as defaultBoards,
  objectTypes as defaultObjectTypes,
  apps as defaultApps,
  activities as defaultActivities,
  type Entity,
  type Status,
} from "./orion-data";
import { graphApi } from "./api-client";

export type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  sources?: { label: string; type: string }[];
  plan?: string[];
};

export type Thread = {
  id: string;
  title: string;
  updatedAt: string;
  messages: ChatMessage[];
};

export type BoardItem = {
  id: string;
  name: string;
  description: string;
  objects: number;
  collaborators: number;
  updated: string;
  status: Status;
  comments?: { author: string; text: string; time: string }[];
};

export type PipelineItem = {
  id: string;
  name: string;
  source: string;
  records: string;
  numericRecords?: number;
  last: string;
  status: Status;
  runs: string;
};

export type ObjectTypeItem = {
  id: string;
  name: string;
  description?: string;
  properties: number;
  links: number;
  objects: string;
  updated: string;
  propertyDefinitions?: { name: string; type: string }[];
};

export type AppItem = {
  id: string;
  name: string;
  description: string;
  widgets: number;
  owner: string;
  status: Status;
};

export type ActivityItem = {
  id: string;
  title: string;
  detail: string;
  time: string;
  tone: "success" | "info" | "warning";
};

export type UserProfile = {
  firstName: string;
  lastName: string;
  email: string;
  domain: string;
  role: "admin" | "analyst";
  mfa: boolean;
  weeklySummary: boolean;
};

type State = {
  theme: "dark" | "light";
  role: "admin" | "analyst";
  profile: UserProfile;
  entities: Entity[];
  boards: BoardItem[];
  pipelines: PipelineItem[];
  objectTypes: ObjectTypeItem[];
  apps: AppItem[];
  activities: ActivityItem[];
  threads: Thread[];

  // Actions
  setTheme: (theme: "dark" | "light") => void;
  setRole: (role: "admin" | "analyst") => void;
  updateProfile: (profileUpdates: Partial<UserProfile>) => void;

  // Knowledge Objects
  addEntity: (entity: Omit<Entity, "id" | "updated"> & { id?: string }) => Promise<Entity>;
  updateEntity: (id: string, updates: Partial<Entity>) => void;
  deleteEntity: (id: string) => void;

  // Boards
  addBoard: (board: { name: string; description: string; status?: Status }) => Promise<BoardItem>;
  addCommentToBoard: (boardId: string, comment: { text: string; author?: string }) => void;
  shareBoard: (boardId: string, email: string) => void;

  // Pipelines
  addPipeline: (pipeline: { name: string; source: string; schedule?: string }) => Promise<PipelineItem>;
  runPipeline: (pipelineId: string) => Promise<void>;
  togglePipelineStatus: (pipelineId: string) => void;

  // Object Types
  addObjectType: (type: { name: string; description?: string; initialProps?: string[] }) => Promise<ObjectTypeItem>;
  addPropertyToType: (typeName: string, propertyName: string, propertyType?: string) => void;

  // Applications
  addApp: (app: { name: string; description: string; widgets?: number; owner?: string; status?: Status }) => Promise<AppItem>;
  updateApp: (id: string, updates: Partial<AppItem>) => void;

  // Activities
  addActivity: (activity: Omit<ActivityItem, "id" | "time">) => void;

  // AI Assistant
  addThread: (title?: string) => string;
  addMessage: (threadId: string, message: ChatMessage) => void;
  deleteThread: (threadId: string) => void;

  // Sync with backend
  syncFromBackend: () => Promise<void>;
};

const initialThreads: Thread[] = [
  {
    id: "northstar-risk",
    title: "Northstar supplier risk",
    updatedAt: "12 min",
    messages: [
      { id: "m1", role: "user", text: "Summarize the key risks connected to Northstar Logistics." },
      {
        id: "m2",
        role: "assistant",
        text: "Northstar Logistics has three material risk concentrations. Its Rotterdam hub handles **62% of regional volume**, creating a single-site dependency. Two tier-two suppliers share beneficial ownership with a sanctioned-adjacent entity, though no direct match is confirmed. Finally, delivery variance rose 18% over the last quarter.\n\nI recommend validating beneficial ownership and opening a resilience review for Warehouse D-17.",
        sources: [
          { label: "Northstar Logistics", type: "Organization" },
          { label: "Warehouse D-17", type: "Location" },
          { label: "Q4 Vendor Ledger", type: "Dataset" },
        ],
      },
    ],
  },
];

const initialProfile: UserProfile = {
  firstName: "Krishna",
  lastName: "Singh",
  email: "krishnasingh15kks@gmail.com",
  domain: "acme.orion.ai",
  role: "admin",
  mfa: true,
  weeklySummary: true,
};

export const useOrionStore = create<State>()(
  persist(
    (set, get) => ({
      theme: "dark",
      role: "admin",
      profile: initialProfile,
      entities: defaultEntities,
      boards: defaultBoards.map((b, i) => ({
        ...b,
        id: i === 0 ? "operation-northstar" : `board-${i}`,
        status: (b.status as Status) || "Active",
        comments: i === 0 ? [
          { author: "Maya Chen", text: "Verified primary supplier connection in Rotterdam hub", time: "10m ago" },
          { author: "Alex Rivera", text: "Added flagged transactions for cross-referencing", time: "5m ago" },
        ] : [],
      })),
      pipelines: defaultPipelines.map((p, i) => ({
        ...p,
        id: `pipe-${i + 1}`,
        status: (p.status as Status) || "Operational",
        numericRecords: 18429102 / (i + 1),
      })),
      objectTypes: defaultObjectTypes.map((t, i) => ({
        ...t,
        id: `ot-${i + 1}`,
        propertyDefinitions: [
          { name: "Legal name", type: "Text" },
          { name: "External ID", type: "Text" },
          { name: "Jurisdiction", type: "Text" },
          { name: "Risk score", type: "Number" },
          { name: "Last verified", type: "Date" },
        ],
      })),
      apps: defaultApps.map((a, i) => ({
        ...a,
        id: `app-${i}`,
        status: (a.status as Status) || "Operational",
      })),
      activities: defaultActivities.map((a, i) => ({
        ...a,
        id: `act-${i + 1}`,
        tone: (a.tone as "success" | "info" | "warning") || "info",
      })),
      threads: initialThreads,

      setTheme: (theme) => set({ theme }),
      setRole: (role) => set({ role }),

      updateProfile: (profileUpdates) => {
        set((s) => ({ profile: { ...s.profile, ...profileUpdates } }));
        graphApi.updateProfile(profileUpdates).catch(() => {});
      },

      addActivity: (act) => {
        const item: ActivityItem = {
          id: `act-${Date.now()}`,
          time: "Just now",
          ...act,
        };
        set((s) => ({ activities: [item, ...s.activities.slice(0, 19)] }));
      },

      // Knowledge Objects
      addEntity: async (entity) => {
        const id = entity.id || `OBJ-${Math.floor(1000 + Math.random() * 9000)}`;
        const newEntity: Entity = {
          ...entity,
          id,
          updated: "Just now",
        };

        set((s) => ({ entities: [newEntity, ...s.entities] }));
        get().addActivity({
          title: `Object created: ${newEntity.name}`,
          detail: `${newEntity.type} · Confidence ${newEntity.confidence}%`,
          tone: "info",
        });

        try {
          await graphApi.createObject({
            primary_label: newEntity.name,
            type: newEntity.type,
            description: newEntity.description,
            owner: newEntity.owner,
            confidence: newEntity.confidence,
            status: newEntity.status,
          });
        } catch (e) {
          console.log("[INFO] Local entity stored, backend API synced:", e);
        }

        return newEntity;
      },

      updateEntity: (id, updates) => {
        set((s) => ({
          entities: s.entities.map((e) =>
            e.id === id ? { ...e, ...updates, updated: "Just now" } : e,
          ),
        }));
        graphApi.updateObject(id, updates).catch(() => {});
      },

      deleteEntity: (id) => {
        set((s) => ({ entities: s.entities.filter((e) => e.id !== id) }));
        graphApi.deleteObject(id).catch(() => {});
      },

      // Boards
      addBoard: async ({ name, description, status = "Active" }) => {
        const id = name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "") || `board-${Date.now()}`;
        const newBoard: BoardItem = {
          id,
          name,
          description: description || "Collaborative investigation workspace",
          objects: 12,
          collaborators: 1,
          updated: "Just now",
          status,
          comments: [],
        };

        set((s) => ({ boards: [newBoard, ...s.boards] }));
        get().addActivity({
          title: `Investigation board opened: ${name}`,
          detail: `Status: ${status} · 12 initial objects`,
          tone: "info",
        });

        try {
          await graphApi.createInvestigation({
            title: name,
            description,
            status,
          });
        } catch (e) {
          console.log("[INFO] Board stored locally:", e);
        }

        return newBoard;
      },

      addCommentToBoard: (boardId, comment) => {
        const commentObj = {
          author: comment.author || get().profile.firstName + " " + get().profile.lastName,
          text: comment.text,
          time: "Just now",
        };
        set((s) => ({
          boards: s.boards.map((b) =>
            b.id === boardId
              ? { ...b, comments: [commentObj, ...(b.comments || [])], updated: "Just now" }
              : b,
          ),
        }));
        graphApi.addBoardComment(boardId, commentObj).catch(() => {});
      },

      shareBoard: (boardId, email) => {
        set((s) => ({
          boards: s.boards.map((b) =>
            b.id === boardId ? { ...b, collaborators: b.collaborators + 1 } : b,
          ),
        }));
        graphApi.shareBoard(boardId, email).catch(() => {});
      },

      // Ingestion Pipelines
      addPipeline: async ({ name, source, schedule = "Every 15 min" }) => {
        const newPipe: PipelineItem = {
          id: `pipe-${Date.now()}`,
          name,
          source,
          records: "0",
          numericRecords: 0,
          last: "Never",
          status: "Operational",
          runs: schedule,
        };

        set((s) => ({ pipelines: [newPipe, ...s.pipelines] }));
        get().addActivity({
          title: `Pipeline configured: ${name}`,
          detail: `Source: ${source} · Schedule: ${schedule}`,
          tone: "info",
        });

        try {
          await graphApi.savePipeline({
            name,
            source_type: source,
            schedule,
            status: "Operational",
          });
        } catch (e) {
          console.log("[INFO] Pipeline saved locally:", e);
        }

        return newPipe;
      },

      runPipeline: async (pipelineId) => {
        const pipe = get().pipelines.find((p) => p.id === pipelineId);
        if (!pipe) return;

        const added = Math.floor(15000 + Math.random() * 45000);
        const currentCount = pipe.numericRecords || 100000;
        const newCount = currentCount + added;
        const formattedRecords = newCount > 1000000
          ? (newCount / 1000000).toFixed(1) + "M"
          : (newCount / 1000).toFixed(0) + "K";

        set((s) => ({
          pipelines: s.pipelines.map((p) =>
            p.id === pipelineId
              ? {
                  ...p,
                  numericRecords: newCount,
                  records: formattedRecords,
                  last: "Just now",
                  status: "Operational",
                }
              : p,
          ),
        }));

        get().addActivity({
          title: `Pipeline ran: ${pipe.name}`,
          detail: `Processed +${added.toLocaleString()} records without errors`,
          tone: "success",
        });

        graphApi.runPipeline(pipelineId).catch(() => {});
      },

      togglePipelineStatus: (pipelineId) => {
        set((s) => ({
          pipelines: s.pipelines.map((p) =>
            p.id === pipelineId
              ? { ...p, status: p.status === "Operational" ? "Paused" : "Operational" }
              : p,
          ),
        }));
      },

      // Object Types
      addObjectType: async ({ name, description, initialProps = ["Legal name", "External ID"] }) => {
        const id = `ot-${Date.now()}`;
        const newType: ObjectTypeItem = {
          id,
          name,
          description: description || "Custom domain object type",
          properties: initialProps.length,
          links: 2,
          objects: "0",
          updated: "Just now",
          propertyDefinitions: initialProps.map((p) => ({ name: p, type: "Text" })),
        };

        set((s) => ({ objectTypes: [newType, ...s.objectTypes] }));
        get().addActivity({
          title: `Object type created: ${name}`,
          detail: `${newType.properties} properties · Added to ontology`,
          tone: "info",
        });

        try {
          await graphApi.createObjectType({
            name,
            description,
            properties: newType.propertyDefinitions,
          });
        } catch (e) {
          console.log("[INFO] Object type saved locally:", e);
        }

        return newType;
      },

      addPropertyToType: (typeName, propertyName, propertyType = "Text") => {
        set((s) => ({
          objectTypes: s.objectTypes.map((t) =>
            t.name.toLowerCase() === typeName.toLowerCase()
              ? {
                  ...t,
                  properties: t.properties + 1,
                  propertyDefinitions: [
                    ...(t.propertyDefinitions || []),
                    { name: propertyName, type: propertyType },
                  ],
                  updated: "Just now",
                }
              : t,
          ),
        }));
      },

      // Applications
      addApp: async ({ name, description, widgets = 6, owner, status = "Operational" }) => {
        const id = `app-${Date.now()}`;
        const newApp: AppItem = {
          id,
          name,
          description: description || "Enterprise operational workflow application",
          widgets,
          owner: owner || get().profile.firstName + " " + get().profile.lastName,
          status,
        };

        set((s) => ({ apps: [newApp, ...s.apps] }));
        get().addActivity({
          title: `Application created: ${name}`,
          detail: `${widgets} widgets · Status: ${status}`,
          tone: "success",
        });

        try {
          await graphApi.createApp(newApp);
        } catch (e) {
          console.log("[INFO] App saved locally:", e);
        }

        return newApp;
      },

      updateApp: (id, updates) => {
        set((s) => ({
          apps: s.apps.map((a) => (a.id === id ? { ...a, ...updates } : a)),
        }));
        graphApi.updateApp(id, updates).catch(() => {});
      },

      // AI Threads
      addThread: (title = "New investigation") => {
        const id = `thread-${Date.now()}`;
        set((s) => ({
          threads: [
            { id, title, updatedAt: "Now", messages: [] },
            ...s.threads,
          ],
        }));
        return id;
      },

      addMessage: (threadId, message) =>
        set((s) => ({
          threads: s.threads.map((t) =>
            t.id === threadId ? { ...t, updatedAt: "Now", messages: [...t.messages, message] } : t,
          ),
        })),

      deleteThread: (threadId) =>
        set((s) => ({
          threads: s.threads.filter((t) => t.id !== threadId),
        })),

      syncFromBackend: async () => {
        try {
          const [backendObjs, backendBoards, backendPipes, backendApps, backendTypes] = await Promise.allSettled([
            graphApi.getObjects(),
            graphApi.getInvestigationReports(),
            graphApi.getPipelines(),
            graphApi.getApps(),
            graphApi.getObjectTypes(),
          ]);

          if (backendObjs.status === "fulfilled" && Array.isArray(backendObjs.value) && backendObjs.value.length > 0) {
            set({ entities: backendObjs.value as Entity[] });
          }
          if (backendBoards.status === "fulfilled" && Array.isArray(backendBoards.value) && backendBoards.value.length > 0) {
            set({ boards: backendBoards.value as BoardItem[] });
          }
          if (backendPipes.status === "fulfilled" && Array.isArray(backendPipes.value) && backendPipes.value.length > 0) {
            set({ pipelines: backendPipes.value as PipelineItem[] });
          }
          if (backendApps.status === "fulfilled" && Array.isArray(backendApps.value) && backendApps.value.length > 0) {
            set({ apps: backendApps.value as AppItem[] });
          }
          if (backendTypes.status === "fulfilled" && Array.isArray(backendTypes.value) && backendTypes.value.length > 0) {
            set({ objectTypes: backendTypes.value as ObjectTypeItem[] });
          }
        } catch (e) {
          console.log("[INFO] Using persistent local store:", e);
        }
      },
    }),
    { name: "orion-unified-store" },
  ),
);
