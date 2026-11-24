package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiModelOption
import com.example.data.AssistantMessage
import com.example.data.DiscoveryCategory
import com.example.data.DiscoveryDraft
import com.example.data.DiscoveryRecord
import com.example.data.IntelAiRepository
import com.example.data.IntelFirestoreRepository
import com.example.data.PreferencesRepository
import com.example.data.ResponseDepth
import com.example.data.SearchHistoryItem
import com.example.data.SortOrder
import com.example.data.ThemePreference
import com.example.data.UiState
import com.example.data.UserPreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiscoveryInvestigationState {
    data object Idle : DiscoveryInvestigationState
    data class Investigating(val query: String) : DiscoveryInvestigationState
    data class Ready(val draft: DiscoveryDraft) : DiscoveryInvestigationState
    data class Error(val query: String, val message: String) : DiscoveryInvestigationState
}

sealed interface AssistantStreamingState {
    data object Idle : AssistantStreamingState
    data class Streaming(val partialText: String) : AssistantStreamingState
    data class Error(val message: String, val failedPrompt: String? = null) : AssistantStreamingState
}

class IntelDiscoveryViewModel(
    private val firestoreRepo: IntelFirestoreRepository,
    private val aiRepo: IntelAiRepository,
    private val prefsRepo: PreferencesRepository,
    private val currentUserId: String
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserPreferences()
        )

    val discoveriesState: StateFlow<UiState<List<DiscoveryRecord>>> =
        firestoreRepo.observeDiscoveries(currentUserId)
            .map<List<DiscoveryRecord>, UiState<List<DiscoveryRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing discoveries", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load saved discoveries."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val searchesState: StateFlow<UiState<List<SearchHistoryItem>>> =
        firestoreRepo.observeSearches(currentUserId)
            .map<List<SearchHistoryItem>, UiState<List<SearchHistoryItem>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing searches", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load search history."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val messagesState: StateFlow<UiState<List<AssistantMessage>>> =
        firestoreRepo.observeMessages(currentUserId)
            .map<List<AssistantMessage>, UiState<List<AssistantMessage>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing assistant messages", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load chat history."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DiscoveryCategory.ALL)
    val selectedCategory: StateFlow<DiscoveryCategory> = _selectedCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _investigationState =
        MutableStateFlow<DiscoveryInvestigationState>(DiscoveryInvestigationState.Idle)
    val investigationState: StateFlow<DiscoveryInvestigationState> = _investigationState.asStateFlow()

    private val _assistantInput = MutableStateFlow("")
    val assistantInput: StateFlow<String> = _assistantInput.asStateFlow()

    private val _assistantStreamingState =
        MutableStateFlow<AssistantStreamingState>(AssistantStreamingState.Idle)
    val assistantStreamingState: StateFlow<AssistantStreamingState> =
        _assistantStreamingState.asStateFlow()

    private val _isSavingDiscovery = MutableStateFlow(false)
    val isSavingDiscovery: StateFlow<Boolean> = _isSavingDiscovery.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    private var investigationJob: Job? = null
    private var assistantJob: Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: DiscoveryCategory) {
        _selectedCategory.value = category
    }

    fun selectSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun updateAssistantInput(text: String) {
        _assistantInput.value = text
    }

    fun investigateTopic(rawQuery: String = _searchQuery.value, categoryOverride: DiscoveryCategory? = null) {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.length < 2) {
            _toastMessages.tryEmit("Enter at least 2 characters to investigate a topic.")
            return
        }
        val activeCategory = categoryOverride ?: _selectedCategory.value
        _searchQuery.value = cleanQuery
        if (categoryOverride != null) {
            _selectedCategory.value = categoryOverride
        }

        investigationJob?.cancel()
        investigationJob = viewModelScope.launch {
            _investigationState.value = DiscoveryInvestigationState.Investigating(cleanQuery)
            val prefs = preferences.value
            val result = aiRepo.investigateTopic(
                query = cleanQuery,
                categoryHint = activeCategory,
                modelOption = prefs.aiModel,
                depth = prefs.responseDepth
            )
            result.fold(
                onSuccess = { draft ->
                    _investigationState.value = DiscoveryInvestigationState.Ready(draft)
                    firestoreRepo.logSearch(
                        query = cleanQuery,
                        category = activeCategory,
                        resultCount = draft.totalInsights
                    )
                },
                onFailure = { error ->
                    _investigationState.value = DiscoveryInvestigationState.Error(
                        query = cleanQuery,
                        message = error.localizedMessage
                            ?: "Unable to synthesize topic intelligence right now."
                    )
                }
            )
        }
    }

    fun saveCurrentDraft(pinned: Boolean = false) {
        val current = _investigationState.value as? DiscoveryInvestigationState.Ready ?: return
        if (current.draft.savedRecordId != null) {
            _toastMessages.tryEmit("Already saved to your Intelligence Workspace.")
            return
        }
        if (_isSavingDiscovery.value) return

        viewModelScope.launch {
            _isSavingDiscovery.value = true
            val result = firestoreRepo.saveDiscovery(current.draft, pinned = pinned)
            _isSavingDiscovery.value = false
            result.fold(
                onSuccess = { newId ->
                    _investigationState.value = DiscoveryInvestigationState.Ready(
                        current.draft.copy(savedRecordId = newId)
                    )
                    _toastMessages.tryEmit(
                        if (pinned) "Discovery saved & pinned to Dashboard" else "Discovery saved to Workspace"
                    )
                },
                onFailure = { err ->
                    _toastMessages.tryEmit(err.localizedMessage ?: "Failed to save discovery.")
                }
            )
        }
    }

    fun toggleDiscoveryPin(record: DiscoveryRecord) {
        viewModelScope.launch {
            val result = firestoreRepo.toggleDiscoveryPin(record.id, !record.pinned)
            result.fold(
                onSuccess = {
                    _toastMessages.tryEmit(if (!record.pinned) "Pinned to Dashboard" else "Unpinned")
                },
                onFailure = { err ->
                    _toastMessages.tryEmit(err.localizedMessage ?: "Could not update pin state.")
                }
            )
        }
    }

    fun deleteDiscovery(record: DiscoveryRecord) {
        viewModelScope.launch {
            val result = firestoreRepo.deleteDiscovery(record.id)
            result.fold(
                onSuccess = {
                    val ready = _investigationState.value as? DiscoveryInvestigationState.Ready
                    if (ready?.draft?.savedRecordId == record.id) {
                        _investigationState.value = DiscoveryInvestigationState.Ready(
                            ready.draft.copy(savedRecordId = null)
                        )
                    }
                    _toastMessages.tryEmit("Removed \"${record.query}\"")
                },
                onFailure = { err ->
                    _toastMessages.tryEmit(err.localizedMessage ?: "Could not delete discovery.")
                }
            )
        }
    }

    fun sendAssistantMessage(rawPrompt: String = _assistantInput.value) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) {
            _toastMessages.tryEmit("Type a question or analytical prompt first.")
            return
        }
        if (_assistantStreamingState.value is AssistantStreamingState.Streaming) return

        _assistantInput.value = ""
        val prefs = preferences.value
        val existingHistory = (messagesState.value as? UiState.Success)?.data.orEmpty()

        assistantJob?.cancel()
        assistantJob = viewModelScope.launch {
            _assistantStreamingState.value = AssistantStreamingState.Streaming("")
            val userSaveResult = firestoreRepo.appendMessage(
                role = "user",
                content = prompt,
                modelName = prefs.aiModel.modelId
            )
            if (userSaveResult.isFailure) {
                _assistantStreamingState.value = AssistantStreamingState.Error(
                    message = userSaveResult.exceptionOrNull()?.localizedMessage
                        ?: "Could not persist your message.",
                    failedPrompt = prompt
                )
                return@launch
            }

            var latestReply = ""
            try {
                aiRepo.streamAssistantReply(
                    prompt = prompt,
                    history = existingHistory,
                    modelOption = prefs.aiModel,
                    depth = prefs.responseDepth
                ).collect { accumulated ->
                    latestReply = accumulated
                    _assistantStreamingState.value = AssistantStreamingState.Streaming(accumulated)
                }

                val assistantSaveResult = firestoreRepo.appendMessage(
                    role = "assistant",
                    content = latestReply,
                    modelName = prefs.aiModel.modelId
                )
                if (assistantSaveResult.isFailure) {
                    _toastMessages.tryEmit("Reply generated, but failed to sync to cloud history.")
                }
                _assistantStreamingState.value = AssistantStreamingState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Assistant stream timed out", e)
                _assistantStreamingState.value = AssistantStreamingState.Error(
                    message = "Response timed out after 60 seconds. Tap retry to ask again.",
                    failedPrompt = prompt
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                _assistantStreamingState.value = AssistantStreamingState.Error(
                    message = e.localizedMessage ?: "AI Assistant request failed.",
                    failedPrompt = prompt
                )
            }
        }
    }

    fun prepareAssistantPromptFromDraft(draft: DiscoveryDraft) {
        _assistantInput.value =
            "Deepen the analysis of \"${draft.query}\". Based on this summary (${draft.summary}), " +
                "what are the biggest blind spots or contrarian risks to examine next?"
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            firestoreRepo.clearSearchHistory().fold(
                onSuccess = { _toastMessages.tryEmit("Search history cleared") },
                onFailure = { _toastMessages.tryEmit(it.localizedMessage ?: "Failed to clear search history.") }
            )
        }
    }

    fun clearAssistantMessages() {
        viewModelScope.launch {
            assistantJob?.cancel()
            _assistantStreamingState.value = AssistantStreamingState.Idle
            firestoreRepo.clearMessages().fold(
                onSuccess = { _toastMessages.tryEmit("Assistant conversation cleared") },
                onFailure = { _toastMessages.tryEmit(it.localizedMessage ?: "Failed to clear conversation.") }
            )
        }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefsRepo.setTheme(theme) }
    }

    fun setAiModelOption(model: AiModelOption) {
        viewModelScope.launch { prefsRepo.setAiModel(model) }
    }

    fun setResponseDepth(depth: ResponseDepth) {
        viewModelScope.launch { prefsRepo.setResponseDepth(depth) }
    }

    fun showToast(message: String) {
        _toastMessages.tryEmit(message)
    }

    private companion object {
        const val TAG = "IntelDiscoveryVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
