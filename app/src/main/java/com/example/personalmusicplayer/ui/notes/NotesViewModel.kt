package com.example.personalmusicplayer.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalmusicplayer.PersonalMusicApp
import kotlinx.coroutines.launch

/**
 * Заметки к песням. Заметка хранится в самой песне (не в плейлисте), поэтому одинакова везде
 * и не теряется при обновлении библиотеки. Пустая или null — заметка удаляется.
 */
class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val library = (app as PersonalMusicApp).libraryRepository

    fun setNote(songId: Long, note: String?) {
        viewModelScope.launch { library.setNote(songId, note) }
    }
}
