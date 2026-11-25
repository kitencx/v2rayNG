package com.v2ray.ang.ui.server

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class JsonFormatResult(
    val source: String,
    val formatted: String? = null,
    val line: Int? = null,
    val column: Int? = null,
)

internal fun formatJsonForEditor(source: String): JsonFormatResult = try {
    require(source.isNotBlank())
    JsonFormatResult(source, formatted = JsonUtil.formatJson(source))
} catch (error: Exception) {
    val location = Regex("line (\\d+) column (\\d+)").find(error.message.orEmpty())
    JsonFormatResult(source, line = location?.groupValues?.get(1)?.toIntOrNull(),
        column = location?.groupValues?.get(2)?.toIntOrNull())
}

class JsonEditorViewModel(application: Application) : BaseViewModel(application) {
    private var formatJob: Job? = null
    private val _formatResult = MutableStateFlow<JsonFormatResult?>(null)
    internal val formatResult = _formatResult.asStateFlow()

    fun format(source: String) {
        formatJob?.cancel()
        formatJob = viewModelScope.launch {
            try {
                _formatResult.value = withContext(Dispatchers.Default) { formatJsonForEditor(source) }
            } catch (error: CancellationException) {
                throw error
            }
        }
    }
}
