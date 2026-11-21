package vn.shelftap.app
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.*
data class AppState(val loans:List<Loan> = emptyList(),val history:List<HistoryItem> = emptyList(),val member:Member?=null,val title:TitleDetails?=null,val loading:Boolean=false,val loansOffline:Boolean=false,val historyOffline:Boolean=false,val message:String?=null)
class MainViewModel(app:Application):AndroidViewModel(app){
 private val api=ShelfTapApi(app);private val _state=MutableStateFlow(AppState());val state=_state.asStateFlow()
 init{refresh()}
 fun refresh(){viewModelScope.launch{_state.value=_state.value.copy(loading=true);val l=api.loans();val h=api.history();val m=api.member();_state.value=AppState(loans=l.value?:emptyList(),history=h.value?:emptyList(),member=m.value,loading=false,loansOffline=l.offline,historyOffline=h.offline,message=l.error?:h.error)}}
 fun renew(id:String){viewModelScope.launch{val r=api.renew(id);if(r.value!=null)_state.value=_state.value.copy(loans=_state.value.loans.map{if(it.loanId==id)r.value!! else it},message=null)else _state.value=_state.value.copy(message=if(r.error=="RENEWAL_NOT_ALLOWED")"This loan cannot be renewed" else r.error)}}
 fun setHistory(enabled:Boolean){viewModelScope.launch{val r=api.setHistory(enabled);if(r.value!=null)_state.value=_state.value.copy(member=r.value,history=if(enabled)_state.value.history else emptyList(),message=null)else _state.value=_state.value.copy(message=r.error)}}
 fun loadTitle(id:String){viewModelScope.launch{val r=api.title(id);_state.value=_state.value.copy(title=r.value,message=r.error)}}
}
