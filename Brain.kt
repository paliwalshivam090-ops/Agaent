package com.not.voiceagent

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AgentAction(val action: String, val args: JSONObject, val reply: String)

object Brain {
    private val client = OkHttpClient()
    private const val SYSTEM = """You control an Android phone. Reply ONLY with JSON:
{"action":"...","args":{...},"reply":"short spoken reply"}
Actions: open_app{name}, call{number}, sms{number,text}, alarm{hour,minute,label},
flashlight{on:boolean}, web_search{query}, none{}. Use none for chit-chat."""

    // Call from a background thread.
    fun think(command: String): AgentAction {
        val body = JSONObject()
            .put("model", "claude-sonnet-5-5")
            .put("max_tokens", 300)
            .put("system", SYSTEM)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", command)))
        val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", BuildConfig.ANTHROPIC_KEY)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(req).execute().use { r ->
            val text = JSONObject(r.body!!.string()).getJSONArray("content").getJSONObject(0).getString("text")
            val j = JSONObject(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1))
            return AgentAction(j.optString("action", "none"), j.optJSONObject("args") ?: JSONObject(), j.optString("reply"))
        }
    }
}
