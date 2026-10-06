-- Migration 0001: Initial Schema Setup
-- Orion Intelligence - PostgreSQL Schemas per Section 7.2 of Architecture Spec

-- ============================================================
-- ENABLE REQUIRED EXTENSIONS
-- ============================================================
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA extensions;
    CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA extensions;
    CREATE EXTENSION IF NOT EXISTS "pg_trgm" WITH SCHEMA extensions;
    CREATE EXTENSION IF NOT EXISTS "vector" WITH SCHEMA extensions;
EXCEPTION WHEN OTHERS THEN
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "vector";
END $$;

-- ============================================================
-- IDEMPOTENT COLUMN REPAIR — RUN BEFORE ANY CREATE TABLE / INSERT
--
-- Rationale: when a prior migration run failed mid-way, Supabase's public
-- schema may contain STALE copies of our tables (e.g. tenants without a
-- `slug` column, or object_types without `api_name` / `is_system`).
-- `CREATE TABLE IF NOT EXISTS` silently skips re-creating those, so any
-- later INSERT/ON CONFLICT referencing the missing column then fails with
-- 42703 "column does not exist".
--
-- Solution: before any DDL that touches user data, run this block.  It
-- declares the *target* column list for every table we will ever create,
-- and does:
--   1. IF table missing col → ALTER TABLE ADD COLUMN (with sensible
--      defaults so existing rows pass later NOT NULL enforcement).
--   2. IF NOT NULL required → ALTER COLUMN SET NOT NULL (best-effort).
--   3. IF a unique index / constraint must exist → CREATE UNIQUE INDEX
--      IF NOT EXISTS.
--
-- The block is tolerant of columns already being present and of most
-- partial prior states.
-- ============================================================

