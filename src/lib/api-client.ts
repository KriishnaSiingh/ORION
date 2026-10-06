const API_BASE_URL = "http://localhost:8085/api/v1";
const AI_API_BASE_URL = "http://localhost:8002";

const getTenantId = (): string => {
  const token = localStorage.getItem("token");
  if (!token) return "00000000-0000-0000-0000-000000000001";
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return "00000000-0000-0000-0000-000000000001";
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    );
    const decoded = JSON.parse(jsonPayload) as { tenantId?: string; tenant_id?: string };
    return decoded.tenantId || decoded.tenant_id || "00000000-0000-0000-0000-000000000001";
  } catch (e) {
    return "00000000-0000-0000-0000-000000000001";
  }
};

export const apiClient = {
  async get<T = unknown>(endpoint: string, params?: Record<string, string>): Promise<T> {
    const url = new URL(`${API_BASE_URL}${endpoint}`);
    if (params) {
      Object.entries(params).forEach(([key, value]) => url.searchParams.append(key, value));
    }
    const token = localStorage.getItem("token");
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Tenant-ID": getTenantId(),
    };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const response = await fetch(url.toString(), { headers });
    if (!response.ok) throw new Error(`API Error: ${response.statusText}`);
    return response.json() as Promise<T>;
  },

  async post<T = unknown>(endpoint: string, body: unknown): Promise<T> {
    const token = localStorage.getItem("token");
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Tenant-ID": getTenantId(),
    };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    });
    if (!response.ok) throw new Error(`API Error: ${response.statusText}`);
    return response.json() as Promise<T>;
  },

  async put<T = unknown>(endpoint: string, body: unknown): Promise<T> {
    const token = localStorage.getItem("token");
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Tenant-ID": getTenantId(),
    };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: "PUT",
      headers,
      body: JSON.stringify(body),
    });
    if (!response.ok) throw new Error(`API Error: ${response.statusText}`);
    return response.json() as Promise<T>;
  },

  async patch<T = unknown>(endpoint: string, body: unknown): Promise<T> {
    const token = localStorage.getItem("token");
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Tenant-ID": getTenantId(),
    };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: "PATCH",
      headers,
      body: JSON.stringify(body),
    });
    if (!response.ok) throw new Error(`API Error: ${response.statusText}`);
    return response.json() as Promise<T>;
  },

  async delete<T = unknown>(endpoint: string): Promise<T> {
    const token = localStorage.getItem("token");
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "X-Tenant-ID": getTenantId(),
    };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: "DELETE",
      headers,
    });
    if (!response.ok && response.status !== 204) throw new Error(`API Error: ${response.statusText}`);
    if (response.status === 204) return {} as T;
    return response.json() as Promise<T>;
  },

  async logout() {
    localStorage.removeItem("token");
  },

  ai: {
    async chat(message: string, tenantId?: string) {
      const tid = tenantId || getTenantId();
      // Try direct FastAPI AI Orchestrator first
      try {
        const directResp = await fetch(`${AI_API_BASE_URL}/chat`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ message, tenant_id: tid }),
        });
        if (directResp.ok) {
          return await directResp.json();
        }
      } catch (err) {
        // Fall through to backend proxy
      }

      // Fallback through backend AI proxy
      return apiClient.post<{ answer: string; evidence?: { label: string; type: string }[]; plan?: string[] }>("/ai/chat", {
        message,
        tenant_id: tid,
      });
    },
  },
};

export const graphApi = {
  async getTemporalGraph(objectId: string, timestamp: number) {
    return apiClient.get(`/knowledge/objects/${objectId}/links`, {
      atTime: new Date(timestamp).toISOString(),
    });
  },

  async getAnalytics(metrics: ("influence" | "community")[]) {
    const endpoints = {
      influence: "/graph/analytics/pagerank",
      community: "/graph/analytics/communities",
    };
    const results: Record<string, unknown> = {};
    for (const metric of metrics) {
      try {
        results[metric] = await apiClient.post(endpoints[metric], {});
      } catch (e) {
        results[metric] = {};
      }
    }
    return results;
  },

  // Knowledge Objects
  async getObjects(params?: Record<string, string>) {
    return apiClient.get<unknown[]>("/knowledge/objects", params);
  },

  async searchKnowledge(query: string) {
    return apiClient.get<unknown[]>("/knowledge/search", { q: query });
  },

  async createObject(payload: unknown) {
    return apiClient.post("/knowledge/objects", payload);
  },

  async updateObject(id: string, payload: unknown) {
    return apiClient.patch(`/knowledge/objects/${id}`, payload);
  },

  async deleteObject(id: string) {
    return apiClient.delete(`/knowledge/objects/${id}`);
  },

  // Ontology
  async getObjectTypes() {
    return apiClient.get<unknown[]>("/ontology/object-types");
  },

  async createObjectType(payload: unknown) {
    return apiClient.post("/ontology/object-types", payload);
  },

  async addProperty(typeId: string, payload: unknown) {
    return apiClient.post(`/ontology/object-types/${typeId}/properties`, payload);
  },

  async getLinkTypes() {
    return apiClient.get<unknown[]>("/ontology/link-types");
  },

  async createLinkType(payload: unknown) {
    return apiClient.post("/ontology/link-types", payload);
  },

  // Ingestion Pipelines
  async getPipelines() {
    return apiClient.get<unknown[]>("/ingestion/pipelines");
  },

  async savePipeline(payload: unknown) {
    return apiClient.post("/ingestion/pipelines", payload);
  },

  async runPipeline(id: string) {
    return apiClient.post(`/ingestion/pipelines/${id}/run`, {});
  },

  // Investigation Boards
  async getInvestigationReports() {
    return apiClient.get<unknown[]>("/boards");
  },

  async createInvestigation(payload: unknown) {
    return apiClient.post("/boards", payload);
  },

  async addBoardComment(boardId: string, payload: { text: string; author?: string }) {
    return apiClient.post(`/boards/${boardId}/comments`, payload);
  },

  async shareBoard(boardId: string, userEmail: string) {
    return apiClient.post(`/boards/${boardId}/share`, { userEmail });
  },

  // Applications
  async getApps() {
    return apiClient.get<unknown[]>("/apps");
  },

  async createApp(payload: unknown) {
    return apiClient.post("/apps", payload);
  },

  async updateApp(appId: string, payload: unknown) {
    return apiClient.put(`/apps/${appId}`, payload);
  },

  // Profile & Settings
  async getProfile() {
    return apiClient.get("/settings/profile");
  },

  async updateProfile(payload: unknown) {
    return apiClient.post("/settings/profile", payload);
  },
};
