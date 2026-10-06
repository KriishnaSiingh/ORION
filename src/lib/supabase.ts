import { createClient, type SupabaseClient } from "@supabase/supabase-js";

const SUPABASE_URL = import.meta.env["VITE_SUPABASE_URL"] as string | undefined;
const SUPABASE_ANON_KEY = import.meta.env["VITE_SUPABASE_ANON_KEY"] as string | undefined;

export const isSupabaseConfigured = Boolean(SUPABASE_URL && SUPABASE_ANON_KEY);

let singletonClient: SupabaseClient | null = null;

export function getSupabase(): SupabaseClient {
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
    throw new Error(
      "Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in your .env file.",
    );
  }
  if (typeof window === "undefined") {
    return createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      auth: { persistSession: false },
    });
  }
  if (!singletonClient) {
    singletonClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    });
  }
  return singletonClient;
}

export type SupabaseDatabase = {
  public: {
    Tables: {
      profiles: {
        Row: {
          id: string;
          full_name: string | null;
          avatar_url: string | null;
          role: string;
          tenant_id: string | null;
          created_at: string;
          updated_at: string;
        };
        Insert: {
          id: string;
          full_name?: string | null;
          avatar_url?: string | null;
          role?: string;
          tenant_id?: string | null;
          created_at?: string;
          updated_at?: string;
        };
        Update: {
          id?: string;
          full_name?: string | null;
          avatar_url?: string | null;
          role?: string;
          tenant_id?: string | null;
          created_at?: string;
          updated_at?: string;
        };
      };
      object_types: {
        Row: {
          id: string;
          tenant_id: string;
          name: string;
          description: string | null;
          icon: string;
          color: string;
          is_system: boolean;
          created_at: string;
          updated_at: string;
        };
      };
      link_types: {
        Row: {
          id: string;
          tenant_id: string;
          name: string;
          description: string | null;
          source_object_type_id: string;
          target_object_type_id: string;
          cardinality: string;
          is_directed: boolean;
          created_at: string;
          updated_at: string;
        };
      };
      knowledge_objects: {
        Row: {
          id: string;
          tenant_id: string;
          object_type_id: string;
          primary_label: string;
          properties: Record<string, unknown>;
          confidence: number;
          status: string;
          created_by: string | null;
          created_at: string;
          updated_at: string;
        };
      };
      knowledge_links: {
        Row: {
          id: string;
          tenant_id: string;
          link_type_id: string;
          source_object_id: string;
          target_object_id: string;
          properties: Record<string, unknown>;
          confidence: number;
          created_by: string | null;
          created_at: string;
        };
      };
      boards: {
        Row: {
          id: string;
          tenant_id: string;
          title: string;
          description: string | null;
          status: string;
          created_by: string;
          created_at: string;
          updated_at: string;
        };
      };
      applications: {
        Row: {
          id: string;
          tenant_id: string;
          name: string;
          description: string | null;
          slug: string;
          icon: string;
          status: string;
          config: Record<string, unknown>;
          owner_id: string;
          created_at: string;
          updated_at: string;
        };
      };
      ingestion_pipelines: {
        Row: {
          id: string;
          tenant_id: string;
          name: string;
          source_type: string;
          source_config: Record<string, unknown>;
          mapping_config: Record<string, unknown>;
          schedule: string | null;
          status: string;
          total_records: number;
          last_run_at: string | null;
          last_run_records: number;
          created_by: string | null;
          created_at: string;
          updated_at: string;
        };
      };
      ai_threads: {
        Row: {
          id: string;
          tenant_id: string;
          user_id: string;
          title: string;
          created_at: string;
          updated_at: string;
        };
      };
    };
  };
};
