package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ShelfTapApi(private val context:Context, private val auth:AuthRepository) {
    private val base="https://api.shelftap.example"
    private suspend fun request(path:String, method:String="GET", body:String?=null):String = withContext(Dispatchers.IO) {
        val c=URL(base+path).openConnection() as HttpURLConnection
        c.requestMethod=method; c.connectTimeout=10000; c.readTimeout=10000
        c.setRequestProperty("Accept","application/json"); c.setRequestProperty("Authorization","Bearer ${auth.accessToken()}")
        if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toByteArray())}}
        val stream=if(c.responseCode in 200..299)c.inputStream else c.errorStream
        val text=stream?.bufferedReader()?.use{it.readText()} ?: "{}"
        if(c.responseCode !in 200..299) throw ApiException(c.responseCode,runCatching{JSONObject(text).optString("code")}.getOrDefault(""))
        text
    }
    suspend fun loans()=request("/api/me/loans").let(::loansFrom)
    suspend fun history()=request("/api/me/history").let(::historyFrom)
    suspend fun member()=memberFrom(request("/api/me"))
    suspend fun setHistory(enabled:Boolean)=memberFrom(request("/api/me/privacy","PUT",JSONObject().put("historyEnabled",enabled).toString()))
    suspend fun renew(id:String)=loanFrom(request("/api/loans/$id/renew","POST"))
    suspend fun book(id:String)=bookFrom(request("/api/titles/$id"))
    fun save(key:String,value:String){context.getSharedPreferences("cache",0).edit().putString(key,value).apply()}
    fun cached(key:String)=context.getSharedPreferences("cache",0).getString(key,null)
    fun hasCache(key:String)=cached(key)!=null
    class ApiException(val status:Int,val code:String):Exception()
    companion object {
        fun loansFrom(s:String)=JSONArray(s).let{a->List(a.length()){loanFrom(a.getJSONObject(it))}}
        fun historyFrom(s:String)=JSONArray(s).let{a->List(a.length()){val o=a.getJSONObject(it);HistoryItem(o.optString("titleId"),o.optString("title"),o.optString("outAt"),o.optString("returnedAt").ifBlank{null})}}
        fun loanFrom(s:String)=loanFrom(JSONObject(s))
        fun loanFrom(o:JSONObject)=Loan(o.optString("loanId"),o.optString("copyId"),o.optString("titleId"),o.optString("title"),o.optString("barcode"),o.optString("outAt"),o.optString("dueAt"),o.optString("returnedAt").ifBlank{null},o.optInt("renewals"),o.optBoolean("renewable"))
        fun memberFrom(s:String)=memberFrom(JSONObject(s))
        fun memberFrom(o:JSONObject)=Member(o.optString("memberId"),o.optString("fullName"),o.optString("email"),o.optString("memberType"),o.optString("status"),o.optBoolean("historyEnabled"),o.optBoolean("mustChangePassword"))
        fun bookFrom(s:String)=bookFrom(JSONObject(s))
        fun bookFrom(o:JSONObject)=BookDetail(o.optString("titleId"),o.optString("isbn"),o.optString("title"),o.optJSONArray("authors").toStrings(),o.optJSONArray("subjects").toStrings(),o.optString("coverUrl").ifBlank{null},o.optInt("copiesAvailable"),o.optInt("copiesTotal"))
        private fun JSONArray?.toStrings()=if(this==null) emptyList() else List(length()){getString(it)}
    }
}
