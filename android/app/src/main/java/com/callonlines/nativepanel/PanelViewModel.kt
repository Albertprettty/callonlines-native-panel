package com.callonlines.nativepanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callonlines.nativepanel.data.MeResponse
import com.callonlines.nativepanel.data.PanelRepository
import com.callonlines.nativepanel.data.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PanelUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val me: MeResponse? = null,
    val callerDraft: String = "",
    val passCurrent: String = "",
    val passNew: String = "",
    val passConfirm: String = "",
    val lastGeneratedPassword: String? = null,
)

class PanelViewModel(
    private val repo: PanelRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(PanelUiState())
    val ui: StateFlow<PanelUiState> = _ui.asStateFlow()

    fun clearState() {
        _ui.value = PanelUiState()
    }

    fun clearToast() {
        _ui.value = _ui.value.copy(message = null, error = null, lastGeneratedPassword = null)
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            val res = repo.loadMe()
            res.fold(
                onSuccess = { me ->
                    val caller = me.sip?.callerid.orEmpty()
                    _ui.value = PanelUiState(
                        loading = false,
                        me = me,
                        callerDraft = caller,
                    )
                },
                onFailure = { e ->
                    _ui.value = PanelUiState(loading = false, error = e.message ?: "Error")
                },
            )
        }
    }

    fun setCallerDraft(v: String) {
        _ui.value = _ui.value.copy(callerDraft = v)
    }

    fun setPassCurrent(v: String) {
        _ui.value = _ui.value.copy(passCurrent = v)
    }

    fun setPassNew(v: String) {
        _ui.value = _ui.value.copy(passNew = v)
    }

    fun setPassConfirm(v: String) {
        _ui.value = _ui.value.copy(passConfirm = v)
    }

    fun saveCallerId(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, message = null)
            val res = repo.updateCallerId(_ui.value.callerDraft)
            res.fold(
                onSuccess = {
                    _ui.value = _ui.value.copy(loading = false, message = "CallerID actualizado")
                    refresh()
                    onDone(null)
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message)
                    onDone(e.message)
                },
            )
        }
    }

    fun changePassword(onMustRelogin: (generated: String?) -> Unit) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, message = null)
            val s = _ui.value
            val res = repo.changePassword(s.passCurrent, s.passNew, s.passConfirm)
            res.fold(
                onSuccess = { r ->
                    val gen = r.generatedPassword
                    tokenStore.clear()
                    onMustRelogin(gen)
                    _ui.value = PanelUiState(
                        lastGeneratedPassword = gen,
                        message = if (gen != null) {
                            "Nueva clave generada (guárdala): $gen"
                        } else {
                            "Contraseña actualizada. Inicia sesión de nuevo."
                        },
                    )
                },
                onFailure = { e ->
                    _ui.value = _ui.value.copy(loading = false, error = e.message)
                },
            )
        }
    }

    companion object {
        fun factory(repo: PanelRepository, tokenStore: TokenStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PanelViewModel(repo, tokenStore) as T
                }
            }
    }
}
