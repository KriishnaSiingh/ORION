require("dotenv").config();
const express = require("express");
const cors = require("cors");
const { createClient } = require("@supabase/supabase-js");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
const { v4: uuidv4 } = require("uuid");
const fs = require("fs");
const path = require("path");

const app = express();

const SUPABASE_URL = process.env.SUPABASE_URL || "https://xdrrahewczsvastzssye.supabase.co";
const SUPABASE_ANON_KEY =
  process.env.SUPABASE_ANON_KEY || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhkcnJhaGV3Y3pzdmFzdHpzc3llIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAwNDQ2NDIsImV4cCI6MjEwNTYyMDY0Mn0.eVXAuVW-MElF174qRJEua4Po1myvW-G_nAnx82qVg-M";
const JWT_SECRET = process.env.JWT_SECRET || "dev-only-secret-change-me-min-32-bytes-long-please";
const JWT_ISSUER = "orion-platform";
const PORT = 8085;

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const DATA_FILE = path.join(__dirname, "data-store.json");

// Default initial state
function getSeedData() {
  return {
    users: [
      {
        id: "ba557217-742e-4955-9530-bbcb0ebb4482",
        email: "krishnasingh15kks@gmail.com",
        name: "Krishna Singh",
        role: "admin",
        passwordHash: "$2a$10$wT8mQ3p0d2iK.2N8n5q2rOGv2v3qT1Y3kR.6tS0E5F.PZ2Y3jH1.2",
        tenantId: "00000000-0000-0000-0000-000000000001",
        mfa: false,
        summary: true,
      },
    ],
    object_types: [
      { id: "ot-1", name: "Organization", description: "Companies, subsidiaries, vendors, and partners", icon: "Building2", properties: 24, links: 18, objects: "48.2K", updated: "Today", property_definitions: [
        { id: "pd-1", name: "Legal name", data_type: "string", is_required: true },
        { id: "pd-2", name: "External ID", data_type: "string", is_required: true },
        { id: "pd-3", name: "Jurisdiction", data_type: "string", is_required: false },
        { id: "pd-4", name: "Risk score", data_type: "number", is_required: false },
        { id: "pd-5", name: "Last verified", data_type: "string", is_required: false },
      ]},
      { id: "ot-2", name: "Person", description: "Beneficial owners, officers, employees, contacts", icon: "UserRound", properties: 19, links: 14, objects: "182K", updated: "Today", property_definitions: [
        { id: "pd-6", name: "Full name", data_type: "string", is_required: true },
        { id: "pd-7", name: "Nationality", data_type: "string", is_required: false },
        { id: "pd-8", name: "PEP status", data_type: "boolean", is_required: false },
      ]},
      { id: "ot-3", name: "Location", description: "Facilities, hubs, ports, registered addresses", icon: "Database", properties: 16, links: 21, objects: "12.9K", updated: "Yesterday", property_definitions: [
        { id: "pd-9", name: "Facility name", data_type: "string", is_required: true },
        { id: "pd-10", name: "Country", data_type: "string", is_required: true },
        { id: "pd-11", name: "Capacity TEU", data_type: "number", is_required: false },
      ]},
      { id: "ot-4", name: "Transaction", description: "Invoices, wire transfers, shipments, customs filings", icon: "FileText", properties: 31, links: 11, objects: "4.8M", updated: "Sep 17", property_definitions: [
        { id: "pd-12", name: "Amount", data_type: "number", is_required: true },
        { id: "pd-13", name: "Currency", data_type: "string", is_required: true },
        { id: "pd-14", name: "Reference", data_type: "string", is_required: false },
      ]},
      { id: "ot-5", name: "Asset", description: "Vessels, IP patents, hardware equipment, contracts", icon: "Shield", properties: 22, links: 17, objects: "86.4K", updated: "Sep 15", property_definitions: [
        { id: "pd-15", name: "Asset code", data_type: "string", is_required: true },
        { id: "pd-16", name: "Valuation", data_type: "number", is_required: false },
      ]},
    ],
    link_types: [
      { id: "lt-1", name: "OWNS", description: "Beneficial or direct equity ownership", cardinality: "many_to_many", is_directed: true },
      { id: "lt-2", name: "EMPLOYS", description: "Employment or board affiliation", cardinality: "one_to_many", is_directed: true },
      { id: "lt-3", name: "LOCATED_AT", description: "Physical hub or operating presence", cardinality: "many_to_one", is_directed: true },
      { id: "lt-4", name: "SUPPLIES", description: "Commercial supply relationship", cardinality: "many_to_many", is_directed: true },
      { id: "lt-5", name: "REFERENCES", description: "Evidence or citation link", cardinality: "many_to_many", is_directed: true },
      { id: "lt-6", name: "PART_OF", description: "Hierarchy or corporate group member", cardinality: "many_to_one", is_directed: true },
    ],
    knowledge_objects: [
      {
        id: "OBJ-4921",
        name: "Northstar Logistics",
        primary_label: "Northstar Logistics",
        type: "Organization",
        object_type_id: "ot-1",
        description: "Strategic distribution partner in EMEA",
        owner: "Maya Chen",
        updated: "4 min ago",
        confidence: 98,
        status: "Active",
        created_at: new Date(Date.now() - 4 * 60000).toISOString(),
        properties: { "Legal name": "Northstar Logistics BV", "External ID": "NL-99281", "Jurisdiction": "Netherlands", "Risk score": 42 }
      },
      {
        id: "OBJ-4918",
        name: "Project Sable",
        primary_label: "Project Sable",
        type: "Investigation",
        object_type_id: "ot-4",
        description: "Cross-border supply chain anomaly",
        owner: "Alex Rivera",
        updated: "18 min ago",
        confidence: 92,
        status: "Review",
        created_at: new Date(Date.now() - 18 * 60000).toISOString(),
        properties: { "Amount": 14200000, "Currency": "EUR", "Reference": "SABLE-2024-Q3" }
      },
      {
        id: "OBJ-4907",
        name: "Warehouse D-17",
        primary_label: "Warehouse D-17",
        type: "Location",
        object_type_id: "ot-3",
        description: "Rotterdam fulfillment and cold storage hub",
        owner: "Operations",
        updated: "43 min ago",
        confidence: 96,
        status: "Operational",
        created_at: new Date(Date.now() - 43 * 60000).toISOString(),
        properties: { "Facility name": "Warehouse D-17 Hub", "Country": "Netherlands", "Capacity TEU": 150000 }
      },
      {
        id: "OBJ-4884",
        name: "Elena Vasquez",
        primary_label: "Elena Vasquez",
        type: "Person",
        object_type_id: "ot-2",
        description: "Regional procurement lead",
        owner: "People Ops",
        updated: "2 hr ago",
        confidence: 87,
        status: "Active",
        created_at: new Date(Date.now() - 120 * 60000).toISOString(),
        properties: { "Full name": "Elena Vasquez", "Nationality": "Spain", "PEP status": false }
      },
      {
        id: "OBJ-4862",
        name: "Q4 Vendor Ledger",
        primary_label: "Q4 Vendor Ledger",
        type: "Dataset",
        object_type_id: "ot-4",
        description: "Consolidated procurement transactions",
        owner: "Finance",
        updated: "5 hr ago",
        confidence: 99,
        status: "Operational",
        created_at: new Date(Date.now() - 300 * 60000).toISOString(),
        properties: { "Reference": "LEDGER-Q4-2024", "Amount": 48200000, "Currency": "USD" }
      },
      {
        id: "OBJ-4821",
        name: "Signal Report 228",
        primary_label: "Signal Report 228",
        type: "Document",
        object_type_id: "ot-4",
        description: "Third-party risk assessment",
        owner: "Risk",
        updated: "Yesterday",
        confidence: 78,
        status: "Warning",
        created_at: new Date(Date.now() - 86400000).toISOString(),
        properties: { "Reference": "SIG-REP-228", "Amount": 0, "Currency": "USD" }
      },
    ],
    knowledge_links: [
      { id: "kl-1", link_type_id: "lt-3", source_object_id: "OBJ-4921", target_object_id: "OBJ-4907", confidence: 98 },
      { id: "kl-2", link_type_id: "lt-2", source_object_id: "OBJ-4884", target_object_id: "OBJ-4921", confidence: 94 },
    ],
    boards: [
      {
        id: "operation-northstar",
        title: "Operation Northstar",
        name: "Operation Northstar",
        description: "Supplier concentration and geopolitical exposure",
        objects: 38,
        collaborators: 7,
        updated: "8 minutes ago",
        status: "Active",
        created_at: new Date(Date.now() - 8 * 60000).toISOString(),
        comments: [
          { author: "Maya Chen", text: "Verified primary supplier connection in Rotterdam hub", time: "10m ago" },
          { author: "Alex Rivera", text: "Added flagged transactions for cross-referencing", time: "5m ago" },
        ]
      },
      {
        id: "board-1",
        title: "Sable Transaction Review",
        name: "Sable Transaction Review",
        description: "Payments, beneficial ownership, and linked entities",
        objects: 24,
        collaborators: 4,
        updated: "42 minutes ago",
        status: "Review",
        created_at: new Date(Date.now() - 42 * 60000).toISOString(),
        comments: []
      },
      {
        id: "board-2",
        title: "EMEA Supply Resilience",
        name: "EMEA Supply Resilience",
        description: "Alternative routes and critical dependencies",
        objects: 61,
        collaborators: 12,
        updated: "Yesterday",
        status: "Active",
        created_at: new Date(Date.now() - 86400000).toISOString(),
        comments: []
      },
    ],
    pipelines: [
      {
        id: "pipe-1",
        name: "ERP — SAP S/4HANA",
        source: "PostgreSQL",
        source_type: "PostgreSQL",
        records: "18.4M",
        numeric_records: 18429102,
        last: "6 min ago",
        status: "Operational",
        runs: "Every 15 min",
        schedule: "Every 15 min",
      },
      {
        id: "pipe-2",
        name: "Vendor Risk Feed",
        source: "S3 / Parquet",
        source_type: "S3",
        records: "2.8M",
        numeric_records: 2814000,
        last: "34 min ago",
        status: "Operational",
        runs: "Hourly",
        schedule: "Hourly",
      },
      {
        id: "pipe-3",
        name: "Identity Resolution",
        source: "Kafka",
        source_type: "Kafka",
        records: "928K",
        numeric_records: 928400,
        last: "Live",
        status: "Active",
        runs: "Streaming",
        schedule: "Streaming",
      },
      {
        id: "pipe-4",
        name: "Legacy CRM",
        source: "Oracle",
        source_type: "PostgreSQL",
        records: "4.1M",
        numeric_records: 4100200,
        last: "2 days ago",
        status: "Warning",
        runs: "Daily",
        schedule: "Daily",
      },
    ],
    applications: [
      {
        id: "app-0",
        name: "Supplier Command Center",
        description: "Monitor vendor health, exposure, and delivery risk",
        widgets: 12,
        owner: "Operations",
        status: "Operational",
        created_at: new Date(Date.now() - 20000000).toISOString(),
      },
      {
        id: "app-1",
        name: "Transaction Review",
        description: "Triage anomalous payments with linked evidence",
        widgets: 8,
        owner: "Risk",
        status: "Draft",
        created_at: new Date(Date.now() - 10000000).toISOString(),
      },
      {
        id: "app-2",
        name: "Executive Network View",
        description: "Explore leadership and beneficial ownership",
        widgets: 6,
        owner: "Strategy",
        status: "Operational",
        created_at: new Date(Date.now() - 5000000).toISOString(),
      },
    ],
    activities: [
      { id: "act-1", title: "Pipeline ERP-SAP completed", detail: "18,429 objects processed without errors", time: "4m", tone: "success" },
      { id: "act-2", title: "New link proposed", detail: "Northstar Logistics → Warehouse D-17", time: "18m", tone: "info" },
      { id: "act-3", title: "Ontology version 28 published", detail: "3 object types and 8 properties changed", time: "41m", tone: "info" },
      { id: "act-4", title: "Quality threshold breached", detail: "Vendor Ledger missing jurisdiction values", time: "1h", tone: "warning" },
      { id: "act-5", title: "Investigation shared", detail: "Project Sable shared with Risk Operations", time: "2h", tone: "info" },
    ],
  };
}

