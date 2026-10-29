package com.gk.englishtamildictionary

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class ModelState { CHECKING, READY, MISSING }

class DictionaryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DictionaryDb(app)

    var query by mutableStateOf("")
        private set
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var entry by mutableStateOf<Entry?>(null)
        private set
    var notFound by mutableStateOf(false)
        private set
    var modelState by mutableStateOf(ModelState.CHECKING)
        private set

    /** English text -> Tamil text (null while pending). Keys are the English strings. */
    val tamil = mutableStateMapOf<String, String>()

    private var translateJob: Job? = null

    init { prepareModel() }

    fun prepareModel() {
        modelState = ModelState.CHECKING
        viewModelScope.launch {
            modelState = if (TamilTranslator.ensureModel()) ModelState.READY else ModelState.MISSING
            entry?.let { translateEntry(it) }
        }
    }

    fun onQueryChange(text: String) {
        query = text
        notFound = false
        suggestions = db.suggestions(text)
    }

    fun search(word: String = query) {
        query = word
        suggestions = emptyList()
        val result = db.lookup(word)
        entry = result
        notFound = result == null && word.isNotBlank()
        result?.let { translateEntry(it) }
    }

    private fun translateEntry(e: Entry) {
        if (modelState != ModelState.READY) return
        translateJob?.cancel()
        translateJob = viewModelScope.launch {
            val texts = listOf(e.word) + e.senses.map { it.definition }
            for (t in texts) {
                if (tamil.containsKey(t)) continue
                TamilTranslator.translate(t)?.let { tamil[t] = it }
            }
        }
    }
}
