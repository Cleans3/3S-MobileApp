package vn.shelftap.app.data
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
class ShelfTapApi(context:Context,private val auth:AuthRepository=SignedInAuthRepository()) {
 private val p=context.getSharedPreferences("shelftap_cache",0); private val base="https://api.shelftap.example"
 suspend fun loans()=list("/api/me/loans","loans",::loan); suspend fun history()=list("/api/me/history","history",::history)
 suspend fun member()=obj("GET","/api/me","member",::member); suspend fun title(id:String)=obj("GET","/api/titles/$id","title_$id",::title)
 suspend fun renew(id:String)=obj("POST","/api/loans/$id/renew","",::loan)
 suspend fun setHistory(on:Boolean):ApiResult<Member> = try { val r=call("PUT","/api/me/privacy",JSONObject().put("historyEnabled",on).toString()); val m=member(JSONObject(r)); p.edit().putString("member",r).apply(); ApiResult(m) } catch(e:Exception) { ApiResult(error=if(e is ApiException&&e.code=="RENEWAL_NOT_ALLOWED") e.code else e.message) }
 private suspend fun <T> list(path:String,key:String,parse:(JSONObject)->T):ApiResult<List<T>>=try{val a=JSONArray(call("GET",path));val v=(0 until a.length()).map{parse(a.getJSONObject(it))};p.edit().putString(key,a.toString()).apply();ApiResult(v)}catch(e:Exception){p.getString(key,null)?.let{val a=JSONArray(it);ApiResult((0 until a.length()).map{parse(a.getJSONObject(it))},offline=true)}?:ApiResult(error="Network unavailable")}
 private suspend fun <T> obj(method:String,path:String,key:String,parse:(JSONObject)->T):ApiResult<T>=try{val s=call(method,path);if(key.isNotEmpty())p.edit().putString(key,s).apply();ApiResult(parse(JSONObject(s)))}catch(e:Exception){p.getString(key,null)?.let{ApiResult(parse(JSONObject(it)),offline=true)}?:ApiResult(error=e.message)}
 private suspend fun call(method:String,path:String,body:String?=null):String{val c=URL(base+path).openConnection() as HttpURLConnection;c.requestMethod=method;c.connectTimeout=8000;c.readTimeout=8000;c.setRequestProperty("Authorization","Bearer ${auth.accessToken()}");c.setRequestProperty("Content-Type","application/json");if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toByteArray())}};val s=if(c.responseCode in 200..299)c.inputStream else c.errorStream;val t=s?.bufferedReader()?.use{it.readText()}?:"{}";if(c.responseCode !in 200..299)throw ApiException(c.responseCode,JSONObject(t).optString("code"));return t}
 private fun loan(o:JSONObject)=Loan(o.optString("loanId"),o.optString("copyId"),o.optString("titleId"),o.optString("title"),o.optString("barcode"),o.optString("outAt"),o.optString("dueAt"),o.optString("returnedAt").takeIf{it.isNotEmpty()},o.optInt("renewals"),o.optBoolean("renewable"))
 private fun history(o:JSONObject)=HistoryItem(o.optString("titleId"),o.optString("title"),o.optString("outAt"),o.optString("returnedAt"))
 private fun member(o:JSONObject)=Member(o.optString("memberId"),o.optString("fullName"),o.optString("email"),o.optString("memberType"),o.optString("status"),o.optBoolean("historyEnabled"),o.optBoolean("mustChangePassword"))
 private fun title(o:JSONObject)=TitleDetails(o.optString("titleId"),o.optString("isbn"),o.optString("title"),strings(o.optJSONArray("authors")),strings(o.optJSONArray("subjects")),o.optString("coverUrl").takeIf{it.isNotEmpty()},o.optInt("copiesAvailable"),o.optInt("copiesTotal"))
 private fun strings(a:JSONArray?)=(0 until(a?.length()?:0)).map{a!!.optString(it)}
 class ApiException(val status:Int,val code:String):Exception(code)
}
