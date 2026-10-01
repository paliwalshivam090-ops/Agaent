package com.not.voiceagent

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock

object Actions {
    // Dial/SMS open the screen pre-filled; the user taps send/call (safe by design).
    fun run(ctx: Context, a: AgentAction) {
        val g = a.args
        when (a.action) {
            "open_app" -> openApp(ctx, g.optString("name"))
            "call" -> go(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + g.optString("number"))))
            "sms" -> go(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + g.optString("number")))
                .putExtra("sms_body", g.optString("text")))
            "alarm" -> go(ctx, Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, g.optInt("hour"))
                .putExtra(AlarmClock.EXTRA_MINUTES, g.optInt("minute"))
                .putExtra(AlarmClock.EXTRA_MESSAGE, g.optString("label")))
            "flashlight" -> {
                val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cm.setTorchMode(cm.cameraIdList[0], g.optBoolean("on", true))
            }
            "web_search" -> go(ctx, Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(g.optString("query")))))
        }
    }

    private fun go(ctx: Context, i: Intent) = ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun openApp(ctx: Context, name: String) {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(main, 0).firstOrNull {
            it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        } ?: return
        pm.getLaunchIntentForPackage(match.activityInfo.packageName)?.let { go(ctx, it) }
    }
}
