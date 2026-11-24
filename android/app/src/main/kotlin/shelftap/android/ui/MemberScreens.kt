package shelftap.android.ui

import android.nfc.Tag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.HistoryItem
import shelftap.android.api.Loan
import shelftap.android.api.Reservation
import shelftap.android.api.ReservationStatus
import shelftap.android.data.Repository
import shelftap.android.nfc.TagReader

/** A list screen's state: the loaded list plus the outcome of the last row action. */
data class ListState<T>(val list: Load<List<T>> = Load.Loading, val busyId: String? = null, val error: UiText? = null)

class LoansViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ListState<Loan>())
    val state: StateFlow<ListState<Loan>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ListState()
        viewModelScope.launch {
            val list = try { Load.Ready(repo.loans()) } catch (e: ApiException) { Load.Failed(e.userMessage()) }
            _state.update { it.copy(list = list) }
        }
    }

    fun renew(loan: Loan) {
        _state.update { it.copy(busyId = loan.loanId, error = null) }
        viewModelScope.launch {
            try {
                val renewed = repo.renew(loan.loanId)
                _state.update { s ->
                    val list = (s.list as? Load.Ready)?.value?.map { if (it.loanId == renewed.loanId) renewed else it }
                    s.copy(list = list?.let { Load.Ready(it) } ?: s.list, busyId = null)
                }
            } catch (e: ApiException) {
                _state.update { it.copy(busyId = null, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun LoansScreen() {
    val vm = appViewModel { LoansViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    LoadView(state.list, onRetry = vm::load) { loans ->
        Column(Modifier.fillMaxSize()) {
            ErrorText(state.error, Modifier.padding(16.dp))
            if (loans.isEmpty()) Text(stringResource(R.string.loans_empty), Modifier.padding(16.dp))
            LazyColumn {
                items(loans, key = { it.loanId }) { loan ->
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(loan.title, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.loans_due, formatDate(loan.dueAt), loan.renewals), style = MaterialTheme.typography.bodyMedium)
                        }
                        OutlinedButton(onClick = { vm.renew(loan) }, enabled = loan.renewable && state.busyId == null) {
                            Text(stringResource(if (state.busyId == loan.loanId) R.string.busy_dots else R.string.loans_renew))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

class CheckoutViewModel(private val repo: Repository) : ViewModel() {
    sealed interface UiState {
        data object Idle : UiState
        data object Working : UiState
        data class Done(val loan: Loan) : UiState
        data class Failed(val message: UiText) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Reader-mode callback. Reads the tag ID and asks the server for a loan; the server alone decides. No tag is written. */
    fun tagTapped(tag: Tag) {
        if (_state.value == UiState.Working) return
        val tagId = TagReader.bookTagId(tag)
        if (tagId == null) {
            _state.value = UiState.Failed(uiText(R.string.err_not_book_tag))
            return
        }
        _state.value = UiState.Working
        viewModelScope.launch {
            _state.value = try {
                UiState.Done(repo.checkout(tagId))
            } catch (e: ApiException) {
                UiState.Failed(e.userMessage())
            }
        }
    }

    fun reset() {
        if (_state.value != UiState.Working) _state.value = UiState.Idle
    }
}

@Composable
fun CheckoutScreen() {
    val vm = appViewModel { CheckoutViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    NfcGate {
        NfcReaderEffect(TagReader.BOOK_FLAGS) { vm.tagTapped(it) }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.checkout_title), style = MaterialTheme.typography.headlineSmall)
            when (val s = state) {
                CheckoutViewModel.UiState.Idle -> Text(stringResource(R.string.checkout_hold))
                CheckoutViewModel.UiState.Working -> Spinner()
                is CheckoutViewModel.UiState.Done -> {
                    Text(stringResource(R.string.checkout_borrowed, s.loan.title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.checkout_due, formatDate(s.loan.dueAt)))
                    Button(onClick = vm::reset) { Text(stringResource(R.string.checkout_again)) }
                }
                is CheckoutViewModel.UiState.Failed -> {
                    ErrorText(s.message)
                    Button(onClick = vm::reset) { Text(stringResource(R.string.action_retry)) }
                }
            }
        }
    }
}

class ReservationsViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ListState<Reservation>())
    val state: StateFlow<ListState<Reservation>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ListState()
        viewModelScope.launch {
            val list = try { Load.Ready(repo.reservations()) } catch (e: ApiException) { Load.Failed(e.userMessage()) }
            _state.update { it.copy(list = list) }
        }
    }

    fun cancel(reservation: Reservation) {
        _state.update { it.copy(busyId = reservation.reservationId, error = null) }
        viewModelScope.launch {
            try {
                repo.cancelReservation(reservation.reservationId)
                _state.update { s ->
                    val list = (s.list as? Load.Ready)?.value?.map {
                        if (it.reservationId == reservation.reservationId) it.copy(status = ReservationStatus.CANCELLED) else it
                    }
                    s.copy(list = list?.let { Load.Ready(it) } ?: s.list, busyId = null)
                }
            } catch (e: ApiException) {
                _state.update { it.copy(busyId = null, error = e.userMessage()) }
            }
        }
    }
}

@Composable
fun ReservationsScreen() {
    val vm = appViewModel { ReservationsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    LoadView(state.list, onRetry = vm::load) { reservations ->
        Column(Modifier.fillMaxSize()) {
            ErrorText(state.error, Modifier.padding(16.dp))
            if (reservations.isEmpty()) Text(stringResource(R.string.reservations_empty), Modifier.padding(16.dp))
            LazyColumn {
                items(reservations, key = { it.reservationId }) { r ->
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                when (r.status) {
                                    ReservationStatus.WAITING -> stringResource(R.string.res_waiting, formatDate(r.placedAt))
                                    ReservationStatus.READY ->
                                        r.readyAt?.let { stringResource(R.string.res_ready_since, formatDate(it)) } ?: stringResource(R.string.res_ready)
                                    ReservationStatus.FULFILLED -> stringResource(R.string.res_fulfilled)
                                    ReservationStatus.CANCELLED -> stringResource(R.string.res_cancelled)
                                    ReservationStatus.EXPIRED -> stringResource(R.string.res_expired)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (r.status == ReservationStatus.WAITING || r.status == ReservationStatus.READY) {
                            OutlinedButton(onClick = { vm.cancel(r) }, enabled = state.busyId == null) {
                                Text(stringResource(if (state.busyId == r.reservationId) R.string.busy_dots else R.string.action_cancel))
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

class HistoryViewModel(private val repo: Repository) : ViewModel() {
    data class UiState(val items: Load<List<HistoryItem>> = Load.Loading, val needsPassword: Boolean = false)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState()
        viewModelScope.launch {
            _state.value = try {
                UiState(Load.Ready(repo.history()))
            } catch (e: ApiException) {
                // A card session may not read history (403 STEP_UP_REQUIRED): offer the password login instead of an error.
                if (e is ApiException.Http && e.code == ErrorCode.STEP_UP_REQUIRED) UiState(needsPassword = true)
                else UiState(Load.Failed(e.userMessage()))
            }
        }
    }
}

@Composable
fun HistoryScreen() {
    val vm = appViewModel { HistoryViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var askPassword by rememberSaveable { mutableStateOf(false) }

    if (state.needsPassword) {
        Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.history_needs_password))
                Button(onClick = { askPassword = true }) { Text(stringResource(R.string.history_login_password)) }
            }
        }
    } else {
        LoadView(state.items, onRetry = vm::load) { items ->
            if (items.isEmpty()) {
                Centered { Text(stringResource(R.string.history_empty)) }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items) { h ->
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(h.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                h.returnedAt?.let { stringResource(R.string.history_returned, formatDate(h.outAt), formatDate(it)) }
                                    ?: stringResource(R.string.history_not_returned, formatDate(h.outAt)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    if (askPassword) StepUpDialog(onDismiss = { askPassword = false }, onDone = { askPassword = false; vm.load() })
}
