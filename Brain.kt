package com.not.voiceagent

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AgentAction(val action: String, val args: JSONObject, val reply: String)

object Brain {
    private val client = OkHttpClient()

    // Call from a background thread.
    fun think(command: String): AgentAction {
        val body = JSONObject().put("command", command)
        val req = Request.Builder().url(BuildConfig.BACKEND_URL + "/think")
            .header("x-app-token", BuildConfig.APP_TOKEN)
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(req).execute().use { r ->
            val j = JSONObject(r.body!!.string())
            if (j.has("error")) throw Exception(j.getString("error"))
            return AgentAction(j.optString("action", "none"), j.optJSONObject("args") ?: JSONObject(), j.optString("reply"))
        }
    }
}