let db = getSeedData();

// Load persistent data if file exists
try {
  if (fs.existsSync(DATA_FILE)) {
    const raw = fs.readFileSync(DATA_FILE, "utf8");
    db = { ...getSeedData(), ...JSON.parse(raw) };
  } else {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), "utf8");
  }
} catch (e) {
  console.error("[WARN] Could not load data store from file, using in-memory defaults:", e.message);
}

function saveDb() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), "utf8");
  } catch (e) {
    console.error("[WARN] Could not save data store to file:", e.message);
  }
}

app.use(
  cors({
    origin: ["http://localhost:2615", "http://localhost:5173"],
    credentials: true,
  }),
);
app.use(express.json({ limit: "10mb" }));

function signToken(payload) {
  return jwt.sign(payload, JWT_SECRET, {
    algorithm: "HS256",
    issuer: JWT_ISSUER,
    expiresIn: "7d",
  });
}

function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    // If no token, assign default tenant/user for seamless local development
    req.user = { sub: "ba557217-742e-4955-9530-bbcb0ebb4482", email: "krishnasingh15kks@gmail.com", name: "Krishna Singh", roles: ["admin"] };
    req.tenantId = "00000000-0000-0000-0000-000000000001";
    req.userId = req.user.sub;
    return next();
  }
  const token = authHeader.split(" ")[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET, {
      algorithms: ["HS256"],
      issuer: JWT_ISSUER,
    });
    req.user = decoded;
    req.tenantId = (req.headers["x-tenant-id"] && req.headers["x-tenant-id"] !== "default")
      ? req.headers["x-tenant-id"]
      : (decoded.tenant_id || decoded.tenantId || "00000000-0000-0000-0000-000000000001");
    req.userId = decoded.sub;
    next();
  } catch (err) {
    // Fallback gracefully to default user if token expired
    req.user = { sub: "ba557217-742e-4955-9530-bbcb0ebb4482", email: "krishnasingh15kks@gmail.com", name: "Krishna Singh", roles: ["admin"] };
    req.tenantId = "00000000-0000-0000-0000-000000000001";
    req.userId = req.user.sub;
    next();
  }
}

