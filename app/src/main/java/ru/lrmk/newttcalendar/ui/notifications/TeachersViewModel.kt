package ru.lrmk.newttcalendar.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset

class TeachersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Получаем список преподавателей..."
        viewModelScope.launch {
            value = withContext(Dispatchers.IO) {
                try {
                    URL("https://www.lrmk.ru/tt/teachers").readText(Charset.forName("cp1251"))
                }
                catch (e: IOException) {
                    "Нет связи с сервером 😕"
                }
            }
        }
    }
    val text: LiveData<String> = _text
}