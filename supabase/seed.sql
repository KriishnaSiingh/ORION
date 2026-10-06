-- ============================================================
-- SEED DATA: Sample Ontology & Objects for Orion Intelligence
-- Run after running migrations
-- ============================================================

-- NOTE: First seed user must exist in auth.users (created via signup)
-- The first user to sign up automatically gets admin role via trigger.

-- We'll use a placeholder tenant_id that matches the first user's profile tenant.
-- Replace the tenant_id below with the actual value from profiles.tenant_id
-- after you create your first user.

-- For now, we'll set up a temporary reference; the actual seed uses dynamic lookup.

-- Ensure all tables and required columns exist before running seed
ALTER TABLE IF EXISTS public.object_types ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS public.object_types ADD COLUMN IF NOT EXISTS api_name VARCHAR(100);
ALTER TABLE IF EXISTS public.object_types ADD COLUMN IF NOT EXISTS icon VARCHAR(100) DEFAULT 'box';
ALTER TABLE IF EXISTS public.object_types ADD COLUMN IF NOT EXISTS color VARCHAR(7) DEFAULT '#6366f1';
ALTER TABLE IF EXISTS public.object_types ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE IF EXISTS public.object_types ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE IF EXISTS public.property_definitions ADD COLUMN IF NOT EXISTS api_name VARCHAR(100);
ALTER TABLE IF EXISTS public.property_definitions ADD COLUMN IF NOT EXISTS is_unique BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS public.property_definitions ADD COLUMN IF NOT EXISTS is_filterable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE IF EXISTS public.property_definitions ADD COLUMN IF NOT EXISTS config JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS public.applications ADD COLUMN IF NOT EXISTS slug VARCHAR(100);
ALTER TABLE IF EXISTS public.applications ADD COLUMN IF NOT EXISTS config JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE IF EXISTS public.applications ADD COLUMN IF NOT EXISTS definition JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE IF EXISTS public.applications ADD COLUMN IF NOT EXISTS owner_id UUID;
ALTER TABLE IF EXISTS public.applications ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'draft';

ALTER TABLE IF EXISTS public.ingestion_pipelines ADD COLUMN IF NOT EXISTS total_records BIGINT DEFAULT 0;
ALTER TABLE IF EXISTS public.ingestion_pipelines ADD COLUMN IF NOT EXISTS last_run_records BIGINT DEFAULT 0;

ALTER TABLE IF EXISTS public.link_types DROP CONSTRAINT IF EXISTS link_types_cardinality_check;
ALTER TABLE IF EXISTS public.link_types ADD CONSTRAINT link_types_cardinality_check CHECK (lower(cardinality) IN ('one_to_one','one_to_many','many_to_many'));

ALTER TABLE IF EXISTS public.applications DROP CONSTRAINT IF EXISTS applications_status_check;
ALTER TABLE IF EXISTS public.applications ADD CONSTRAINT applications_status_check CHECK (lower(status) IN ('draft', 'published', 'archived', 'operational', 'active'));

ALTER TABLE IF EXISTS public.boards DROP CONSTRAINT IF EXISTS boards_status_check;
ALTER TABLE IF EXISTS public.boards ADD CONSTRAINT boards_status_check CHECK (lower(status) IN ('active','archived','review','draft'));

ALTER TABLE IF EXISTS public.knowledge_objects DROP CONSTRAINT IF EXISTS knowledge_objects_status_check;
ALTER TABLE IF EXISTS public.knowledge_objects ADD CONSTRAINT knowledge_objects_status_check CHECK (lower(status) IN ('active','draft','review','archived'));

ALTER TABLE IF EXISTS public.ingestion_pipelines DROP CONSTRAINT IF EXISTS ingestion_pipelines_status_check;
ALTER TABLE IF EXISTS public.ingestion_pipelines ADD CONSTRAINT ingestion_pipelines_status_check CHECK (lower(status) IN ('active','paused','warning','failed','idle','running','completed'));