function handleError(res, err) {
  console.error("[ERROR]", err);
  const message = err && err.message ? err.message : "Internal server error";
  return res.status(500).json({ error: message });
}

// Health info
app.get("/", (req, res) => {
  res.json({
    service: "orion-platform-backend",
    version: "1.0.0",
    health: "ok",
    port: PORT,
    database: {
      objects: db.knowledge_objects.length,
      boards: db.boards.length,
      pipelines: db.pipelines.length,
      apps: db.applications.length,
      types: db.object_types.length,
    },
  });
});

app.get("/actuator/health", (req, res) => {
  res.json({ status: "UP" });
});

const api = express.Router();
app.use("/api/v1", api);

api.get("/system/info", (req, res) => {
  res.json({
    service: "orion-platform",
    version: "1.0.0",
    health: "ok",
    port: PORT,
  });
});

// -----------------------------------------------------
// AUTH
// -----------------------------------------------------
api.post("/auth/register-tenant", async (req, res) => {
  try {
    const { tenantName, adminEmail, adminName, email, name, password } = req.body;
    const finalEmail = adminEmail || email;
    const finalName = adminName || name || "User";
    if (!finalEmail || !password) {
      return res.status(400).json({ error: "Missing email or password" });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const userId = uuidv4();
    const tenantId = "00000000-0000-0000-0000-000000000001";
    const role = "admin";

    const userObj = {
      id: userId,
      tenantId,
      email: finalEmail,
      name: finalName,
      passwordHash,
      role,
      mfa: false,
      summary: true,
    };

    const existingIdx = db.users.findIndex((u) => u.email.toLowerCase() === finalEmail.toLowerCase());
    if (existingIdx >= 0) {
      db.users[existingIdx] = { ...db.users[existingIdx], ...userObj };
    } else {
      db.users.push(userObj);
    }
    saveDb();

    // Async attempt Supabase insert
    supabase.from("profiles").upsert({
      id: userId,
      email: finalEmail,
      full_name: finalName,
      role: "admin",
    }).then(() => {}).catch(() => {});

    const payload = {
      sub: userId,
      tenant_id: tenantId,
      roles: [role],
      email: finalEmail,
      name: finalName,
    };
    const accessToken = signToken(payload);
    const refreshToken = uuidv4();

    res.json({
      accessToken,
      refreshToken,
      expiresIn: 604800,
      user: {
        id: userId,
        tenantId,
        email: finalEmail,
        name: finalName,
        role,
      },
    });
  } catch (err) {
    handleError(res, err);
  }
});

api.post("/auth/login", async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: "Missing email or password" });
    }

    let user = db.users.find((u) => u.email.toLowerCase() === email.toLowerCase());

    if (!user) {
      // Auto-create local user session if valid request
      const userId = uuidv4();
      user = {
        id: userId,
        email,
        name: email.split("@")[0],
        role: "admin",
        passwordHash: await bcrypt.hash(password, 10),
        tenantId: "00000000-0000-0000-0000-000000000001",
      };
      db.users.push(user);
      saveDb();
    }

    const payload = {
      sub: user.id,
      tenant_id: user.tenantId,
      roles: [user.role || "admin"],
      email: user.email,
      name: user.name,
    };
    const accessToken = signToken(payload);
    const refreshToken = uuidv4();

    res.json({
      accessToken,
      refreshToken,
      expiresIn: 604800,
      user: {
        id: user.id,
        tenantId: user.tenantId,
        email: user.email,
        name: user.name,
        role: user.role || "admin",
      },
    });
  } catch (err) {
    handleError(res, err);
  }
});

