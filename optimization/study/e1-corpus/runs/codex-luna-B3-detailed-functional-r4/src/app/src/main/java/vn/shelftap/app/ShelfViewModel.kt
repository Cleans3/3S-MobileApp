package vn.shelftap.app
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
data class ShelfState(val loans:List<Loan> = emptyList(),val history:List<HistoryItem> = emptyList(),val member:Member?=null,val offlineLoans:Boolean=false,val offlineHistory:Boolean=false,val loading:Boolean=false,val message:String?=null)
class ShelfViewModel(private val repo:ShelfTapRepository):ViewModel(){
 private val _state=MutableStateFlow(ShelfState(loading=true));val state=_state.asStateFlow()
 init{refresh()}
 fun refresh(){viewModelScope.launch{try{val l=repo.loans();val h=repo.history();val m=runCatching{repo.member()}.getOrNull();_state.value=ShelfState(l.first,h.first,m,l.second,h.second)}catch(e:Exception){_state.value=_state.value.copy(loading=false,message="Unable to load your library")}}}
 fun renew(x:Loan){viewModelScope.launch{try{val u=repo.renew(x.loanId);_state.value=_state.value.copy(loans=_state.value.loans.map{if(it.loanId==u.loanId)u else it})}catch(e:ShelfTapApi.ApiException){_state.value=_state.value.copy(message=if(e.code=="RENEWAL_NOT_ALLOWED")"This loan cannot be renewed" else "Unable to renew this loan")}catch(e:Exception){_state.value=_state.value.copy(message="Unable to renew this loan")}}}
 fun setHistory(v:Boolean){viewModelScope.launch{try{_state.value=_state.value.copy(member=repo.privacy(v))}catch(e:Exception){_state.value=_state.value.copy(message="Unable to update privacy settings")}}}
 fun clearMessage(){_state.value=_state.value.copy(message=null)}
}
class ShelfViewModelFactory(private val repo:ShelfTapRepository):androidx.lifecycle.ViewModelProvider.Factory{override fun <T:ViewModel>create(c:Class<T>):T=ShelfViewModel(repo) as T}