-- Allow seed data to be inserted before any user signs up
ALTER TABLE IF EXISTS public.boards ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE IF EXISTS public.knowledge_objects ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE IF EXISTS public.knowledge_links ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE IF EXISTS public.ingestion_pipelines ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE IF EXISTS public.applications ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE IF EXISTS public.applications ALTER COLUMN created_by DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_properties_type_api_name ON public.properties (object_type_id, api_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_property_defs_type_api_name ON public.property_definitions (object_type_id, api_name);

DO $$
DECLARE
    first_tenant UUID;
    admin_id UUID;
    org_type_id UUID;
    person_type_id UUID;
    location_type_id UUID;
    transaction_type_id UUID;
    investigation_type_id UUID;
    dataset_type_id UUID;
    document_type_id UUID;
    ceo_link_id UUID;
    works_at_link_id UUID;
    located_at_link_id UUID;
    involved_in_link_id UUID;
    northstar_id UUID;
    sable_id UUID;
    warehouse_id UUID;
    elena_id UUID;
BEGIN
    -- 1. Grab (or create) a real tenant row. Never rely on a random UUID that
    --    isn't in public.tenants — would FK-violate every tenanted insert.
    SELECT id INTO first_tenant FROM public.tenants ORDER BY created_at ASC LIMIT 1;
    IF first_tenant IS NULL THEN
        INSERT INTO public.tenants (id, name, slug, status)
        VALUES (
            '00000000-0000-0000-0000-000000000001'::UUID,
            'Default Tenant',
            'default',
            'ACTIVE'
        )
        ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
        RETURNING id INTO first_tenant;
    END IF;

    -- 2. Grab (or fake) an admin user id.
    --    Only use the real profile id; otherwise NULL, because columns like
    --    boards.created_by / applications.owner_id / knowledge_objects.created_by
    --    are all NULLable FKs and we prefer NULL over a random UUID that would
    --    FK-violate against auth.users.
    SELECT id INTO admin_id FROM public.profiles ORDER BY created_at ASC LIMIT 1;
    IF admin_id IS NULL THEN
        BEGIN
            SELECT id INTO admin_id FROM auth.users ORDER BY created_at ASC LIMIT 1;
        EXCEPTION WHEN OTHERS THEN
            admin_id := NULL;
        END;
    END IF;

    -- ============================================================
    -- OBJECT TYPES
    -- ============================================================
    INSERT INTO public.object_types (id, tenant_id, name, description, icon, color, is_system) VALUES
        (gen_random_uuid(), first_tenant, 'Organization', 'Companies, governments, NGOs', 'building-2', '#6366f1', FALSE),
        (gen_random_uuid(), first_tenant, 'Person', 'Individuals: employees, contacts, persons of interest', 'user-round', '#10b981', FALSE),
        (gen_random_uuid(), first_tenant, 'Location', 'Addresses, facilities, regions', 'map-pin', '#f59e0b', FALSE),
        (gen_random_uuid(), first_tenant, 'Transaction', 'Financial transactions, payments, invoices', 'receipt', '#ef4444', FALSE),
        (gen_random_uuid(), first_tenant, 'Investigation', 'Case files, investigations, signal reports', 'search', '#8b5cf6', FALSE),
        (gen_random_uuid(), first_tenant, 'Dataset', 'Data sources and raw dataset references', 'database', '#06b6d4', FALSE),
        (gen_random_uuid(), first_tenant, 'Document', 'PDF reports, contracts, images, emails', 'file-text', '#64748b', FALSE)
    ON CONFLICT DO NOTHING;

    SELECT id INTO org_type_id        FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Organization';
    SELECT id INTO person_type_id     FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Person';
    SELECT id INTO location_type_id   FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Location';
    SELECT id INTO transaction_type_id FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Transaction';
    SELECT id INTO investigation_type_id FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Investigation';
    SELECT id INTO dataset_type_id    FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Dataset';
    SELECT id INTO document_type_id   FROM public.object_types WHERE tenant_id = first_tenant AND name = 'Document';

    -- ============================================================
    -- PROPERTIES for Organization
    -- ============================================================
    INSERT INTO public.property_definitions (tenant_id, object_type_id, name, data_type, is_required, is_searchable, description) VALUES
        (first_tenant, org_type_id, 'registration_number', 'string', FALSE, TRUE, 'Legal registration identifier'),
        (first_tenant, org_type_id, 'jurisdiction', 'string', FALSE, TRUE, 'Country of registration'),
        (first_tenant, org_type_id, 'industry', 'string', FALSE, TRUE, 'Industry sector'),
        (first_tenant, org_type_id, 'founded_year', 'integer', FALSE, TRUE, 'Year founded')
    ON CONFLICT DO NOTHING;

    -- ============================================================
    -- PROPERTIES for Person
    -- ============================================================
    INSERT INTO public.property_definitions (tenant_id, object_type_id, name, data_type, is_required, is_searchable, description) VALUES
        (first_tenant, person_type_id, 'email', 'string', FALSE, TRUE, 'Work email'),
        (first_tenant, person_type_id, 'phone', 'string', FALSE, TRUE, 'Phone number'),
        (first_tenant, person_type_id, 'date_of_birth', 'date', FALSE, FALSE, 'Date of birth'),
        (first_tenant, person_type_id, 'nationality', 'string', FALSE, TRUE, 'Nationality')
    ON CONFLICT DO NOTHING;

    -- ============================================================
    -- LINK TYPES
    -- ============================================================
    INSERT INTO public.link_types (id, tenant_id, name, description, source_object_type_id, target_object_type_id, cardinality) VALUES
        (gen_random_uuid(), first_tenant, 'CEO_OF', 'Person is CEO of an Organization', person_type_id, org_type_id, 'ONE_TO_MANY'),
        (gen_random_uuid(), first_tenant, 'WORKS_AT', 'Person works at Organization', person_type_id, org_type_id, 'MANY_TO_MANY'),
        (gen_random_uuid(), first_tenant, 'LOCATED_AT', 'Organization or Person located at a Location', org_type_id, location_type_id, 'MANY_TO_MANY'),
        (gen_random_uuid(), first_tenant, 'INVOLVED_IN', 'Entity involved in an Investigation', org_type_id, investigation_type_id, 'MANY_TO_MANY')
    ON CONFLICT DO NOTHING;

    SELECT id INTO ceo_link_id       FROM public.link_types WHERE tenant_id = first_tenant AND name = 'CEO_OF';
    SELECT id INTO works_at_link_id  FROM public.link_types WHERE tenant_id = first_tenant AND name = 'WORKS_AT';
    SELECT id INTO located_at_link_id FROM public.link_types WHERE tenant_id = first_tenant AND name = 'LOCATED_AT';
    SELECT id INTO involved_in_link_id FROM public.link_types WHERE tenant_id = first_tenant AND name = 'INVOLVED_IN';

    -- ============================================================
    -- SAMPLE KNOWLEDGE OBJECTS
    -- ============================================================
    INSERT INTO public.knowledge_objects (id, tenant_id, object_type_id, primary_label, properties, confidence, status, created_by) VALUES
        (gen_random_uuid(), first_tenant, org_type_id, 'Northstar Logistics',
            '{"registration_number":"NL-RD-8821","jurisdiction":"Netherlands","industry":"Logistics","founded_year":2004}'::jsonb,
            98.0, 'Active', admin_id),
        (gen_random_uuid(), first_tenant, investigation_type_id, 'Project Sable',
            '{"priority":"high","classification":"confidential"}'::jsonb,
            92.0, 'Review', admin_id),
        (gen_random_uuid(), first_tenant, location_type_id, 'Warehouse D-17',
            '{"address":"Havenweg 41, Rotterdam","country":"NL","type":"Cold Storage"}'::jsonb,
            96.0, 'Active', admin_id),
        (gen_random_uuid(), first_tenant, person_type_id, 'Elena Vasquez',
            '{"email":"elena.vasquez@northstar.log","phone":"+31 10 123 4567","nationality":"Spain"}'::jsonb,
            87.0, 'Active', admin_id),
        (gen_random_uuid(), first_tenant, dataset_type_id, 'Q4 Vendor Ledger',
            '{"source":"SAP","period":"Q4-2024","records":128429}'::jsonb,
            99.0, 'Active', admin_id),
        (gen_random_uuid(), first_tenant, document_type_id, 'Signal Report 228',
            '{"report_date":"2025-09-12","pages":38,"document_type":"Risk Assessment"}'::jsonb,
            78.0, 'Review', admin_id)
    ON CONFLICT DO NOTHING;

    SELECT id INTO northstar_id  FROM public.knowledge_objects WHERE tenant_id = first_tenant AND primary_label = 'Northstar Logistics';
    SELECT id INTO sable_id      FROM public.knowledge_objects WHERE tenant_id = first_tenant AND primary_label = 'Project Sable';
    SELECT id INTO warehouse_id  FROM public.knowledge_objects WHERE tenant_id = first_tenant AND primary_label = 'Warehouse D-17';
    SELECT id INTO elena_id      FROM public.knowledge_objects WHERE tenant_id = first_tenant AND primary_label = 'Elena Vasquez';

    -- ============================================================
    -- SAMPLE KNOWLEDGE LINKS
    -- ============================================================
    INSERT INTO public.knowledge_links (tenant_id, link_type_id, source_object_id, target_object_id, properties, confidence, created_by) VALUES
        (first_tenant, works_at_link_id, elena_id, northstar_id, '{"role":"Regional Procurement Lead","since":"2021-06"}'::jsonb, 95.0, admin_id),
        (first_tenant, located_at_link_id, northstar_id, warehouse_id, '{"operational":"primary","volume_share":0.62}'::jsonb, 97.0, admin_id),
        (first_tenant, involved_in_link_id, northstar_id, sable_id, '{"role":"Target Entity","flagged":true}'::jsonb, 90.0, admin_id)
    ON CONFLICT DO NOTHING;

    -- ============================================================
    -- SAMPLE INGESTION PIPELINES
    -- ============================================================
    INSERT INTO public.ingestion_pipelines (tenant_id, name, source_type, source_config, schedule, status, total_records, last_run_records, created_by) VALUES
        (first_tenant, 'ERP — SAP S/4HANA', 'postgres', '{"host":"sapprd.internal","port":5432,"database":"erp"}'::jsonb, 'Every 15 min', 'Active', 18400000, 18429, admin_id),
        (first_tenant, 'Vendor Risk Feed', 's3', '{"bucket":"risk-feeds","prefix":"vendor/parquet"}'::jsonb, 'Hourly', 'Active', 2800000, 41200, admin_id),
        (first_tenant, 'Identity Resolution', 'kafka', '{"brokers":"kafka.internal:9092","topic":"identity.events"}'::jsonb, 'Streaming', 'Active', 928000, 1247, admin_id),
        (first_tenant, 'Legacy CRM', 'mysql', '{"host":"crm-legacy.internal","database":"crm"}'::jsonb, 'Daily', 'Warning', 4100000, 0, admin_id)
    ON CONFLICT DO NOTHING;

    -- ============================================================
    -- SAMPLE APPLICATIONS
    -- ============================================================
    INSERT INTO public.applications (tenant_id, name, description, slug, icon, status, config, owner_id) VALUES
        (first_tenant, 'Supplier Command Center', 'Monitor vendor health, exposure, and delivery risk', 'supplier-command', 'shield', 'Operational', '{"widgets":12,"views":["overview","risk","map"]}'::jsonb, admin_id),
        (first_tenant, 'Transaction Review', 'Triage anomalous payments with linked evidence', 'tx-review', 'receipt', 'Draft', '{"widgets":8,"views":["queue","detail"]}'::jsonb, admin_id),
        (first_tenant, 'Executive Network View', 'Explore leadership and beneficial ownership', 'exec-network', 'network', 'Operational', '{"widgets":6,"views":["graph","profile"]}'::jsonb, admin_id)
    ON CONFLICT DO NOTHING;

    -- ============================================================
    -- SAMPLE BOARDS
    -- ============================================================
    INSERT INTO public.boards (tenant_id, title, description, status, created_by) VALUES
        (first_tenant, 'Operation Northstar', 'Supplier concentration and geopolitical exposure', 'Active', admin_id),
        (first_tenant, 'Sable Transaction Review', 'Payments, beneficial ownership, and linked entities', 'Review', admin_id),
        (first_tenant, 'EMEA Supply Resilience', 'Alternative routes and critical dependencies', 'Active', admin_id)
    ON CONFLICT DO NOTHING;

    -- ============================================================
    -- SYNC: property_definitions (seeded above, legacy name)
    -- <-> canonical properties table.  Keep both in sync so the app's
    -- existing queries against either name return the same rows.
    -- ============================================================
    INSERT INTO public.properties
        (id, tenant_id, object_type_id, name, api_name, data_type,
         is_required, is_unique, is_searchable, is_filterable, description, config, created_at)
    SELECT
        d.id, d.tenant_id, d.object_type_id, d.name, d.api_name, d.data_type,
        d.is_required, d.is_unique, d.is_searchable, d.is_filterable,
        d.description, d.config, coalesce(d.created_at, now())
    FROM public.property_definitions d
    ON CONFLICT (object_type_id, api_name) DO NOTHING;

    -- Also back-fill anything that already exists in properties -> legacy
    INSERT INTO public.property_definitions
        (id, tenant_id, object_type_id, name, api_name, data_type,
         is_required, is_unique, is_searchable, is_filterable, description, config, created_at)
    SELECT
        p.id, p.tenant_id, p.object_type_id, p.name, p.api_name, p.data_type,
        p.is_required, p.is_unique, p.is_searchable, p.is_filterable,
        p.description, p.config, coalesce(p.created_at, now())
    FROM public.properties p
    ON CONFLICT (object_type_id, api_name) DO NOTHING;
END $$;
