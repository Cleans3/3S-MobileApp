package com.example.libraryloans

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.content.Context
import android.widget.Toast
import kotlin.math.min

data class Book(val title: String, val author: String, val due: String, val color: Int, val renewals: Int = 2, var renewed: Boolean = false)

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.setStatusBarColor(Color.rgb(251,249,244)); window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; setContentView(LibraryView(this)) }
}

private class LibraryView(context: Context) : View(context) {
    private val bg = Color.rgb(251,249,244); private val ink = Color.rgb(23,32,42); private val navy = Color.rgb(21,61,82); private val coral = Color.rgb(232,117,97); private val muted = Color.rgb(105,116,120)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG); private val loans = mutableListOf(Book("The Night Circus", "Erin Morgenstern", "Due in 3 days", Color.rgb(54,80,94)), Book("Tomorrow, and Tomorrow, and Tomorrow", "Gabrielle Zevin", "Due in 11 days", Color.rgb(204,116,89)))
    private val history = listOf(Book("Sea of Tranquility", "Emily St. John Mandel", "Returned 12 Oct 2025", Color.rgb(81,101,80)), Book("The Book Thief", "Markus Zusak", "Returned 28 Aug 2025", Color.rgb(185,137,70)), Book("Pachinko", "Min Jin Lee", "Returned 04 Jul 2025", Color.rgb(126,90,104)))
    private var tab = 0; private var detail: Book? = null; private var toast: String? = null
    private val density get() = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private fun text(s: String, x: Float, y: Float, size: Float, color: Int = ink, bold: Boolean = false) { paint.color=color; paint.textSize=dp(size); paint.typeface=if(bold) Typeface.create("sans",Typeface.BOLD) else Typeface.create("sans",Typeface.NORMAL); canvas?.drawText(s,dp(x),dp(y),paint) }
    private var canvas: Canvas? = null
    override fun onDraw(c: Canvas) { canvas=c; c.drawColor(bg); drawHeader(); if(tab==0) drawLoans() else drawHistory(); drawBottomNav(); detail?.let { drawDetail(it) }; canvas=null }
    private fun rect(l:Float,t:Float,r:Float,b:Float,color:Int,rad:Float=0f) { paint.color=color; paint.style=Paint.Style.FILL; canvas?.drawRoundRect(dp(l),dp(t),dp(r),dp(b),dp(rad),dp(rad),paint) }
    private fun stroke(l:Float,t:Float,r:Float,b:Float,color:Int,rad:Float=0f) { paint.color=color; paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(1f); canvas?.drawRoundRect(dp(l),dp(t),dp(r),dp(b),dp(rad),dp(rad),paint); paint.style=Paint.Style.FILL }
    private fun drawHeader() { text("MY LIBRARY",24f,34f,12f,coral,true); text("Hello, Alex",24f,68f,29f,navy,true); text("Keep your reading journey moving.",24f,92f,14f,muted); text("⋯",327f,58f,28f,navy,true) }
    private fun drawLoans() { text("Your loans",24f,135f,20f,ink,true); text("${loans.size} books borrowed",24f,157f,13f,muted); var y=177f; loans.forEach { drawBook(it,y,true); y+=158f }; if(loans.isEmpty()) { text("You have no active loans.",24f,y+20,16f,muted) } }
    private fun drawHistory() { text("Reading history",24f,135f,20f,ink,true); text("Books you have returned",24f,157f,13f,muted); var y=177f; history.forEach { drawBook(it,y,false); y+=130f } }
    private fun drawBook(b:Book,y:Float,active:Boolean) { rect(24f,y,376f,y+112f,Color.WHITE,16f); rect(38f,y+16f,92f,y+96f,b.color,7f); text("BOOK",51f,y+51f,10f,Color.WHITE,true); text("▰",58f,y+76f,18f,Color.WHITE); text(b.title,110f,y+34f,16f,ink,true); text(b.author,110f,y+56f,13f,muted); text(b.due,110f,y+82f,12f,if(active) coral else muted,true); if(active) { stroke(292f,y+72f,360f,y+98f,Color.rgb(220,226,225),13f); text("RENEW",305f,y+89f,10f,navy,true) } else text("›",350f,y+67f,28f,navy) }
    private fun drawBottomNav() { rect(0f,640f,400f,720f,Color.WHITE); rect(if(tab==0) 47f else 247f,650f,153f+(if(tab==0)0f else 200f),678f,Color.rgb(255,237,232),18f); text("▣",78f,670f,18f,if(tab==0)coral:muted); text("◷",278f,670f,19f,if(tab==1)coral:muted); text("Loans",69f,695f,11f,if(tab==0)navy:muted,true); text("History",263f,695f,11f,if(tab==1)navy:muted,true) }
    private fun drawDetail(b:Book) { rect(0f,0f,400f,720f,Color.argb(95,0,0,0)); rect(20f,132f,380f,610f,bg,22f); rect(42f,157f,145f,300f,b.color,10f); text("BOOK",67f,228f,12f,Color.WHITE,true); text("▰",80f,265f,26f,Color.WHITE); text("Book details",42f,340f,13f,coral,true); text(b.title,42f,373f,23f,navy,true); text(b.author,42f,399f,15f,muted); text("A thoughtful, immersive read from the",42f,445f,14f,ink); text("library collection.",42f,466f,14f,ink); stroke(42f,512f,358f,558f,coral,23f); text("CLOSE",179f,541f,12f,coral,true); text("×",340f,180f,27f,navy) }
    override fun onTouchEvent(e: android.view.MotionEvent): Boolean { if(e.action!=MotionEvent.ACTION_UP) return true; val x=e.x/density; val y=e.y/density; if(detail!=null) { if(y>490 || x>320&&y<210) { detail=null; invalidate() }; return true }; if(y>630) { tab=if(x>200)1 else 0; invalidate(); return true }; if(tab==0 && y>177 && y<335) { if(x>280&&y>240) { loans[0].renewed=true; loans[0].due="Due in 10 days"; Toast.makeText(context,"Loan renewed",Toast.LENGTH_SHORT).show(); invalidate() } else { detail=loans[0]; invalidate() }; return true }; if(tab==0&&y>=335&&y<500) { if(x>280&&y>400) { loans[1].renewed=true; loans[1].due="Due in 18 days"; Toast.makeText(context,"Loan renewed",Toast.LENGTH_SHORT).show(); invalidate() } else { detail=loans[1]; invalidate() }; return true }; if(tab==1&&y>177&&y<570) { val idx=((y-177)/130).toInt(); if(idx in history.indices) { detail=history[idx]; invalidate() } }; return true }
}
