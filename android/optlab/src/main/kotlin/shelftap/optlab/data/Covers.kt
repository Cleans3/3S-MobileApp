package shelftap.optlab.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import java.io.File
import kotlin.random.Random

/** Locally generated covers: no network. 1500x2250 on purpose, a plausible scanned/hero size that is far larger than a list thumbnail. */
object Covers {
    const val W = 1500
    const val H = 2250

    fun file(ctx: Context, idx: Int) = File(ctx.filesDir, "covers/c$idx.jpg")

    /** Writes any missing cover files; returns how long it took in ms. */
    fun ensure(ctx: Context): Long {
        val t0 = System.nanoTime()
        file(ctx, 0).parentFile!!.mkdirs()
        for (i in 0 until BookGenerator.COVERS) {
            val f = file(ctx, i)
            if (f.exists()) continue
            val bmp = draw(i)
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bmp.recycle()
        }
        return (System.nanoTime() - t0) / 1_000_000
    }

    private fun draw(idx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Random(idx)
        val hue = idx * 360f / BookGenerator.COVERS
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, W.toFloat(), H.toFloat(),
            Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.9f)), Color.HSVToColor(floatArrayOf((hue + 40) % 360, 0.75f, 0.4f)),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
        paint.shader = null
        // Detail so the JPEG is not trivially small: 600 translucent shapes.
        repeat(600) {
            paint.color = Color.HSVToColor(r.nextInt(40, 110), floatArrayOf((hue + r.nextInt(-60, 60) + 360) % 360, 0.5f, r.nextFloat()))
            val x = r.nextFloat() * W
            val y = r.nextFloat() * H
            if (it % 2 == 0) c.drawCircle(x, y, 20f + r.nextFloat() * 160f, paint) else c.drawRect(x, y, x + 40f + r.nextFloat() * 300f, y + 20f + r.nextFloat() * 120f, paint)
        }
        paint.color = Color.argb(200, 255, 255, 255)
        c.drawRect(100f, H * 0.62f, W - 100f, H * 0.62f + 260f, paint)
        return bmp
    }
}
