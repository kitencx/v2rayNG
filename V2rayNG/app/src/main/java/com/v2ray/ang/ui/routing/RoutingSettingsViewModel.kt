package com.v2ray.ang.ui.routing

import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.app.Application
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.extension.moveItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RoutingSettingsViewModel(application: Application) : BaseViewModel(application) {
    private val rulesets: MutableList<RulesetItem> = mutableListOf()

    private val _rulesetsFlow = MutableStateFlow<List<RulesetItem>>(emptyList())
    val rulesetsFlow: StateFlow<List<RulesetItem>> = _rulesetsFlow.asStateFlow()

    fun getAll(): List<RulesetItem> = rulesets.toList()

    private var reloadJob: Job? = null
    private val writes = Mutex()
    private val _editorState = MutableStateFlow<RoutingEditorState?>(null)
    internal val editorState = _editorState.asStateFlow()

    private fun loadedRules(): MutableList<RulesetItem> {
        val loaded = MmkvManager.decodeRoutingRulesets()?.toMutableList() ?: mutableListOf()
        var changed = false
        loaded.forEach { item ->
            if (item.id.isBlank()) { item.id = UUID.randomUUID().toString(); changed = true }
        }
        if (changed) MmkvManager.encodeRoutingRulesets(loaded)
        return loaded
    }

    fun reload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { loadedRules() }
                rulesets.clear(); rulesets.addAll(loaded)
                _rulesetsFlow.value = loaded.toList()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load routing rules", error)
                toastError(R.string.toast_failure)
            }
        }
    }

    fun update(id: String, item: RulesetItem) {
        viewModelScope.launch {
            writes.withLock {
                try {
                    val updated = withContext(Dispatchers.IO) {
                        val current = loadedRules()
                        val next = replaceRuleById(current, id, item) ?: return@withContext null
                        MmkvManager.encodeRoutingRulesets(next.toMutableList())
                        next
                    } ?: return@withLock
                    rulesets.clear(); rulesets.addAll(updated); _rulesetsFlow.value = updated
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to update routing rule: $id", error)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    fun move(fromId: String, toId: String) {
        viewModelScope.launch {
            writes.withLock {
                try {
                    val updated = withContext(Dispatchers.IO) {
                        val current = loadedRules()
                        val from = current.indexOfFirst { it.id == fromId }
                        val to = current.indexOfFirst { it.id == toId }
                        if (from < 0 || to < 0) return@withContext null
                        current.moveItem(from, to)
                        MmkvManager.encodeRoutingRulesets(current)
                        current.toList()
                    } ?: return@withLock
                    rulesets.clear(); rulesets.addAll(updated); _rulesetsFlow.value = updated
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) { LogUtil.e(AppConfig.TAG, "Failed to reorder routing rules", error); toastError(R.string.toast_failure) }
            }
        }
    }

    fun loadEditor(id: String?, legacyPosition: Int, onUnavailable: () -> Unit) {
        if (_editorState.value != null) return
        viewModelScope.launch {
            try {
                _editorState.value = withContext(Dispatchers.IO) {
                    val current = loadedRules()
                    val rule = resolveRuleForEditor(current, id, legacyPosition)
                    RoutingEditorState(rule, SettingsManager.getProfileRemarks(), SettingsManager.canUseProcessRouting())
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load routing editor", error)
                toastError(R.string.toast_failure)
                onUnavailable()
            }
        }
    }

    fun saveEditor(item: RulesetItem, onSaved: () -> Unit): Boolean {
        if (item.remarks.isNullOrBlank()) return false
        val editor = _editorState.value ?: return false
        mutateEditor(onSaved) { current ->
            val original = editor.initial
            if (original == null) listOf(item.copy(id = UUID.randomUUID().toString())) + current
            else replaceRuleById(current, original.id, item)
        }
        return true
    }

    fun deleteEditor(onDeleted: () -> Unit) {
        val id = _editorState.value?.initial?.id ?: return
        mutateEditor(onDeleted) { current -> current.filterNot { it.id == id } }
    }

    private fun mutateEditor(onSaved: () -> Unit, mutation: (List<RulesetItem>) -> List<RulesetItem>?) {
        viewModelScope.launch {
            writes.withLock {
                try {
                    val saved = withContext(Dispatchers.IO) {
                        val next = mutation(loadedRules()) ?: return@withContext false
                        MmkvManager.encodeRoutingRulesets(next.toMutableList())
                        true
                    }
                    if (saved) { toastSuccess(R.string.toast_success); onSaved() } else toastError(R.string.toast_none_data)
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) { LogUtil.e(AppConfig.TAG, "Failed to save routing editor", error); toastError(R.string.toast_failure) }
            }
        }
    }
}

internal data class RoutingEditorState(val initial: RulesetItem?, val suggestions: List<String>, val canUseProcess: Boolean)

internal fun resolveRuleForEditor(rules: List<RulesetItem>, id: String?, legacyPosition: Int): RulesetItem? {
    if (id == null && legacyPosition < 0) return null
    return checkNotNull(if (id != null) rules.firstOrNull { it.id == id } else rules.getOrNull(legacyPosition)) {
        "The requested routing rule no longer exists"
    }
}

internal fun replaceRuleById(rules: List<RulesetItem>, id: String, replacement: RulesetItem): List<RulesetItem>? {
    if (rules.none { it.id == id }) return null
    return rules.map { if (it.id == id) replacement.copy(id = id) else it }
}
