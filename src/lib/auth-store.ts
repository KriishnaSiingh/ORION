import { create } from "zustand";
import type { Session, User } from "@supabase/supabase-js";
import { getSupabase, isSupabaseConfigured } from "./supabase";

export { isSupabaseConfigured };

export type UserRole = "admin" | "analyst" | "viewer";

export type AuthProfile = {
  id: string;
  full_name: string | null;
  avatar_url: string | null;
  role: UserRole;
  tenant_id: string | null;
  email: string | undefined;
  initials: string;
  displayName: string;
};

export type AuthState = {
  session: Session | null;
  user: User | null;
  profile: AuthProfile | null;
  status: "idle" | "loading" | "authenticated" | "unauthenticated" | "error";
  error: string | null;
  initialized: boolean;
  initialize: () => Promise<void>;
  signIn: (email: string, password: string) => Promise<{ ok: boolean; error?: string }>;
  signUp: (
    fullName: string,
    email: string,
    password: string,
  ) => Promise<{ ok: boolean; error?: string; needsEmailConfirm?: boolean }>;
  signOut: () => Promise<{ ok: boolean; error?: string }>;
  refreshProfile: () => Promise<void>;
  clear: () => void;
};

function deriveInitials(name: string | null, email: string | undefined): string {
  if (name && name.trim().length > 0) {
    const parts = name.trim().split(/\s+/);
    const first = parts[0] ?? "";
    const last = parts[parts.length - 1] ?? "";
    if (parts.length >= 2 && first && last) {
      const fa = first[0] ?? "";
      const la = last[0] ?? "";
      return (fa + la).toUpperCase();
    }
    return first.slice(0, 2).toUpperCase();
  }
  if (email) return email.slice(0, 2).toUpperCase();
  return "??";
}

function profileFrom(
  user: User | null,
  raw: Record<string, unknown> | null,
): AuthProfile | null {
  if (!user) return null;
  const userMetadata = user.user_metadata as Record<string, unknown> | undefined;
  const appMetadata = user.app_metadata as Record<string, unknown> | undefined;
  const fullName = (raw?.["full_name"] as string | null) ?? (userMetadata?.["full_name"] as string | null) ?? null;
  const email = user.email;
  const displayName = fullName && fullName.trim() ? fullName : (email ?? "User");
  return {
    id: user.id,
    full_name: fullName,
    avatar_url: (raw?.["avatar_url"] as string | null) ?? (userMetadata?.["avatar_url"] as string | null) ?? null,
    role: ((raw?.["role"] as UserRole | undefined) ?? (userMetadata?.["role"] as UserRole | undefined) ?? "analyst"),
    tenant_id: (raw?.["tenant_id"] as string | null) ?? (appMetadata?.["tenant_id"] as string | null) ?? null,
    email,
    initials: deriveInitials(fullName, email),
    displayName,
  };
}

export const useAuth = create<AuthState>((set, get) => ({
  session: null,
  user: null,
  profile: null,
  status: "idle",
  error: null,
  initialized: false,

  clear: () => {
    set({ session: null, user: null, profile: null, status: "unauthenticated", error: null });
  },

  refreshProfile: async () => {
    const { user } = get();
    if (!user) return;
    if (!isSupabaseConfigured) return;
    try {
      const supabase = getSupabase();
      const { data, error } = await supabase
        .from("profiles")
        .select("full_name, avatar_url, role, tenant_id")
        .eq("id", user.id)
        .limit(1)
        .maybeSingle();
      if (error && !error.message.includes("does not exist")) {
        // ignore schema errors during dev
      }
      set({ profile: profileFrom(user, data ?? null) });
    } catch {
      set({ profile: profileFrom(user, null) });
    }
  },

  initialize: async () => {
    const { initialized } = get();
    if (initialized) return;
    set({ status: "loading" });

    if (!isSupabaseConfigured) {
      set({ initialized: true, status: "unauthenticated", error: "Supabase env vars not set" });
      return;
    }

    try {
      const supabase = getSupabase();
      const {
        data: { session },
      } = await supabase.auth.getSession();

      if (session?.user) {
        set({ session, user: session.user });
        await get().refreshProfile();
        set({ status: "authenticated", initialized: true, error: null });
      } else {
        set({ initialized: true, status: "unauthenticated" });
      }

      const {
        data: { subscription },
      } = supabase.auth.onAuthStateChange(async (_event, newSession) => {
        if (newSession?.user) {
          set({ session: newSession, user: newSession.user, status: "authenticated" });
          await get().refreshProfile();
        } else {
          get().clear();
        }
      });

      // Keep reference so it's not garbage collected
      (globalThis as unknown as { __orionAuthUnsub?: () => void }).__orionAuthUnsub =
        subscription.unsubscribe;
    } catch (e) {
      const message = e instanceof Error ? e.message : "Auth initialization failed";
      set({ initialized: true, status: "error", error: message });
    }
  },

  signIn: async (email, password) => {
    if (!isSupabaseConfigured) {
      return {
        ok: false,
        error:
          "Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in your .env.",
      };
    }
    set({ status: "loading", error: null });
    try {
      const supabase = getSupabase();
      const { data, error } = await supabase.auth.signInWithPassword({ email, password });
      if (error) throw error;
      if (data.user) {
        set({ session: data.session, user: data.user });
        await get().refreshProfile();
        set({ status: "authenticated" });
      }
      return { ok: true };
    } catch (e) {
      const message = e instanceof Error ? e.message : "Sign in failed";
      set({ status: "error", error: message });
      return { ok: false, error: message };
    }
  },

  signUp: async (fullName, email, password) => {
    if (!isSupabaseConfigured) {
      return {
        ok: false,
        error:
          "Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in your .env.",
      };
    }
    set({ status: "loading", error: null });
    try {
      const supabase = getSupabase();
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: {
          data: { full_name: fullName },
        },
      });
      if (error) throw error;
      if (data?.user && !data.session) {
        return { ok: true, needsEmailConfirm: true };
      }
      if (data?.session?.user) {
        set({ session: data.session, user: data.user });
        await get().refreshProfile();
        set({ status: "authenticated" });
      }
      return { ok: true };
    } catch (e) {
      const message = e instanceof Error ? e.message : "Sign up failed";
      set({ status: "error", error: message });
      return { ok: false, error: message };
    }
  },

  signOut: async () => {
    if (!isSupabaseConfigured) {
      get().clear();
      return { ok: true };
    }
    try {
      const supabase = getSupabase();
      const { error } = await supabase.auth.signOut();
      if (error) throw error;
      get().clear();
      return { ok: true };
    } catch (e) {
      get().clear();
      const message = e instanceof Error ? e.message : "Sign out failed";
      return { ok: false, error: message };
    }
  },
}));
