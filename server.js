const http = require("http");

const SYSTEM = `You control an Android phone. Reply ONLY with JSON:
{"action":"...","args":{...},"reply":"short spoken reply"}
Actions: open_app{name}, call{number}, sms{number,text}, alarm{hour,minute,label},
flashlight{on:boolean}, web_search{query}, none{}. Use none for chit-chat.`;

const send = (res, code, obj) => {
  res.writeHead(code, { "Content-Type": "application/json" });
  res.end(JSON.stringify(obj));
};

http.createServer(async (req, res) => {
  if (req.method === "GET") return send(res, 200, { ok: true });
  if (req.method !== "POST" || req.url !== "/think") return send(res, 404, { error: "not found" });
  if (req.headers["x-app-token"] !== process.env.APP_TOKEN) return send(res, 401, { error: "unauthorized" });

  let raw = "";
  for await (const c of req) { raw += c; if (raw.length > 5000) return send(res, 413, { error: "too big" }); }
  try {
    const { command } = JSON.parse(raw);
    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "x-api-key": process.env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
      },
      body: JSON.stringify({
        model: "claude-sonnet-5-5", max_tokens: 300, system: SYSTEM,
        messages: [{ role: "user", content: String(command) }],
      }),
    });
    const data = await r.json();
    const text = data.content[0].text;
    const j = JSON.parse(text.slice(text.indexOf("{"), text.lastIndexOf("}") + 1));
    send(res, 200, { action: j.action || "none", args: j.args || {}, reply: j.reply || "" });
  } catch (e) {
    send(res, 500, { error: String(e.message || e) });
  }
}).listen(process.env.PORT || 10000);
