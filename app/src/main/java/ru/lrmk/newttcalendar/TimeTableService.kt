package ru.lrmk.newttcalendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset


class TimeTableService : Service() {
    val CHANNEL_ID = "LRMK_TIMETABLE"
    val CHANNEL_NAME = "LRMK TimeTable"
    val CHANNEL_DESCRIPTION = "Import timetable to calendar"
    val NOTIFY_ID = 101

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("SERVICETT", "START! ${flags} ${startId}")
        CoroutineScope(Dispatchers.IO).launch {
            val content =
            try {
                URL("https://www.lrmk.ru/tt/groups").readText(Charset.forName("cp1251"))
            }
            catch (e: IOException) {
                "Нет связи с сервером 😕"
            }
            Log.i("SERVICETT", content)

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                channel.description = CHANNEL_DESCRIPTION
                channel.enableLights(true)
                channel.lightColor = Color.RED
                channel.enableVibration(false)
                notificationManager.createNotificationChannel(channel)
            }
            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_school_black_24dp)
                .setContentTitle("Расписание")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            notificationManager.notify(NOTIFY_ID, builder.build())

            stopSelf(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Log.i("SERVICETT", "STOP!")
        super.onDestroy()
    }
}
