package com.drducqy.edb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drducqy.edb.core.DictionaryTrie
import com.drducqy.edb.core.TranslatorEngine
import com.drducqy.edb.data.repository.DictionaryRepository
import com.drducqy.edb.data.source.AssetDataSource
import com.drducqy.edb.data.source.FileDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val trie = DictionaryTrie()

    private val repository = DictionaryRepository(
        AssetDataSource(application),
        FileDataSource(application),
        trie,
        application
    )

    // Truyền repository vào Engine để dùng map Phồn-Giản
    private val engine = TranslatorEngine(trie, repository)

    // StateFlow cho UI
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _outputText = MutableStateFlow("")
    val outputText: StateFlow<String> = _outputText.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        // Mặc định nạp tiếng Anh khi mở app
        loadLanguage("en")
    }

    fun switchLanguage(langCode: String) {
        loadLanguage(langCode)
    }

    private fun loadLanguage(langCode: String) {
        _isReady.value = false
        viewModelScope.launch {
            repository.loadData(langCode)
            _isReady.value = true

            // Dịch lại nếu đang có văn bản
            if (_inputText.value.isNotEmpty()) {
                translate(_inputText.value)
            }
        }
    }

    fun onTextChanged(newText: String) {
        _inputText.value = newText
        if (_isReady.value) {
            translate(newText)
        }
    }

    private fun translate(text: String) {
        viewModelScope.launch {
            val result = engine.translate(text)
            _outputText.value = result
        }
    }
}