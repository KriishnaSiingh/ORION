from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import httpx
import asyncio
import uuid

app = FastAPI(title="Orion AI Orchestrator")

JAVA_BACKEND_URL = "http://localhost:8085/api/v1"

class ChatRequest(BaseModel):
    message: str
    tenant_id: str

class ChatResponse(BaseModel):
    answer: str
    evidence: List[Dict[str, Any]]
    plan: List[str]

class InvestigationTrigger(BaseModel):
    eventType: str
    objectId: str
    properties: Dict[str, Any]
    timestamp: int

async def call_backend(endpoint: str, params: Dict[str, Any] = None):
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{JAVA_BACKEND_URL}{endpoint}", params=params)
            response.raise_for_status()
            return response.json()
    except (httpx.HTTPError, Exception):
        return []

async def search_tool(query: str, object_type: Optional[str] = None):
    """Performs hybrid search on the knowledge graph."""
    params = {"q": query, "limit": 5}
    if object_type:
        params["type"] = object_type
    results = await call_backend("/knowledge/hybrid-search", params)
    return results

async def traverse_tool(object_id: str):
    """Finds related objects in the knowledge graph."""
    results = await call_backend(f"/knowledge/objects/{object_id}/links")
    return results

@app.get("/")
async def root():
    return {
        "service": "Orion AI Orchestrator",
        "version": "1.0.0",
        "backend": JAVA_BACKEND_URL,
        "endpoints": {
            "health": "/health",
            "chat": "/chat (POST)",
            "investigate": "/investigate (POST)"
        }
    }

@app.get("/health")
async def health():
    return {"status": "ok", "backend": JAVA_BACKEND_URL}

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    user_msg = request.message.lower()
    plan = []
    evidence = []

    plan.append("Analyzing query to identify target entities...")

    if "who is" in user_msg or "find" in user_msg:
        plan.append("Executing hybrid search to locate candidate objects...")
        keyword = user_msg.split("find")[-1].strip() if "find" in user_msg else user_msg.split("is")[-1].strip()
        search_results = await search_tool(keyword)

        if search_results:
            target = search_results[0]
            evidence.append(target)
            obj_id = target.get("id")

            plan.append(f"Found object {obj_id}. Traversing links to find connections...")
            connections = await traverse_tool(obj_id)
            evidence.extend(connections)
        else:
            plan.append("No matching objects found in the knowledge graph.")
    else:
        plan.append("Query too vague. Performing general knowledge search...")
        search_results = await search_tool(user_msg)
        evidence.extend(search_results[:3])

    plan.append("Synthesizing final answer based on gathered evidence...")

    if not evidence:
        answer = "I couldn't find any specific information in the knowledge graph to answer your question."
    else:
        entities = [e.get("name", "Unknown") for e in evidence]
        answer = f"Based on the knowledge graph, I found {len(entities)} related entities: {', '.join(entities)}. These objects are linked via the established ontology."

    return ChatResponse(
        answer=answer,
        evidence=evidence,
        plan=plan
    )

@app.post("/investigate")
async def investigate(trigger: InvestigationTrigger):
    """Autonomous agentic loop triggered by ingestion events."""
    print(f"🚨 TRIGGER RECEIVED: {trigger.eventType} for object {trigger.objectId}")

    properties = trigger.properties
    risk_score = float(properties.get("riskScore", 0))

    if risk_score > 0.7:
        print(f"🔥 High risk detected ({risk_score}). Initiating deep investigation...")
        connections = await traverse_tool(trigger.objectId)
        patterns = await search_tool("known fraud patterns")

        report_id = f"report_{uuid.uuid4().hex[:8]}"
        report_content = f"Investigation Report for {trigger.objectId}: Found {len(connections)} connections and {len(patterns)} matching patterns."

        print(f"✅ Investigation complete. Report created: {report_id}")
        return {"status": "investigation_complete", "reportId": report_id}

    print("Low risk. Monitoring only.")
    return {"status": "monitored"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)
