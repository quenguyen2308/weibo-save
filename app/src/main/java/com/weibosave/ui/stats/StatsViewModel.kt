package com.weibosave.ui.stats

import androidx.lifecycle.ViewModel
import com.weibosave.data.UsageRepository
import com.weibosave.model.UsageSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

enum class DateFilter { TODAY, THIS_WEEK, THIS_MONTH, ALL }

data class StatsUiState(
    val filter: DateFilter = DateFilter.ALL,
    val allSessions: List<UsageSession> = emptyList(),
    val filteredSessions: List<UsageSession> = emptyList(),
    val showDeleteConfirm: DeleteScope? = null,
)

enum class DeleteScope { WEEK, MONTH, ALL }

class StatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        val sessions = UsageRepository.getAllSessions().sortedByDescending { it.timestamp }
        val current = _uiState.value
        _uiState.value = current.copy(allSessions = sessions)
        applyFilter(current.filter, sessions)
    }

    fun setFilter(filter: DateFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        applyFilter(filter, _uiState.value.allSessions)
    }

    fun requestDelete(scope: DeleteScope) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = scope)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
    }

    fun confirmDelete() {
        when (_uiState.value.showDeleteConfirm) {
            DeleteScope.WEEK -> UsageRepository.deleteSessionsNewerThan(
                System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            )
            DeleteScope.MONTH -> UsageRepository.deleteSessionsNewerThan(
                System.currentTimeMillis() - 30L * 24 * 3600 * 1000
            )
            DeleteScope.ALL -> UsageRepository.clearAll()
            null -> Unit
        }
        _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        load()
    }

    private fun applyFilter(filter: DateFilter, sessions: List<UsageSession>) {
        val cutoff = when (filter) {
            DateFilter.TODAY -> startOfDay()
            DateFilter.THIS_WEEK -> startOfWeek()
            DateFilter.THIS_MONTH -> startOfMonth()
            DateFilter.ALL -> 0L
        }
        _uiState.value = _uiState.value.copy(
            filteredSessions = sessions.filter { it.timestamp >= cutoff }
        )
    }

    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfWeek(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
