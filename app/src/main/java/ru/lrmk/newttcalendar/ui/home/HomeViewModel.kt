package ru.lrmk.newttcalendar.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.lrmk.newttcalendar.Calendar

class HomeViewModel : ViewModel() {
    val PROJECTION_ID_INDEX = 0
    val PROJECTION_DISPLAY_NAME_INDEX = 2
    val please = listOf(Calendar(0, "Пожалуйста, дайте разрешение на доступ к календарю"))
    lateinit var context: Context

    @SuppressLint("MissingPermission")
    private val _text = MutableLiveData<List<Calendar>>().apply {
        value = please
        viewModelScope.launch {
            value = withContext(Dispatchers.IO) {
                val EVENT_PROJECTION = arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.OWNER_ACCOUNT
                )
                var cur: Cursor? = null
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
                    cur = context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, EVENT_PROJECTION, null, null, null)

                var calendars = mutableListOf<Calendar>()
                while (cur!=null && cur.moveToNext()) {
                    calendars.add(Calendar(cur.getLong(PROJECTION_ID_INDEX), cur.getString(PROJECTION_DISPLAY_NAME_INDEX)))
                }
                if (calendars.size == 0) calendars.add(Calendar(0, "К сожалению, у вас нет ни одного календаря"))
                if (cur==null) please else calendars
            }
        }
    }
    val text: LiveData<List<Calendar>> = _text
}