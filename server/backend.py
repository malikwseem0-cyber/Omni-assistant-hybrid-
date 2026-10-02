"""
OmniAssist AI Backend Server (Python + FastAPI)
Handles multi-turn conversational context, intent routing, and automation macro dispatch
"""

import os
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn

app = FastAPI(title="OmniAssist Python LLM Backend", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

class HistoryItem(BaseModel):
    sender: str
    content: str

class AssistRequest(BaseModel):
    query: str
    screenContext: Optional[str] = ""
    history: Optional[List[HistoryItem]] = []

@app.get("/health")
def health():
    return {"status": "ok", "service": "OmniAssist Python Backend"}

@app.post("/api/assist")
def handle_assist(req: AssistRequest):
    q = req.query.strip().lower()
    
    # 1. Macro trigger dispatch
    if "open youtube" in q or "search youtube" in q:
        return {
            "intent": "RUN_MACRO",
            "spokenResponse": "Launching YouTube search automation macro",
            "macro": {
                "title": "YouTube Search",
                "steps": [
                    {"actionType": "LAUNCH_APP", "target": "com.google.android.youtube"},
                    {"actionType": "WAIT_FOR_SCREEN_TEXT", "target": "Search", "timeoutMs": 3000},
                    {"actionType": "TAP_TEXT", "target": "Search"},
                    {"actionType": "INPUT_TEXT", "target": "Search", "value": "AI phone automation"}
                ]
            }
        }
    
    # 2. Call intent
    if q.startswith("call "):
        target = req.query[5:].strip()
        return {
            "intent": "CALL_CONTACT",
            "spokenResponse": f"Placing call to {target}",
            "target": target
        }

    # 3. Screen inspection
    if "screen" in q and ("what" in q or "summarize" in q):
        ctx = req.screenContext[:250] if req.screenContext else "No active window text detected."
        return {
            "intent": "INSPECT_SCREEN",
            "spokenResponse": f"Screen context:\n{ctx}"
        }

    # Default reply
    return {
        "intent": "GENERAL_ANSWER",
        "spokenResponse": f"OmniAssist executed: '{req.query}'. All phone subsystems active."
    }

if __name__ == "__main__":
    uvicorn.run("backend:app", host="0.0.0.0", port=5000, reload=True)