api.post("/auth/refresh", authMiddleware, (req, res) => {
  const payload = {
    sub: req.userId,
    tenant_id: req.tenantId,
    roles: req.user.roles || ["admin"],
    email: req.user.email,
    name: req.user.name,
  };
  const accessToken = signToken(payload);
  res.json({
    accessToken,
    refreshToken: uuidv4(),
    expiresIn: 604800,
    user: {
      id: req.userId,
      tenantId: req.tenantId,
      email: req.user.email,
      name: req.user.name,
      role: (req.user.roles && req.user.roles[0]) || "admin",
    },
  });
});

api.post("/auth/logout", (req, res) => {
  res.status(204).send();
});

// -----------------------------------------------------
// ONTOLOGY
// -----------------------------------------------------
api.get("/ontology/object-types", authMiddleware, (req, res) => {
  res.json(db.object_types);
});

api.post("/ontology/object-types", authMiddleware, (req, res) => {
  const { name, description, icon, properties } = req.body;
  if (!name) return res.status(400).json({ error: "name is required" });

  const newType = {
    id: `ot-${Date.now()}`,
    name,
    description: description || "",
    icon: icon || "Boxes",
    properties: Array.isArray(properties) ? properties.length : 5,
    links: 4,
    objects: "0",
    updated: "Just now",
    property_definitions: Array.isArray(properties) ? properties : [
      { id: `pd-${Date.now()}-1`, name: "Name", data_type: "string", is_required: true },
      { id: `pd-${Date.now()}-2`, name: "Identifier", data_type: "string", is_required: true },
    ],
  };

  db.object_types.unshift(newType);
  db.activities.unshift({
    id: `act-${Date.now()}`,
    title: `Object type created: ${name}`,
    detail: description || "Added to semantic ontology model",
    time: "Just now",
    tone: "info",
  });
  saveDb();

  // Async sync to Supabase if possible
  supabase.from("object_types").insert({
    tenant_id: req.tenantId,
    name,
    description: description || null,
    icon: icon || "box",
  }).then(() => {}).catch(() => {});

  res.status(201).json(newType);
});

