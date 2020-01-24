package ru.lrmk.newttcalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    //val BOOT_ACTION = "android.intent.action.BOOT_COMPLETED"

    override fun onReceive(context: Context, intent: Intent) {
        //if (intent.action == BOOT_ACTION)
            context.startService(Intent(context, SetAlarmService::class.java))
    }
}
