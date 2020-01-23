package ru.lrmk.newttcalendar.ui.dashboard

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

class GroupsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Получаем список групп..."
        viewModelScope.launch {
            value = withContext(Dispatchers.IO) {
                try {
                    URL("https://www.lrmk.ru/tt/groups").readText(Charset.forName("cp1251"))
                }
                catch (e: IOException) {
                    "Нет связи с сервером 😕"
                }
            }
        }
    }
    val text: LiveData<String> = _text
}