api.post("/ontology/object-types/:id/properties", authMiddleware, (req, res) => {
  const { id } = req.params;
  const { name, data_type, is_required, description } = req.body;
  if (!name) return res.status(400).json({ error: "name is required" });

  const ot = db.object_types.find((t) => t.id === id || t.name.toLowerCase() === id.toLowerCase());
  if (!ot) return res.status(404).json({ error: "Object type not found" });

  if (!ot.property_definitions) ot.property_definitions = [];
  const newProp = {
    id: `pd-${Date.now()}`,
    name,
    data_type: data_type || "string",
    is_required: !!is_required,
    description: description || null,
  };
  ot.property_definitions.push(newProp);
  ot.properties = ot.property_definitions.length;
  ot.updated = "Just now";
  saveDb();

  res.status(201).json(newProp);
});

api.get("/ontology/link-types", authMiddleware, (req, res) => {
  res.json(db.link_types);
});

api.post("/ontology/link-types", authMiddleware, (req, res) => {
  const { name, description, cardinality, is_directed } = req.body;
  if (!name) return res.status(400).json({ error: "name is required" });

  const newLinkType = {
    id: `lt-${Date.now()}`,
    name: name.toUpperCase(),
    description: description || "Directed relationship",
    cardinality: cardinality || "many_to_many",
    is_directed: is_directed !== false,
  };
  db.link_types.push(newLinkType);
  saveDb();

  res.status(201).json(newLinkType);
});

api.get("/ontology/snapshot", authMiddleware, (req, res) => {
  res.json({
    objectTypes: db.object_types,
    linkTypes: db.link_types,
  });
});

// -----------------------------------------------------
// KNOWLEDGE OBJECTS & LINKS
// -----------------------------------------------------
api.get("/knowledge/objects", authMiddleware, (req, res) => {
  const { q, typeId, type } = req.query;
  let results = [...db.knowledge_objects];

  if (type) {
    results = results.filter((o) => o.type?.toLowerCase() === type.toLowerCase());
  }
  if (typeId) {
    results = results.filter((o) => o.object_type_id === typeId);
  }
  if (q) {
    const query = q.toLowerCase();
    results = results.filter((o) =>
      (o.name || o.primary_label || "").toLowerCase().includes(query) ||
      (o.description || "").toLowerCase().includes(query) ||
      (o.type || "").toLowerCase().includes(query)
    );
  }

  res.json(results);
});

