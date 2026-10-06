-- =====================================================
-- ORION INTELLIGENCE — Quick Setup Schema
-- Run this in: Supabase Dashboard > SQL Editor
-- Project: xdrrahewczsvastzssye
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- TENANTS
CREATE TABLE IF NOT EXISTS public.tenants (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(200) NOT NULL DEFAULT 'My Workspace',
  slug VARCHAR(63) NOT NULL DEFAULT 'default',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- PROFILES
CREATE TABLE IF NOT EXISTS public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT,
  avatar_url TEXT,
  email TEXT,
  role TEXT NOT NULL DEFAULT 'analyst',
  tenant_id UUID REFERENCES public.tenants(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auto-create profile on signup trigger
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name, role)
  VALUES (NEW.id, NEW.email, COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)), 'analyst')
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- OBJECT TYPES
CREATE TABLE IF NOT EXISTS public.object_types (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  icon VARCHAR(50) NOT NULL DEFAULT 'box',
  color VARCHAR(20) NOT NULL DEFAULT '#6366f1',
  is_system BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(tenant_id, name)
);

-- PROPERTY DEFINITIONS
CREATE TABLE IF NOT EXISTS public.property_definitions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  object_type_id UUID NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  data_type VARCHAR(50) NOT NULL DEFAULT 'string',
  is_required BOOLEAN NOT NULL DEFAULT false,
  is_unique BOOLEAN NOT NULL DEFAULT false,
  is_searchable BOOLEAN NOT NULL DEFAULT true,
  default_value TEXT,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- LINK TYPES
CREATE TABLE IF NOT EXISTS public.link_types (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  source_object_type_id UUID NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
  target_object_type_id UUID NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
  cardinality VARCHAR(20) NOT NULL DEFAULT 'many_to_many',
  is_directed BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- KNOWLEDGE OBJECTS
CREATE TABLE IF NOT EXISTS public.knowledge_objects (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  object_type_id UUID NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
  primary_label VARCHAR(500) NOT NULL,
  properties JSONB NOT NULL DEFAULT '{}',
  confidence NUMERIC(5,2) NOT NULL DEFAULT 100.0,
  status VARCHAR(20) NOT NULL DEFAULT 'Active',
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- KNOWLEDGE LINKS
CREATE TABLE IF NOT EXISTS public.knowledge_links (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  link_type_id UUID NOT NULL REFERENCES public.link_types(id) ON DELETE CASCADE,
  source_object_id UUID NOT NULL REFERENCES public.knowledge_objects(id) ON DELETE CASCADE,
  target_object_id UUID NOT NULL REFERENCES public.knowledge_objects(id) ON DELETE CASCADE,
  properties JSONB NOT NULL DEFAULT '{}',
  confidence NUMERIC(5,2) NOT NULL DEFAULT 100.0,
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- BOARDS
CREATE TABLE IF NOT EXISTS public.boards (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'Active',
  created_by UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- APPLICATIONS
CREATE TABLE IF NOT EXISTS public.applications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  slug VARCHAR(63) NOT NULL,
  icon VARCHAR(50) NOT NULL DEFAULT 'box',
  status VARCHAR(20) NOT NULL DEFAULT 'draft',
  config JSONB NOT NULL DEFAULT '{}',
  owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- INGESTION PIPELINES
CREATE TABLE IF NOT EXISTS public.ingestion_pipelines (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  source_type VARCHAR(50) NOT NULL,
  source_config JSONB NOT NULL DEFAULT '{}',
  mapping_config JSONB NOT NULL DEFAULT '{}',
  schedule TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'Paused',
  total_records INTEGER NOT NULL DEFAULT 0,
  last_run_at TIMESTAMPTZ,
  last_run_records INTEGER NOT NULL DEFAULT 0,
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- AI THREADS
CREATE TABLE IF NOT EXISTS public.ai_threads (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title VARCHAR(200) NOT NULL DEFAULT 'New Thread',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.object_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_objects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.boards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ingestion_pipelines ENABLE ROW LEVEL SECURITY;

CREATE POLICY "profiles_read" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "profiles_write_own" ON public.profiles FOR ALL USING (auth.uid() = id);

SELECT 'Orion schema ready!' AS result;
