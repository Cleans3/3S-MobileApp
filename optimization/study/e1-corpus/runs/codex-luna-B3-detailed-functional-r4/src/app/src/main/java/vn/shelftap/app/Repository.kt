package vn.shelftap.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShelfTapRepository(private val api:ShelfTapApi) {
    suspend fun loans():Pair<List<Loan>,Boolean> = load("loans",{api.loans()}){ShelfTapApi.loansFrom(it)}
    suspend fun history():Pair<List<HistoryItem>,Boolean> = load("history",{api.history()}){ShelfTapApi.historyFrom(it)}
    suspend fun member():Member=api.member()
    suspend fun privacy(v:Boolean)=api.setHistory(v)
    suspend fun renew(id:String)=api.renew(id)
    suspend fun book(id:String)=api.book(id)
    private suspend fun <T> load(key:String,remote:suspend()->T,parse:(String)->T):Pair<T,Boolean> = withContext(Dispatchers.IO){try{val x=remote();api.save(key,when(x){is List<*>-> when(x.firstOrNull()){is Loan->"["+x.joinToString(","){loanJson(it as Loan)}+"]";is HistoryItem->"["+x.joinToString(","){historyJson(it as HistoryItem)}+"]";else->"[]"};else->""});x to false}catch(e:Exception){api.cached(key)?.let{parse(it) to true} ?: throw e}}
    private fun loanJson(x:Loan)="{\"loanId\":\"${x.loanId}\",\"copyId\":\"${x.copyId}\",\"titleId\":\"${x.titleId}\",\"title\":\"${x.title}\",\"barcode\":\"${x.barcode}\",\"outAt\":\"${x.outAt}\",\"dueAt\":\"${x.dueAt}\",\"returnedAt\":${x.returnedAt?.let{"\"$it\""}?:"null"},\"renewals\":${x.renewals},\"renewable\":${x.renewable}}"
    private fun historyJson(x:HistoryItem)="{\"titleId\":\"${x.titleId}\",\"title\":\"${x.title}\",\"outAt\":\"${x.outAt}\",\"returnedAt\":${x.returnedAt?.let{"\"$it\""}?:"null"}}"
}