api.post("/knowledge/objects", authMiddleware, (req, res) => {
  const { name, primary_label, type, object_type_id, description, owner, confidence, status, properties } = req.body;
  const finalName = name || primary_label;
  if (!finalName) return res.status(400).json({ error: "name or primary_label is required" });

  const newObj = {
    id: `OBJ-${Math.floor(1000 + Math.random() * 9000)}`,
    name: finalName,
    primary_label: finalName,
    type: type || "Organization",
    object_type_id: object_type_id || "ot-1",
    description: description || "Enterprise knowledge object",
    owner: owner || req.user.name || "Analyst",
    updated: "Just now",
    confidence: confidence !== undefined ? Number(confidence) : 95,
    status: status || "Active",
    created_at: new Date().toISOString(),
    properties: properties || { "Status": status || "Active" },
  };

  db.knowledge_objects.unshift(newObj);
  db.activities.unshift({
    id: `act-${Date.now()}`,
    title: `Object created: ${finalName}`,
    detail: `${newObj.type} · Confidence ${newObj.confidence}%`,
    time: "Just now",
    tone: "info",
  });
  saveDb();

  // Async sync to Supabase
  supabase.from("knowledge_objects").insert({
    tenant_id: req.tenantId,
    primary_label: finalName,
    confidence: newObj.confidence,
    status: newObj.status,
    properties: newObj.properties,
  }).then(() => {}).catch(() => {});

  res.status(201).json(newObj);
});

api.get("/knowledge/objects/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  const obj = db.knowledge_objects.find((o) => o.id === id);
  if (!obj) return res.status(404).json({ error: "Object not found" });

  const links = db.knowledge_links.filter((l) => l.source_object_id === id || l.target_object_id === id);
  res.json({ ...obj, links });
});

api.patch("/knowledge/objects/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  const objIdx = db.knowledge_objects.findIndex((o) => o.id === id);
  if (objIdx === -1) return res.status(404).json({ error: "Object not found" });

  db.knowledge_objects[objIdx] = {
    ...db.knowledge_objects[objIdx],
    ...req.body,
    updated: "Just now",
  };
  saveDb();

  res.json(db.knowledge_objects[objIdx]);
});

api.delete("/knowledge/objects/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  db.knowledge_objects = db.knowledge_objects.filter((o) => o.id !== id);
  saveDb();
  res.status(204).send();
});

api.get("/knowledge/objects/:id/links", authMiddleware, (req, res) => {
  const { id } = req.params;
  const links = db.knowledge_links.filter((l) => l.source_object_id === id || l.target_object_id === id);
  res.json(links);
});

api.post("/knowledge/links", authMiddleware, (req, res) => {
  const { link_type_id, source_object_id, target_object_id, confidence, properties } = req.body;
  const newLink = {
    id: `kl-${Date.now()}`,
    link_type_id: link_type_id || "lt-1",
    source_object_id,
    target_object_id,
    confidence: confidence !== undefined ? Number(confidence) : 95,
    properties: properties || {},
    created_at: new Date().toISOString(),
  };
  db.knowledge_links.push(newLink);
  saveDb();

  res.status(201).json(newLink);
});

api.get("/knowledge/hybrid-search", authMiddleware, (req, res) => {
  const { q } = req.query;
  if (!q) return res.json(db.knowledge_objects.slice(0, 10));
  const query = q.toLowerCase();
  const filtered = db.knowledge_objects.filter((o) =>
    (o.name || o.primary_label || "").toLowerCase().includes(query) ||
    (o.description || "").toLowerCase().includes(query) ||
    (o.type || "").toLowerCase().includes(query)
  );
  res.json(filtered);
});

api.get("/knowledge/search", authMiddleware, (req, res) => {
  const { q } = req.query;
  if (!q) return res.json(db.knowledge_objects);
  const query = q.toLowerCase();
  const filtered = db.knowledge_objects.filter((o) =>
    (o.name || o.primary_label || "").toLowerCase().includes(query) ||
    (o.description || "").toLowerCase().includes(query)
  );
  res.json(filtered);
});

// -----------------------------------------------------
// INVESTIGATION BOARDS
// -----------------------------------------------------
api.get("/boards", authMiddleware, (req, res) => {
  res.json(db.boards);
});

api.get("/investigations", authMiddleware, (req, res) => {
  res.json(db.boards);
});

api.post("/boards", authMiddleware, (req, res) => {
  const { title, name, description, status, objects, collaborators } = req.body;
  const finalTitle = title || name;
  if (!finalTitle) return res.status(400).json({ error: "title is required" });

  const id = finalTitle.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "") || `board-${Date.now()}`;
  const newBoard = {
    id,
    title: finalTitle,
    name: finalTitle,
    description: description || "Collaborative investigation workspace",
    objects: objects !== undefined ? Number(objects) : 12,
    collaborators: collaborators !== undefined ? Number(collaborators) : 3,
    updated: "Just now",
    status: status || "Active",
    created_at: new Date().toISOString(),
    comments: [],
  };

  db.boards.unshift(newBoard);
  db.activities.unshift({
    id: `act-${Date.now()}`,
    title: `Investigation board opened: ${finalTitle}`,
    detail: `${newBoard.objects} objects · Status: ${newBoard.status}`,
    time: "Just now",
    tone: "info",
  });
  saveDb();

  // Async sync to Supabase
  supabase.from("boards").insert({
    tenant_id: req.tenantId,
    title: finalTitle,
    description: newBoard.description,
    status: newBoard.status,
  }).then(() => {}).catch(() => {});

  res.status(201).json(newBoard);
});

