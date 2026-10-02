/**
 * OmniAssist AI Backend Server (Node.js + Express)
 * Handles multi-turn conversational context, intent routing, and automation macro dispatch
 */

const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 5000;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

// Known macro registry
const MACRO_REGISTRY = [
  {
    id: "youtube_search",
    triggerPhrase: "open youtube and search",
    title: "YouTube Search Automation",
    steps: [
      { actionType: "LAUNCH_APP", target: "com.google.android.youtube", timeoutMs: 2000 },
      { actionType: "WAIT_FOR_SCREEN_TEXT", target: "Search", timeoutMs: 3000 },
      { actionType: "TAP_TEXT", target: "Search", timeoutMs: 1000 },
      { actionType: "INPUT_TEXT", target: "Search", value: "AI Phone Automation", timeoutMs: 1000 }
    ]
  },
  {
    id: "whatsapp_message",
    triggerPhrase: "send whatsapp message",
    title: "WhatsApp Quick Message",
    steps: [
      { actionType: "LAUNCH_APP", target: "com.whatsapp", timeoutMs: 2000 },
      { actionType: "WAIT_FOR_SCREEN_TEXT", target: "Search", timeoutMs: 3000 },
      { actionType: "TAP_TEXT", target: "Search", timeoutMs: 1000 }
    ]
  }
];

app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'OmniAssist LLM Dispatcher', version: '2.0.0' });
});

app.get('/macros', (req, res) => {
  res.json({ macros: MACRO_REGISTRY });
});

app.post('/api/assist', async (req, res) => {
  const { query, screenContext, history = [] } = req.body;
  if (!query) {
    return res.status(400).json({ error: 'Query text is required' });
  }

  console.log(`[OmniAssist] Received query: "${query}" (Screen Context length: ${screenContext?.length || 0})`);

  const lower = query.toLowerCase().trim();

  // 1. Direct macro pattern matching
  for (const macro of MACRO_REGISTRY) {
    if (lower.includes(macro.triggerPhrase)) {
      return res.json({
        intent: 'RUN_MACRO',
        spokenResponse: `Executing automation macro: ${macro.title}`,
        macro: macro
      });
    }
  }

  // 2. Call / SMS intent
  if (lower.startsWith('call ')) {
    const contact = query.substring(5).trim();
    return res.json({
      intent: 'CALL_CONTACT',
      spokenResponse: `Calling ${contact} now.`,
      target: contact
    });
  }

  // 3. Screen Inspection intent
  if (lower.includes('screen') && (lower.includes('what') || lower.includes('summarize'))) {
    return res.json({
      intent: 'INSPECT_SCREEN',
      spokenResponse: screenContext
        ? `Here is what I analyze on your screen:\n${screenContext.substring(0, 300)}...`
        : 'Screen content is not available or Accessibility permission is disabled.'
    });
  }

  // 4. Multi-turn AI response with Gemini API
  if (GEMINI_API_KEY) {
    try {
      const prompt = `You are OmniAssist, a phone assistant. User said: "${query}". Respond concisely in 1-2 spoken sentences.`;
      const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;
      const geminiRes = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        })
      });
      const data = await geminiRes.json();
      const aiReply = data.candidates?.[0]?.content?.parts?.[0]?.text || "Command processed.";
      return res.json({
        intent: 'GENERAL_ANSWER',
        spokenResponse: aiReply
      });
    } catch (err) {
      console.error('Gemini API call failed:', err);
    }
  }

  // Fallback response
  res.json({
    intent: 'GENERAL_ANSWER',
    spokenResponse: `Understood: "${query}". I am ready to automate tasks, scan barcodes, or control your phone.`
  });
});

app.listen(PORT, () => {
  console.log(`OmniAssist LLM Backend running on http://0.0.0.0:${PORT}`);
});