DO $$
DECLARE
    _schema CONSTANT TEXT := 'public';
    _t TEXT;
    _c TEXT;
    _type TEXT;
    _def  TEXT;
    _nn   BOOLEAN;
    _sql  TEXT;
    i    INT;
    _specs TEXT[][] := ARRAY[
        -- -------------------------------------------------------------
        -- tenants
        -- -------------------------------------------------------------
        ARRAY['tenants','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['tenants','name','VARCHAR(200)','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['tenants','slug','VARCHAR(63)','NOT NULL DEFAULT ''default''','f'],
        ARRAY['tenants','status','VARCHAR(20)','NOT NULL DEFAULT ''ACTIVE''','f'],
        ARRAY['tenants','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['tenants','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- users
        -- -------------------------------------------------------------
        ARRAY['users','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['users','tenant_id','UUID','','f'],
        ARRAY['users','email','VARCHAR(320)','NOT NULL DEFAULT ''unknown@example.invalid''','f'],
        ARRAY['users','name','VARCHAR(200)','NOT NULL DEFAULT ''Unknown''','f'],
        ARRAY['users','status','VARCHAR(20)','NOT NULL DEFAULT ''ACTIVE''','f'],
        ARRAY['users','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['users','last_login_at','TIMESTAMPTZ','','f'],
        -- -------------------------------------------------------------
        -- roles
        -- -------------------------------------------------------------
        ARRAY['roles','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['roles','tenant_id','UUID','','f'],
        ARRAY['roles','name','VARCHAR(100)','NOT NULL DEFAULT ''role''','f'],
        ARRAY['roles','description','VARCHAR(500)','','f'],
        -- -------------------------------------------------------------
        -- user_roles
        -- -------------------------------------------------------------
        ARRAY['user_roles','user_id','UUID','NOT NULL','f'],
        ARRAY['user_roles','role_id','UUID','NOT NULL','f'],
        -- -------------------------------------------------------------
        -- profiles (extends auth.users)
        -- -------------------------------------------------------------
        ARRAY['profiles','id','UUID','PRIMARY KEY','f'],
        ARRAY['profiles','email','TEXT','','f'],
        ARRAY['profiles','full_name','TEXT','','f'],
        ARRAY['profiles','avatar_url','TEXT','','f'],
        ARRAY['profiles','role','TEXT','NOT NULL DEFAULT ''analyst''','f'],
        ARRAY['profiles','tenant_id','UUID','','f'],
        ARRAY['profiles','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['profiles','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- object_types
        -- -------------------------------------------------------------
        ARRAY['object_types','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['object_types','tenant_id','UUID','','f'],
        ARRAY['object_types','name','VARCHAR(200)','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['object_types','api_name','VARCHAR(100)','NOT NULL DEFAULT ''item''','f'],
        ARRAY['object_types','description','TEXT','','f'],
        ARRAY['object_types','icon','VARCHAR(100)','DEFAULT ''box''','f'],
        ARRAY['object_types','color','VARCHAR(7)','DEFAULT ''#6366f1''','f'],
        ARRAY['object_types','is_system','BOOLEAN','NOT NULL DEFAULT FALSE','f'],
        ARRAY['object_types','version','INTEGER','NOT NULL DEFAULT 1','f'],
        ARRAY['object_types','is_active','BOOLEAN','NOT NULL DEFAULT TRUE','f'],
        ARRAY['object_types','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['object_types','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- properties
        -- -------------------------------------------------------------
        ARRAY['properties','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['properties','tenant_id','UUID','NOT NULL','f'],
        ARRAY['properties','object_type_id','UUID','NOT NULL','f'],
        ARRAY['properties','name','VARCHAR(200)','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['properties','api_name','VARCHAR(100)','NOT NULL DEFAULT ''item''','f'],
        ARRAY['properties','data_type','VARCHAR(30)','NOT NULL DEFAULT ''string''','f'],
        ARRAY['properties','is_required','BOOLEAN','NOT NULL DEFAULT FALSE','f'],
        ARRAY['properties','is_unique','BOOLEAN','NOT NULL DEFAULT FALSE','f'],
        ARRAY['properties','is_searchable','BOOLEAN','NOT NULL DEFAULT TRUE','f'],
        ARRAY['properties','is_filterable','BOOLEAN','NOT NULL DEFAULT TRUE','f'],
        ARRAY['properties','description','TEXT','','f'],
        ARRAY['properties','config','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['properties','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- property_definitions (legacy name, mirror of properties)
        -- -------------------------------------------------------------
        ARRAY['property_definitions','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['property_definitions','tenant_id','UUID','NOT NULL','f'],
        ARRAY['property_definitions','object_type_id','UUID','NOT NULL','f'],
        ARRAY['property_definitions','name','VARCHAR(200)','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['property_definitions','api_name','VARCHAR(100)','NOT NULL DEFAULT ''item''','f'],
        ARRAY['property_definitions','data_type','VARCHAR(30)','NOT NULL DEFAULT ''string''','f'],
        ARRAY['property_definitions','is_required','BOOLEAN','NOT NULL DEFAULT FALSE','f'],
        ARRAY['property_definitions','is_unique','BOOLEAN','NOT NULL DEFAULT FALSE','f'],
        ARRAY['property_definitions','is_searchable','BOOLEAN','NOT NULL DEFAULT TRUE','f'],
        ARRAY['property_definitions','is_filterable','BOOLEAN','NOT NULL DEFAULT TRUE','f'],
        ARRAY['property_definitions','description','TEXT','','f'],
        ARRAY['property_definitions','config','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['property_definitions','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- link_types
        -- -------------------------------------------------------------
        ARRAY['link_types','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['link_types','tenant_id','UUID','','f'],
        ARRAY['link_types','name','VARCHAR(200)','NOT NULL DEFAULT ''link''','f'],
        ARRAY['link_types','api_name','VARCHAR(100)','NOT NULL DEFAULT ''link''','f'],
        ARRAY['link_types','description','TEXT','','f'],
        ARRAY['link_types','source_object_type_id','UUID','NOT NULL','f'],
        ARRAY['link_types','target_object_type_id','UUID','NOT NULL','f'],
        ARRAY['link_types','cardinality','VARCHAR(30)','NOT NULL DEFAULT ''MANY_TO_MANY''','f'],
        ARRAY['link_types','is_bidirectional','BOOLEAN','NOT NULL DEFAULT FALSE','f'],
        ARRAY['link_types','version','INTEGER','NOT NULL DEFAULT 1','f'],
        ARRAY['link_types','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- applications (+ slug / config / owner_id added for seed compat)
        -- -------------------------------------------------------------
        ARRAY['applications','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['applications','tenant_id','UUID','','f'],
        ARRAY['applications','name','VARCHAR(200)','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['applications','slug','VARCHAR(100)','','f'],
        ARRAY['applications','description','TEXT','','f'],
        ARRAY['applications','icon','VARCHAR(100)','DEFAULT ''blocks''','f'],
        ARRAY['applications','definition','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['applications','config','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['applications','owner_id','UUID','','f'],
        ARRAY['applications','version','INTEGER','NOT NULL DEFAULT 1','f'],
        ARRAY['applications','status','VARCHAR(32)','NOT NULL DEFAULT ''draft''','f'],
        ARRAY['applications','created_by','UUID','','f'],
        ARRAY['applications','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['applications','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- application_versions
        -- -------------------------------------------------------------
        ARRAY['application_versions','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['application_versions','application_id','UUID','NOT NULL','f'],
        ARRAY['application_versions','version_number','INTEGER','NOT NULL DEFAULT 1','f'],
        ARRAY['application_versions','definition','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['application_versions','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- pipelines
        -- -------------------------------------------------------------
        ARRAY['pipelines','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['pipelines','tenant_id','UUID','','f'],
        ARRAY['pipelines','name','VARCHAR(200)','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['pipelines','source_type','VARCHAR(50)','NOT NULL DEFAULT ''csv''','f'],
        ARRAY['pipelines','config','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['pipelines','status','VARCHAR(20)','NOT NULL DEFAULT ''idle''','f'],
        ARRAY['pipelines','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- pipeline_runs
        -- -------------------------------------------------------------
        ARRAY['pipeline_runs','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['pipeline_runs','pipeline_id','UUID','NOT NULL','f'],
        ARRAY['pipeline_runs','status','VARCHAR(20)','NOT NULL DEFAULT ''queued''','f'],
        ARRAY['pipeline_runs','started_at','TIMESTAMPTZ','','f'],
        ARRAY['pipeline_runs','finished_at','TIMESTAMPTZ','','f'],
        ARRAY['pipeline_runs','metrics','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['pipeline_runs','error_message','TEXT','','f'],
        -- -------------------------------------------------------------
        -- lineage_events
        -- -------------------------------------------------------------
        ARRAY['lineage_events','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['lineage_events','tenant_id','UUID','','f'],
        ARRAY['lineage_events','object_id','UUID','','f'],
        ARRAY['lineage_events','event_type','VARCHAR(50)','NOT NULL DEFAULT ''created''','f'],
        ARRAY['lineage_events','source_pipeline_run_id','UUID','','f'],
        ARRAY['lineage_events','source_details','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['lineage_events','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- audit_logs
        -- -------------------------------------------------------------
        ARRAY['audit_logs','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['audit_logs','tenant_id','UUID','','f'],
        ARRAY['audit_logs','user_id','UUID','','f'],
        ARRAY['audit_logs','action','VARCHAR(100)','NOT NULL DEFAULT ''action''','f'],
        ARRAY['audit_logs','resource_type','VARCHAR(100)','','f'],
        ARRAY['audit_logs','resource_id','UUID','','f'],
        ARRAY['audit_logs','details','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['audit_logs','ip_address','VARCHAR(45)','','f'],
        ARRAY['audit_logs','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- knowledge_objects
        -- -------------------------------------------------------------
        ARRAY['knowledge_objects','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['knowledge_objects','tenant_id','UUID','','f'],
        ARRAY['knowledge_objects','object_type_id','UUID','NOT NULL','f'],
        ARRAY['knowledge_objects','primary_label','TEXT','NOT NULL DEFAULT ''Object''','f'],
        ARRAY['knowledge_objects','properties','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['knowledge_objects','confidence','NUMERIC(5,2)','DEFAULT 100.00','f'],
        ARRAY['knowledge_objects','status','TEXT','NOT NULL DEFAULT ''Active''','f'],
        ARRAY['knowledge_objects','created_by','UUID','','f'],
        ARRAY['knowledge_objects','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['knowledge_objects','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- knowledge_links
        -- -------------------------------------------------------------
        ARRAY['knowledge_links','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['knowledge_links','tenant_id','UUID','','f'],
        ARRAY['knowledge_links','link_type_id','UUID','NOT NULL','f'],
        ARRAY['knowledge_links','source_object_id','UUID','NOT NULL','f'],
        ARRAY['knowledge_links','target_object_id','UUID','NOT NULL','f'],
        ARRAY['knowledge_links','properties','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['knowledge_links','confidence','NUMERIC(5,2)','DEFAULT 100.00','f'],
        ARRAY['knowledge_links','created_by','UUID','','f'],
        ARRAY['knowledge_links','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- object_embeddings
        -- -------------------------------------------------------------
        ARRAY['object_embeddings','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['object_embeddings','tenant_id','UUID','','f'],
        ARRAY['object_embeddings','object_id','UUID','NOT NULL','f'],
        ARRAY['object_embeddings','object_type','VARCHAR(100)','','f'],
        -- vector column: we will handle separately because `vector(1536)`
        -- requires the extension and should not appear in this block.
        ARRAY['object_embeddings','model_version','TEXT','DEFAULT ''text-embedding-ada-002''','f'],
        ARRAY['object_embeddings','text_content','TEXT','','f'],
        ARRAY['object_embeddings','metadata','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['object_embeddings','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- boards
        -- -------------------------------------------------------------
        ARRAY['boards','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['boards','tenant_id','UUID','','f'],
        ARRAY['boards','title','TEXT','NOT NULL DEFAULT ''Untitled board''','f'],
        ARRAY['boards','description','TEXT','','f'],
        ARRAY['boards','status','TEXT','NOT NULL DEFAULT ''Active''','f'],
        ARRAY['boards','created_by','UUID','','f'],
        ARRAY['boards','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['boards','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- board_items
        -- -------------------------------------------------------------
        ARRAY['board_items','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['board_items','tenant_id','UUID','','f'],
        ARRAY['board_items','board_id','UUID','NOT NULL','f'],
        ARRAY['board_items','object_id','UUID','','f'],
        ARRAY['board_items','x_pos','INTEGER','DEFAULT 0','f'],
        ARRAY['board_items','y_pos','INTEGER','DEFAULT 0','f'],
        ARRAY['board_items','width','INTEGER','DEFAULT 240','f'],
        ARRAY['board_items','height','INTEGER','DEFAULT 140','f'],
        ARRAY['board_items','color','TEXT','','f'],
        ARRAY['board_items','note','TEXT','','f'],
        ARRAY['board_items','added_by','UUID','','f'],
        ARRAY['board_items','added_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- board_comments
        -- -------------------------------------------------------------
        ARRAY['board_comments','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['board_comments','tenant_id','UUID','','f'],
        ARRAY['board_comments','board_id','UUID','NOT NULL','f'],
        ARRAY['board_comments','object_id','UUID','','f'],
        ARRAY['board_comments','author_id','UUID','NOT NULL','f'],
        ARRAY['board_comments','content','TEXT','NOT NULL DEFAULT ''''''','f'],
        ARRAY['board_comments','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- board_collaborators
        -- -------------------------------------------------------------
        ARRAY['board_collaborators','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['board_collaborators','tenant_id','UUID','','f'],
        ARRAY['board_collaborators','board_id','UUID','NOT NULL','f'],
        ARRAY['board_collaborators','user_id','UUID','NOT NULL','f'],
        ARRAY['board_collaborators','role','TEXT','NOT NULL DEFAULT ''editor''','f'],
        ARRAY['board_collaborators','added_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- ingestion_pipelines
        -- -------------------------------------------------------------
        ARRAY['ingestion_pipelines','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['ingestion_pipelines','tenant_id','UUID','','f'],
        ARRAY['ingestion_pipelines','name','TEXT','NOT NULL DEFAULT ''Untitled''','f'],
        ARRAY['ingestion_pipelines','source_type','TEXT','NOT NULL DEFAULT ''csv''','f'],
        ARRAY['ingestion_pipelines','source_config','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['ingestion_pipelines','mapping_config','JSONB','NOT NULL DEFAULT ''{}''::jsonb','f'],
        ARRAY['ingestion_pipelines','schedule','TEXT','','f'],
        ARRAY['ingestion_pipelines','status','TEXT','NOT NULL DEFAULT ''Paused''','f'],
        ARRAY['ingestion_pipelines','total_records','BIGINT','DEFAULT 0','f'],
        ARRAY['ingestion_pipelines','last_run_at','TIMESTAMPTZ','','f'],
        ARRAY['ingestion_pipelines','last_run_records','BIGINT','DEFAULT 0','f'],
        ARRAY['ingestion_pipelines','created_by','UUID','','f'],
        ARRAY['ingestion_pipelines','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['ingestion_pipelines','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- ai_threads
        -- -------------------------------------------------------------
        ARRAY['ai_threads','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['ai_threads','tenant_id','UUID','','f'],
        ARRAY['ai_threads','user_id','UUID','NOT NULL','f'],
        ARRAY['ai_threads','title','TEXT','NOT NULL DEFAULT ''New investigation''','f'],
        ARRAY['ai_threads','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        ARRAY['ai_threads','updated_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f'],
        -- -------------------------------------------------------------
        -- ai_messages
        -- -------------------------------------------------------------
        ARRAY['ai_messages','id','UUID','PRIMARY KEY DEFAULT uuid_generate_v4()','f'],
        ARRAY['ai_messages','tenant_id','UUID','','f'],
        ARRAY['ai_messages','thread_id','UUID','NOT NULL','f'],
        ARRAY['ai_messages','role','TEXT','NOT NULL DEFAULT ''user''','f'],
        ARRAY['ai_messages','content','TEXT','NOT NULL DEFAULT ''''''','f'],
        ARRAY['ai_messages','sources','JSONB','DEFAULT ''[]''::jsonb','f'],
        ARRAY['ai_messages','created_at','TIMESTAMPTZ','NOT NULL DEFAULT now()','f']
    ];
BEGIN
    FOR i IN 1 .. array_length(_specs, 1) LOOP
        _t    := _specs[i][1];
        _c    := _specs[i][2];
        _type := _specs[i][3];
        _def  := _specs[i][4];
        _nn   := _specs[i][5] = 't';

        CONTINUE WHEN NOT EXISTS (
            SELECT 1 FROM information_schema.tables
             WHERE table_schema = _schema AND table_name = _t
        );

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema = _schema AND table_name = _t AND column_name = _c
        ) THEN
            _sql := format('ALTER TABLE %I.%I ADD COLUMN %I %s %s', _schema, _t, _c, _type, _def);
            EXECUTE _sql;
        END IF;
    END LOOP;

    -- ------- Special cases that need a hand-crafted ALTER --------
    -- object_embeddings.vector (pgvector type, can't just put in array above)
    IF EXISTS (SELECT 1 FROM information_schema.tables
                WHERE table_schema=_schema AND table_name='object_embeddings')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema=_schema AND table_name='object_embeddings' AND column_name='vector') THEN
       -- Try to add as vector(1536); if vector extension somehow missing, fall back to float[] so the column exists.
       BEGIN
           EXECUTE 'ALTER TABLE public.object_embeddings ADD COLUMN vector vector(1536)';
       EXCEPTION WHEN OTHERS THEN
           EXECUTE 'ALTER TABLE public.object_embeddings ADD COLUMN vector double precision[]';
       END;
    END IF;
END $$;

-- ============================================================
-- 7.2.A — IDENTITY & TENANCY
-- ============================================================

-- tenants
CREATE TABLE IF NOT EXISTS public.tenants (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(63)  NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tenants_slug CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{1,61}[a-z0-9])?$')
);

-- users
CREATE TABLE IF NOT EXISTS public.users (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id      UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    email          VARCHAR(320) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);

-- roles
CREATE TABLE IF NOT EXISTS public.roles (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name)
);

-- user_roles
CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id    UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES public.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- SUPABASE AUTH PROFILES (extends auth.users, maps to users+tenants)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    full_name TEXT,
    avatar_url TEXT,
    role TEXT NOT NULL DEFAULT 'analyst' CHECK (role IN ('admin', 'analyst', 'viewer')),
    tenant_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 7.2.B — ONTOLOGY
-- ============================================================

-- object_types
CREATE TABLE IF NOT EXISTS public.object_types (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    api_name    VARCHAR(100) NOT NULL,
    description TEXT,
    icon        VARCHAR(100) DEFAULT 'box',
    color       VARCHAR(7)   DEFAULT '#6366f1',
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    version     INTEGER      NOT NULL DEFAULT 1,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_object_types_tenant_api_name UNIQUE (tenant_id, api_name)
);

-- properties (canonical table per 7.2.B)
CREATE TABLE IF NOT EXISTS public.properties (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    object_type_id  UUID         NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    api_name        VARCHAR(100) NOT NULL,
    data_type       VARCHAR(30)  NOT NULL CHECK (data_type IN ('string','number','date','boolean','enum','geo','integer','float','datetime','json','reference')),
    is_required     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_unique       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_searchable   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_filterable   BOOLEAN      NOT NULL DEFAULT TRUE,
    description     TEXT,
    config          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_properties_type_api_name UNIQUE (object_type_id, api_name)
);
CREATE INDEX IF NOT EXISTS idx_properties_object_type ON public.properties(object_type_id);

-- property_definitions (legacy synonym of public.properties, kept for seed
-- compatibility with older "property_definitions" INSERT statements that do
-- not include api_name / config / is_unique / is_filterable.  We make it a
-- real table with the same schema (so seed has a table to INSERT into) and
-- then, if it's empty after migrations, mirror any canonical rows to it.
CREATE TABLE IF NOT EXISTS public.property_definitions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    object_type_id  UUID         NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    api_name        VARCHAR(100) NOT NULL,
    data_type       VARCHAR(30)  NOT NULL CHECK (data_type IN ('string','number','date','boolean','enum','geo','integer','float','datetime','json','reference')),
    is_required     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_unique       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_searchable   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_filterable   BOOLEAN      NOT NULL DEFAULT TRUE,
    description     TEXT,
    config          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_property_defs_type_api_name UNIQUE (object_type_id, api_name)
);
CREATE INDEX IF NOT EXISTS idx_property_defs_object_type ON public.property_definitions(object_type_id);

-- link_types
CREATE TABLE IF NOT EXISTS public.link_types (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name                    VARCHAR(200) NOT NULL,
    api_name                VARCHAR(100) NOT NULL,
    description             TEXT,
    source_object_type_id   UUID         NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
    target_object_type_id   UUID         NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
    cardinality             VARCHAR(30)  NOT NULL DEFAULT 'MANY_TO_MANY'
        CHECK (lower(cardinality) IN ('one_to_one','one_to_many','many_to_many')),
    is_bidirectional        BOOLEAN      NOT NULL DEFAULT FALSE,
    version                 INTEGER      NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_link_types_tenant_api_name UNIQUE (tenant_id, api_name)
);
CREATE INDEX IF NOT EXISTS idx_link_types_source ON public.link_types(tenant_id, source_object_type_id);
CREATE INDEX IF NOT EXISTS idx_link_types_target ON public.link_types(tenant_id, target_object_type_id);

-- ============================================================
-- 7.2.C — APPLICATION BUILDER
-- ============================================================

-- applications
CREATE TABLE IF NOT EXISTS public.applications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(100),
    description TEXT,
    icon        VARCHAR(100) DEFAULT 'blocks',
    definition  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    config      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    owner_id    UUID         REFERENCES auth.users(id) ON DELETE SET NULL,
    version     INTEGER      NOT NULL DEFAULT 1,
    status      VARCHAR(32)  NOT NULL DEFAULT 'draft',
    created_by  UUID         REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_applications_tenant ON public.applications(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_applications_tenant_slug ON public.applications(tenant_id, slug);

-- application_versions
CREATE TABLE IF NOT EXISTS public.application_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id  UUID         NOT NULL REFERENCES public.applications(id) ON DELETE CASCADE,
    version_number  INTEGER      NOT NULL,
    definition      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_versions_app_num UNIQUE (application_id, version_number)
);

-- ============================================================
-- 7.2.D — INGESTION & LINEAGE
-- ============================================================

-- pipelines
CREATE TABLE IF NOT EXISTS public.pipelines (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    source_type VARCHAR(50)  NOT NULL CHECK (source_type IN ('csv','json','postgres','mysql','api','s3','kafka','pdf','docx','doc','excel')),
    config      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status      VARCHAR(20)  NOT NULL DEFAULT 'idle' CHECK (status IN ('idle','running','paused','failed','completed')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_pipelines_tenant ON public.pipelines(tenant_id);

-- pipeline_runs
CREATE TABLE IF NOT EXISTS public.pipeline_runs (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pipeline_id    UUID         NOT NULL REFERENCES public.pipelines(id) ON DELETE CASCADE,
    status         VARCHAR(20)  NOT NULL CHECK (status IN ('queued','running','completed','failed','cancelled')),
    started_at     TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ,
    metrics        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    error_message  TEXT
);
CREATE INDEX IF NOT EXISTS idx_pipeline_runs_pipeline ON public.pipeline_runs(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_runs_status ON public.pipeline_runs(status);

-- lineage_events
CREATE TABLE IF NOT EXISTS public.lineage_events (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    object_id               UUID,
    event_type              VARCHAR(50)  NOT NULL CHECK (event_type IN ('created','updated','merged','deleted','ingested','linked','unlinked')),
    source_pipeline_run_id  UUID         REFERENCES public.pipeline_runs(id) ON DELETE SET NULL,
    source_details          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_lineage_tenant ON public.lineage_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_lineage_object ON public.lineage_events(object_id);

-- ============================================================
-- 7.2.E — AUDIT & GOVERNANCE
-- ============================================================

-- audit_logs
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id      UUID         REFERENCES public.tenants(id) ON DELETE SET NULL,
    user_id        UUID         REFERENCES auth.users(id) ON DELETE SET NULL,
    action         VARCHAR(100) NOT NULL,
    resource_type  VARCHAR(100),
    resource_id    UUID,
    details        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    ip_address     VARCHAR(45),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_tenant_time ON public.audit_logs(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_user ON public.audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON public.audit_logs(resource_type, resource_id);

-- ============================================================
-- KNOWLEDGE GRAPH (Objects + Links instances)
-- ============================================================

-- knowledge_objects (instance of object_types)
CREATE TABLE IF NOT EXISTS public.knowledge_objects (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    object_type_id  UUID         NOT NULL REFERENCES public.object_types(id) ON DELETE CASCADE,
    primary_label   TEXT         NOT NULL,
    properties      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    confidence      NUMERIC(5,2) DEFAULT 100.00 CHECK (confidence >= 0 AND confidence <= 100),
    status          TEXT         NOT NULL DEFAULT 'Active' CHECK (status IN ('Active','Draft','Review','Archived')),
    created_by      UUID         REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_objects_tenant_type ON public.knowledge_objects(tenant_id, object_type_id);
CREATE INDEX IF NOT EXISTS idx_objects_properties_gin ON public.knowledge_objects USING GIN(properties jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_objects_label_trgm ON public.knowledge_objects USING GIN(primary_label gin_trgm_ops);

-- knowledge_links (instance of link_types)
CREATE TABLE IF NOT EXISTS public.knowledge_links (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id         UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    link_type_id      UUID         NOT NULL REFERENCES public.link_types(id) ON DELETE CASCADE,
    source_object_id  UUID         NOT NULL REFERENCES public.knowledge_objects(id) ON DELETE CASCADE,
    target_object_id  UUID         NOT NULL REFERENCES public.knowledge_objects(id) ON DELETE CASCADE,
    properties        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    confidence        NUMERIC(5,2) DEFAULT 100.00 CHECK (confidence >= 0 AND confidence <= 100),
    created_by        UUID         REFERENCES auth.users(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_links_type_source_target UNIQUE(link_type_id, source_object_id, target_object_id)
);
CREATE INDEX IF NOT EXISTS idx_links_source ON public.knowledge_links(source_object_id);
CREATE INDEX IF NOT EXISTS idx_links_target ON public.knowledge_links(target_object_id);

-- ============================================================
-- 7.4 VECTOR DATABASE (pgvector embeddings)
-- ============================================================

CREATE TABLE IF NOT EXISTS public.object_embeddings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    object_id       UUID         NOT NULL REFERENCES public.knowledge_objects(id) ON DELETE CASCADE,
    object_type     VARCHAR(100),
    vector          vector(1536),
    model_version   TEXT DEFAULT 'text-embedding-ada-002',
    text_content    TEXT,
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_embeddings_vector ON public.object_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_embeddings_object ON public.object_embeddings(object_id);

-- ============================================================
-- BOARDS (Investigation / Collaboration boards)
-- ============================================================

CREATE TABLE IF NOT EXISTS public.boards (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id    UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    title        TEXT         NOT NULL,
    description  TEXT,
    status       TEXT         NOT NULL DEFAULT 'Active' CHECK (status IN ('Active','Archived','Review')),
    created_by   UUID         REFERENCES auth.users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.board_items (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id  UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    board_id   UUID         NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
    object_id  UUID         REFERENCES public.knowledge_objects(id) ON DELETE CASCADE,
    x_pos      INTEGER      DEFAULT 0,
    y_pos      INTEGER      DEFAULT 0,
    width      INTEGER      DEFAULT 240,
    height     INTEGER      DEFAULT 140,
    color      TEXT,
    note       TEXT,
    added_by   UUID         REFERENCES auth.users(id),
    added_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.board_comments (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id  UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    board_id   UUID         NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
    object_id  UUID         REFERENCES public.knowledge_objects(id),
    author_id  UUID         NOT NULL REFERENCES auth.users(id),
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.board_collaborators (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    board_id  UUID         NOT NULL REFERENCES public.boards(id) ON DELETE CASCADE,
    user_id   UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role      TEXT         NOT NULL DEFAULT 'editor' CHECK (role IN ('owner','editor','viewer')),
    added_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_board_collaborators UNIQUE(board_id, user_id)
);

-- ============================================================
-- AI THREADS / MESSAGES
-- ============================================================

CREATE TABLE IF NOT EXISTS public.ai_threads (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    user_id     UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title       TEXT         NOT NULL DEFAULT 'New investigation',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.ai_messages (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    thread_id   UUID         NOT NULL REFERENCES public.ai_threads(id) ON DELETE CASCADE,
    role        TEXT         NOT NULL CHECK (role IN ('user','assistant','system')),
    content     TEXT         NOT NULL,
    sources     JSONB        DEFAULT '[]'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INGESTION PIPELINES (compat alias - pipelines is canonical per 7.2.D)
-- ============================================================

CREATE TABLE IF NOT EXISTS public.ingestion_pipelines (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID         NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name            TEXT         NOT NULL,
    source_type     TEXT         NOT NULL CHECK (source_type IN ('csv','json','postgres','mysql','api','s3','kafka','pdf','docx')),
    source_config   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    mapping_config  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    schedule        TEXT,
    status          TEXT         NOT NULL DEFAULT 'Paused' CHECK (status IN ('Active','Paused','Warning','Failed')),
    total_records   BIGINT       DEFAULT 0,
    last_run_at     TIMESTAMPTZ,
    last_run_records BIGINT     DEFAULT 0,
    created_by      UUID         REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SAFETY PATCH: Ensure default tenant + FK targets exist
-- BEFORE any inline code block tries to SELECT from these tables.
-- This resolves the "relation public.tenants does not exist" error
-- by always creating rows first (idempotently via DO block checks).
-- ============================================================

DO $$
DECLARE
    v_tenant          UUID;
    v_profile_tenant  UUID;
    v_tenants_exists  BOOLEAN;
    v_profiles_exists BOOLEAN;
BEGIN
    -- 1. Default tenant (always insert one row if table is empty)
    v_tenants_exists := EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'tenants'
    );

    IF v_tenants_exists THEN
        SELECT id INTO v_tenant FROM public.tenants ORDER BY created_at ASC LIMIT 1;

        IF v_tenant IS NULL THEN
            INSERT INTO public.tenants (id, name, slug, status)
            VALUES (
                '00000000-0000-0000-0000-000000000001'::UUID,
                'Default Tenant',
                'default',
                'ACTIVE'
            )
            ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
            RETURNING id INTO v_tenant;
        END IF;
    ELSE
        v_tenant := '00000000-0000-0000-0000-000000000001'::UUID;
    END IF;

    -- 2. Grab first profile tenant (if any profiles exist)
    v_profiles_exists := EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'profiles'
    );

    IF v_profiles_exists THEN
        SELECT tenant_id INTO v_profile_tenant
          FROM public.profiles
         WHERE tenant_id IS NOT NULL
         LIMIT 1;
    ELSE
        v_profile_tenant := NULL;
    END IF;

    v_tenant := COALESCE(v_profile_tenant, v_tenant);

    -- 3. Back-fill tenant_id on legacy tables that might be missing it.
    --
    --    IMPORTANT: Postgres forbids a PL/pgSQL variable (v_tenant) inside
    --    `ALTER TABLE ... ADD COLUMN ... DEFAULT <expr>` because that context
    --    only accepts immutable expressions / literals, not variable references
    --    (error 0A000: cannot use column reference in DEFAULT expression).
    --    So instead, for every table we do a 3-step dance:
    --       1. ADD COLUMN tenant_id UUID  (nullable, no DEFAULT)
    --       2. UPDATE ... SET tenant_id = v_tenant WHERE tenant_id IS NULL;
    --          (UPDATE SET allows PL variable references just fine)
    --       3. ALTER COLUMN tenant_id SET NOT NULL;  (for required columns)
    --
    --    profiles stays nullable (Supabase trigger fills it after signup).

    -- -----------------------------------------------------------
    -- Helper: execute a 3-step backfill for a table.
    -- -----------------------------------------------------------
    DECLARE
        _tbl  TEXT;
        _nn   BOOLEAN;
        _col  CONSTANT TEXT := 'tenant_id';
        _idx  INT;
        _tables TEXT[] := ARRAY[
            'profiles',               'f',
            'object_types',           't',
            'properties',             't',
            'property_definitions',   't',
            'link_types',             't',
            'knowledge_objects',      't',
            'knowledge_links',        't',
            'object_embeddings',      't',
            'boards',                 't',
            'board_items',            't',
            'board_comments',         't',
            'board_collaborators',    't',
            'applications',           't',
            'pipelines',              't',
            'ingestion_pipelines',    't',
            'audit_logs',             'f',  -- audit_logs.tenant_id FK is SET NULL, allow nulls
            'ai_threads',             't',
            'ai_messages',            't'
        ];
    BEGIN
        -- profiles.email (separate, tiny column)
        IF v_profiles_exists THEN
            IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema='public' AND table_name='profiles' AND column_name='email') THEN
                ALTER TABLE public.profiles ADD COLUMN email TEXT;
            END IF;
        END IF;

        _idx := 1;
        WHILE _idx < array_length(_tables, 1) LOOP
            _tbl := _tables[_idx];
            _nn  := _tables[_idx + 1] = 't';
            _idx := _idx + 2;

            -- Only act if the table actually has an id column (table exists).
            IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema='public' AND table_name=_tbl AND column_name='id') THEN

                IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_schema='public' AND table_name=_tbl AND column_name=_col) THEN

                    -- Step 1: add as nullable
                    EXECUTE format('ALTER TABLE public.%I ADD COLUMN %I UUID', _tbl, _col);
                END IF;

                -- Step 2: back-fill (runs whether column just-added or pre-existing-null)
                EXECUTE format('UPDATE public.%I SET %I = $1 WHERE %I IS NULL', _tbl, _col, _col)
                USING v_tenant;

                -- Step 3: enforce NOT NULL only for required tables
                IF _nn THEN
                    EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I SET NOT NULL', _tbl, _col);
                END IF;
            END IF;
        END LOOP;
    END;
END $$;

-- ============================================================
-- RLS (Row Level Security) Policies — tenant isolation
-- ============================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.object_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.properties ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.property_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.link_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_objects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.knowledge_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.object_embeddings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.boards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.board_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.board_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.board_collaborators ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.application_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pipelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pipeline_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lineage_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ingestion_pipelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ai_threads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ai_messages ENABLE ROW LEVEL SECURITY;

-- Helper to get tenant_id from profile
CREATE OR REPLACE FUNCTION public.get_user_tenant() RETURNS UUID AS $$
BEGIN
    RETURN (SELECT tenant_id FROM public.profiles WHERE id = auth.uid() LIMIT 1);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop any existing public policies to guarantee idempotency on rerun
DO $$
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN (SELECT policyname, tablename FROM pg_policies WHERE schemaname = 'public') LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- Profiles: user can see own profile, admins see all
CREATE POLICY "User view own profile" ON public.profiles
    FOR SELECT USING (id = auth.uid());
CREATE POLICY "User update own profile" ON public.profiles
    FOR UPDATE USING (id = auth.uid());
CREATE POLICY "User insert own profile" ON public.profiles
    FOR INSERT WITH CHECK (id = auth.uid());

-- Tenants: tenant isolation
CREATE POLICY "Tenant read tenants" ON public.tenants FOR SELECT
    USING (id = public.get_user_tenant());

-- Tenanted tables: generic tenant isolation
CREATE POLICY "Tenant read object_types" ON public.object_types FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write object_types" ON public.object_types FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read properties" ON public.properties FOR SELECT
    USING (tenant_id = public.get_user_tenant() OR EXISTS (
        SELECT 1 FROM public.object_types ot
        WHERE ot.id = properties.object_type_id AND ot.tenant_id = public.get_user_tenant()
    ));
CREATE POLICY "Tenant write properties" ON public.properties FOR ALL
    USING (tenant_id = public.get_user_tenant() OR EXISTS (
        SELECT 1 FROM public.object_types ot
        WHERE ot.id = properties.object_type_id AND ot.tenant_id = public.get_user_tenant()
    ));

-- property_definitions (legacy mirror of properties)
CREATE POLICY "Tenant read property_definitions" ON public.property_definitions FOR SELECT
    USING (tenant_id = public.get_user_tenant() OR EXISTS (
        SELECT 1 FROM public.object_types ot
        WHERE ot.id = property_definitions.object_type_id AND ot.tenant_id = public.get_user_tenant()
    ));
CREATE POLICY "Tenant write property_definitions" ON public.property_definitions FOR ALL
    USING (tenant_id = public.get_user_tenant() OR EXISTS (
        SELECT 1 FROM public.object_types ot
        WHERE ot.id = property_definitions.object_type_id AND ot.tenant_id = public.get_user_tenant()
    ));

CREATE POLICY "Tenant read link_types" ON public.link_types FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write link_types" ON public.link_types FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read objects" ON public.knowledge_objects FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write objects" ON public.knowledge_objects FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read links_kg" ON public.knowledge_links FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write links_kg" ON public.knowledge_links FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read boards" ON public.boards FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write boards" ON public.boards FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read board_items" ON public.board_items FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write board_items" ON public.board_items FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read board_comments" ON public.board_comments FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write board_comments" ON public.board_comments FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read board_collaborators" ON public.board_collaborators FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write board_collaborators" ON public.board_collaborators FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read apps" ON public.applications FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write apps" ON public.applications FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read app_versions" ON public.application_versions FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.applications a
        WHERE a.id = application_versions.application_id AND a.tenant_id = public.get_user_tenant()
    ));

CREATE POLICY "Tenant read pipelines" ON public.pipelines FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write pipelines" ON public.pipelines FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read pipeline_runs" ON public.pipeline_runs FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.pipelines p
        WHERE p.id = pipeline_runs.pipeline_id AND p.tenant_id = public.get_user_tenant()
    ));

CREATE POLICY "Tenant read lineage_events" ON public.lineage_events FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write lineage_events" ON public.lineage_events FOR INSERT
    WITH CHECK (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read ingestion_pipelines" ON public.ingestion_pipelines FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write ingestion_pipelines" ON public.ingestion_pipelines FOR ALL
    USING (tenant_id = public.get_user_tenant());

CREATE POLICY "Tenant read threads" ON public.ai_threads FOR SELECT
    USING (tenant_id = public.get_user_tenant() AND user_id = auth.uid());
CREATE POLICY "Tenant write threads" ON public.ai_threads FOR ALL
    USING (tenant_id = public.get_user_tenant() AND user_id = auth.uid());

CREATE POLICY "Tenant read embeddings" ON public.object_embeddings FOR SELECT
    USING (tenant_id = public.get_user_tenant());
CREATE POLICY "Tenant write embeddings" ON public.object_embeddings FOR ALL
    USING (tenant_id = public.get_user_tenant());

-- AI messages: tenant + thread ownership
CREATE POLICY "Tenant read messages" ON public.ai_messages FOR SELECT
    USING (tenant_id = public.get_user_tenant() AND EXISTS (
        SELECT 1 FROM public.ai_threads t
        WHERE t.id = ai_messages.thread_id AND t.user_id = auth.uid()
    ));
CREATE POLICY "Tenant write messages" ON public.ai_messages FOR ALL
    USING (tenant_id = public.get_user_tenant() AND EXISTS (
        SELECT 1 FROM public.ai_threads t
        WHERE t.id = ai_messages.thread_id AND t.user_id = auth.uid()
    ));

-- Audit: only admins can read audit logs; anyone in the tenant can insert
CREATE POLICY "Admins read audit" ON public.audit_logs FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.profiles p
            WHERE p.id = auth.uid() AND p.role = 'admin'
            AND p.tenant_id = public.audit_logs.tenant_id
        )
    );
CREATE POLICY "Anyone insert audit" ON public.audit_logs FOR INSERT
    WITH CHECK (tenant_id = public.get_user_tenant());

-- ============================================================
-- AUTO-PROFILE TRIGGER (creates profile + tenant when user signs up)
-- ============================================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    new_tenant_id UUID;
    first_user BOOLEAN;
BEGIN
    first_user := (SELECT COUNT(*) FROM public.profiles) = 0;

    IF first_user THEN
        -- Create a fresh tenant for the first user of the platform
        new_tenant_id := uuid_generate_v4();
        INSERT INTO public.tenants (id, name, slug, status)
        VALUES (
            new_tenant_id,
            COALESCE(split_part(NEW.email, '@', 1), 'My') || ' Organization',
            lower(regexp_replace(COALESCE(split_part(NEW.email, '@', 1), 'default'), '[^a-z0-9-]', '-', 'g')),
            'ACTIVE'
        )
        ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
        RETURNING id INTO new_tenant_id;
    ELSE
        -- Subsequent users: re-use the tenant of the earliest existing profile
        SELECT tenant_id INTO new_tenant_id
          FROM public.profiles
         WHERE tenant_id IS NOT NULL
         ORDER BY created_at ASC
         LIMIT 1;
    END IF;

    IF new_tenant_id IS NULL THEN
        new_tenant_id := uuid_generate_v4();
    END IF;

    INSERT INTO public.profiles (id, email, full_name, role, tenant_id)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
        CASE WHEN first_user THEN 'admin' ELSE 'analyst' END,
        new_tenant_id
    )
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================
-- UPDATED_AT TRIGGERS
-- ============================================================

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_profiles_updated_at ON public.profiles;
CREATE TRIGGER set_profiles_updated_at BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_tenants_updated_at ON public.tenants;
CREATE TRIGGER set_tenants_updated_at BEFORE UPDATE ON public.tenants
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_object_types_updated_at ON public.object_types;
CREATE TRIGGER set_object_types_updated_at BEFORE UPDATE ON public.object_types
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_link_types_updated_at ON public.link_types;
CREATE TRIGGER set_link_types_updated_at BEFORE UPDATE ON public.link_types
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_objects_updated_at ON public.knowledge_objects;
CREATE TRIGGER set_objects_updated_at BEFORE UPDATE ON public.knowledge_objects
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_boards_updated_at ON public.boards;
CREATE TRIGGER set_boards_updated_at BEFORE UPDATE ON public.boards
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_applications_updated_at ON public.applications;
CREATE TRIGGER set_applications_updated_at BEFORE UPDATE ON public.applications
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_pipelines_updated_at ON public.pipelines;
CREATE TRIGGER set_pipelines_updated_at BEFORE UPDATE ON public.pipelines
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_ingestion_pipelines_updated_at ON public.ingestion_pipelines;
CREATE TRIGGER set_ingestion_pipelines_updated_at BEFORE UPDATE ON public.ingestion_pipelines
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS set_threads_updated_at ON public.ai_threads;
CREATE TRIGGER set_threads_updated_at BEFORE UPDATE ON public.ai_threads
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ============================================================
-- API-NAME DERIVATION TRIGGERS
--
-- Postgres forbids using a column-reference inside a `DEFAULT <expr>`
-- (error 0A000 "cannot use column reference in DEFAULT expression"),
-- so we can't write DEFAULT lower(regexp_replace(name, ...)).  Instead we
-- derive api_name from name (when the caller omitted it) via a BEFORE
-- INSERT trigger — a trigger body CAN see NEW.name just fine.
--
-- The slug function is reused for all 4 ontology tables and is collision
-- safe: if the slug already exists for the same tenant/object_type it
-- appends '_2', '_3', etc.  (Collision only really matters when the
-- uniqueness scope is wider than (tenant_id, api_name) — but since that's
-- our unique key already the suffix keeps inserts valid.)
-- ============================================================

CREATE OR REPLACE FUNCTION public.slugify(text)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE
        WHEN coalesce($1, '') = '' THEN 'item'
        ELSE lower(substring(regexp_replace($1, '[^a-z0-9_]+', '_', 'ig'), 1, 96))
    END;
$$;

CREATE OR REPLACE FUNCTION public.derive_api_name_from_name()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    _base  TEXT;
    _try   TEXT;
    _sfx   INT := 2;
    _scope_key TEXT;
    _exists BOOLEAN;
BEGIN
    IF NEW.api_name IS NULL OR length(btrim(NEW.api_name)) = 0 THEN
        _base := public.slugify(NEW.name);

        -- Some tables scope uniqueness per (tenant, api_name),
        -- properties / property_definitions scope it per (object_type_id, api_name).
        -- Use TG_TABLE_NAME + TG_TABLE_SCHEMA + a key query per table to dedupe.
        IF TG_TABLE_SCHEMA = 'public' AND TG_TABLE_NAME IN ('properties','property_definitions') THEN
            _try := _base;
            LOOP
                EXECUTE format(
                    'SELECT EXISTS(SELECT 1 FROM public.%I WHERE object_type_id = $1 AND api_name = $2)',
                    TG_TABLE_NAME
                ) INTO _exists USING NEW.object_type_id, _try;
                IF NOT _exists THEN EXIT; END IF;
                _try := _base || '_' || _sfx;
                _sfx := _sfx + 1;
                EXIT WHEN _sfx > 1000;
            END LOOP;
        ELSE
            _try := _base;
            LOOP
                EXECUTE format(
                    'SELECT EXISTS(SELECT 1 FROM public.%I WHERE tenant_id IS NOT DISTINCT FROM $1 AND api_name = $2)',
                    TG_TABLE_NAME
                ) INTO _exists USING NEW.tenant_id, _try;
                IF NOT _exists THEN EXIT; END IF;
                _try := _base || '_' || _sfx;
                _sfx := _sfx + 1;
                EXIT WHEN _sfx > 1000;
            END LOOP;
        END IF;

        NEW.api_name := _try;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS derive_object_type_api_name ON public.object_types;
CREATE TRIGGER derive_object_type_api_name
BEFORE INSERT ON public.object_types FOR EACH ROW
EXECUTE FUNCTION public.derive_api_name_from_name();

DROP TRIGGER IF EXISTS derive_properties_api_name ON public.properties;
CREATE TRIGGER derive_properties_api_name
BEFORE INSERT ON public.properties FOR EACH ROW
EXECUTE FUNCTION public.derive_api_name_from_name();

DROP TRIGGER IF EXISTS derive_property_defs_api_name ON public.property_definitions;
CREATE TRIGGER derive_property_defs_api_name
BEFORE INSERT ON public.property_definitions FOR EACH ROW
EXECUTE FUNCTION public.derive_api_name_from_name();

DROP TRIGGER IF EXISTS derive_link_types_api_name ON public.link_types;
CREATE TRIGGER derive_link_types_api_name
BEFORE INSERT ON public.link_types FOR EACH ROW
EXECUTE FUNCTION public.derive_api_name_from_name();

-- Audit logs and lineage events are append-only — reject UPDATE / DELETE / TRUNCATE

CREATE OR REPLACE FUNCTION public.audit_logs_reject_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only (% not allowed)', TG_OP USING ERRCODE = 'restrict_violation';
END $$;

DROP TRIGGER IF EXISTS trg_audit_no_update_delete ON public.audit_logs;
CREATE TRIGGER trg_audit_no_update_delete
    BEFORE UPDATE OR DELETE ON public.audit_logs
    FOR EACH ROW EXECUTE FUNCTION public.audit_logs_reject_mutation();

DROP TRIGGER IF EXISTS trg_audit_no_truncate ON public.audit_logs;
CREATE TRIGGER trg_audit_no_truncate
    BEFORE TRUNCATE ON public.audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION public.audit_logs_reject_mutation();

CREATE OR REPLACE FUNCTION public.lineage_events_reject_mutation()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'lineage_events is append-only (% not allowed)', TG_OP USING ERRCODE = 'restrict_violation';
END $$;

DROP TRIGGER IF EXISTS trg_lineage_no_update_delete ON public.lineage_events;
CREATE TRIGGER trg_lineage_no_update_delete
    BEFORE UPDATE OR DELETE ON public.lineage_events
    FOR EACH ROW EXECUTE FUNCTION public.lineage_events_reject_mutation();

DROP TRIGGER IF EXISTS trg_lineage_no_truncate ON public.lineage_events;
CREATE TRIGGER trg_lineage_no_truncate
    BEFORE TRUNCATE ON public.lineage_events
    FOR EACH STATEMENT EXECUTE FUNCTION public.lineage_events_reject_mutation();