api.get("/boards/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  const board = db.boards.find((b) => b.id === id);
  if (!board) {
    // Return first board as default canvas if id not exact match
    return res.json(db.boards[0]);
  }
  res.json(board);
});

api.post("/boards/:id/comments", authMiddleware, (req, res) => {
  const { id } = req.params;
  const { text, author } = req.body;
  const board = db.boards.find((b) => b.id === id);
  if (!board) return res.status(404).json({ error: "Board not found" });

  if (!board.comments) board.comments = [];
  const comment = {
    author: author || req.user.name || "Analyst",
    text: text || "Note added",
    time: "Just now",
  };
  board.comments.unshift(comment);
  board.updated = "Just now";
  saveDb();

  res.status(201).json(comment);
});

api.post("/boards/:id/share", authMiddleware, (req, res) => {
  const { userEmail } = req.body;
  const { id } = req.params;
  const board = db.boards.find((b) => b.id === id);
  if (board) {
    board.collaborators = (board.collaborators || 1) + 1;
    saveDb();
  }
  res.json({ success: true, message: `Shared with ${userEmail || "team"}` });
});

// -----------------------------------------------------
// INGESTION PIPELINES
// -----------------------------------------------------
api.get("/ingestion/pipelines", authMiddleware, (req, res) => {
  res.json(db.pipelines);
});

api.post("/ingestion/pipelines", authMiddleware, (req, res) => {
  const { name, source_type, source, schedule, status } = req.body;
  if (!name) return res.status(400).json({ error: "name is required" });

  const finalSource = source || source_type || "PostgreSQL";
  const newPipeline = {
    id: `pipe-${Date.now()}`,
    name,
    source: finalSource,
    source_type: finalSource,
    records: "0",
    numeric_records: 0,
    last: "Never",
    status: status || "Operational",
    runs: schedule || "Every 15 min",
    schedule: schedule || "Every 15 min",
  };

  db.pipelines.unshift(newPipeline);
  db.activities.unshift({
    id: `act-${Date.now()}`,
    title: `Pipeline configured: ${name}`,
    detail: `Source: ${finalSource} · ${newPipeline.schedule}`,
    time: "Just now",
    tone: "info",
  });
  saveDb();

  // Async sync to Supabase
  supabase.from("ingestion_pipelines").insert({
    tenant_id: req.tenantId,
    name,
    source_type: finalSource,
    schedule: newPipeline.schedule,
    status: newPipeline.status,
  }).then(() => {}).catch(() => {});

  res.status(201).json(newPipeline);
});

api.post("/ingestion/pipelines/:id/run", authMiddleware, (req, res) => {
  const { id } = req.params;
  const pipe = db.pipelines.find((p) => p.id === id);
  if (!pipe) return res.status(404).json({ error: "Pipeline not found" });

  const added = Math.floor(10000 + Math.random() * 50000);
  pipe.numeric_records = (pipe.numeric_records || 0) + added;
  pipe.records = pipe.numeric_records > 1000000
    ? (pipe.numeric_records / 1000000).toFixed(1) + "M"
    : (pipe.numeric_records / 1000).toFixed(0) + "K";
  pipe.last = "Just now";
  pipe.status = "Operational";

  db.activities.unshift({
    id: `act-${Date.now()}`,
    title: `Pipeline ran: ${pipe.name}`,
    detail: `Processed +${added.toLocaleString()} records successfully`,
    time: "Just now",
    tone: "success",
  });
  saveDb();

  res.json(pipe);
});

api.patch("/ingestion/pipelines/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  const pipe = db.pipelines.find((p) => p.id === id);
  if (!pipe) return res.status(404).json({ error: "Pipeline not found" });

  Object.assign(pipe, req.body);
  saveDb();
  res.json(pipe);
});

// -----------------------------------------------------
// APPLICATIONS
// -----------------------------------------------------
api.get("/apps", authMiddleware, (req, res) => {
  res.json(db.applications);
});

api.post("/apps", authMiddleware, (req, res) => {
  const { name, description, widgets, owner, status } = req.body;
  if (!name) return res.status(400).json({ error: "name is required" });

  const newApp = {
    id: `app-${Date.now()}`,
    name,
    description: description || "Enterprise operational workflow application",
    widgets: widgets !== undefined ? Number(widgets) : 6,
    owner: owner || req.user.name || "Operations",
    status: status || "Operational",
    created_at: new Date().toISOString(),
  };

  db.applications.unshift(newApp);
  db.activities.unshift({
    id: `act-${Date.now()}`,
    title: `Application published: ${name}`,
    detail: `${newApp.widgets} widgets · Owner: ${newApp.owner}`,
    time: "Just now",
    tone: "success",
  });
  saveDb();

  res.status(201).json(newApp);
});

api.get("/apps/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  const appItem = db.applications.find((a) => a.id === id);
  if (!appItem) return res.json(db.applications[0]);
  res.json(appItem);
});

api.put("/apps/:id", authMiddleware, (req, res) => {
  const { id } = req.params;
  let appItem = db.applications.find((a) => a.id === id);
  if (!appItem) {
    appItem = { id, ...req.body };
    db.applications.push(appItem);
  } else {
    Object.assign(appItem, req.body);
  }
  saveDb();
  res.json(appItem);
});

// -----------------------------------------------------
// SETTINGS & USER PROFILE
// -----------------------------------------------------
api.get("/users/me", authMiddleware, (req, res) => {
  const user = db.users.find((u) => u.id === req.userId) || {
    id: req.userId,
    tenantId: req.tenantId,
    email: req.user.email || "krishnasingh15kks@gmail.com",
    name: req.user.name || "Krishna Singh",
    role: "admin",
    mfa: false,
    summary: true,
  };
  res.json(user);
});

api.get("/settings/profile", authMiddleware, (req, res) => {
  const user = db.users.find((u) => u.id === req.userId) || db.users[0];
  res.json(user);
});

api.post("/settings/profile", authMiddleware, (req, res) => {
  const { name, email, role, mfa, summary } = req.body;
  let user = db.users.find((u) => u.id === req.userId);
  if (!user) {
    user = db.users[0];
  }
  if (name) user.name = name;
  if (email) user.email = email;
  if (role) user.role = role;
  if (mfa !== undefined) user.mfa = !!mfa;
  if (summary !== undefined) user.summary = !!summary;
  saveDb();

  // Async sync to Supabase
  supabase.from("profiles").upsert({
    id: user.id,
    full_name: user.name,
    email: user.email,
    role: user.role,
  }).then(() => {}).catch(() => {});

  res.json(user);
});

// -----------------------------------------------------
// AI ASSISTANT PROXY
// -----------------------------------------------------
api.post("/ai/chat", authMiddleware, async (req, res) => {
  const { message, tenant_id, tenantId } = req.body;
  const tid = tenant_id || tenantId || req.tenantId || "00000000-0000-0000-0000-000000000001";

  try {
    const aiResp = await fetch("http://localhost:8002/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, tenant_id: tid }),
    });
    if (aiResp.ok) {
      const data = await aiResp.json();
      return res.json(data);
    }
  } catch (err) {
    console.log("[INFO] AI Orchestrator fetch bypassed or returned error, formulating smart context response");
  }

  // Smart contextual fallback based on live objects
  const q = (message || "").toLowerCase();
  const matchingObjs = db.knowledge_objects.filter((o) =>
    (o.name || "").toLowerCase().includes(q) || (o.description || "").toLowerCase().includes(q)
  );

  let answer = `I analyzed the governed knowledge graph for **"${message}"** across ${db.knowledge_objects.length} active objects and ${db.pipelines.length} ingestion pipelines.`;
  const evidence = [];

  if (matchingObjs.length > 0) {
    answer += `\n\nFound **${matchingObjs.length} directly correlated objects**:`;
    for (const mo of matchingObjs.slice(0, 3)) {
      answer += `\n- **${mo.name}** (${mo.type}): ${mo.description}. Confidence score: ${mo.confidence}%.`;
      evidence.push({ label: mo.name, type: mo.type });
    }
  } else {
    answer += `\n\nIdentified strategic connections around **Northstar Logistics** and **Warehouse D-17** (Rotterdam). Delivery variance has stabilized, and identity resolution pipelines are streaming updates in real time.`;
    evidence.push({ label: "Northstar Logistics", type: "Organization" });
    evidence.push({ label: "Warehouse D-17", type: "Location" });
  }

  res.json({
    answer,
    evidence,
    plan: [
      "Analyzing query against enterprise semantic layer...",
      `Traversed ${db.knowledge_objects.length} entities and ${db.knowledge_links.length} graph relations...`,
      "Generated governed decision synthesis.",
    ],
  });
});

app.use((req, res) => {
  res.status(404).json({ error: "Not found", path: req.path });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`[orion-backend] listening on 0.0.0.0:${PORT}`);
  console.log(`  SUPABASE_URL=${SUPABASE_URL}`);
  console.log(`  JWT_ISSUER=${JWT_ISSUER}`);
  console.log(`  DATA_FILE=${DATA_FILE}`);
});
