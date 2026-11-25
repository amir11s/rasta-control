package com.rasta.control

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import java.util.Calendar

val Navy = Color(0xFF14233F)
val Gold = Color(0xFFC8962E)
val Cream = Color(0xFFFBF8F2)
val Mist = Color(0xFFE6E2EC)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("rasta", 0)
    fun limits(c: Context): Map<String, Int> =
        p(c).all.filter { it.key.startsWith("l_") && it.value is Int }
            .map { it.key.removePrefix("l_") to it.value as Int }.toMap()
    fun set(c: Context, pkg: String, min: Int?) {
        val e = p(c).edit()
        if (min == null) e.remove("l_$pkg") else e.putInt("l_$pkg", min)
        e.apply()
    }
}

object Usage {
    @Suppress("DEPRECATION")
    fun granted(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) == AppOpsManager.MODE_ALLOWED
    }
    fun accessibilityOn(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains(c.packageName + "/")
    }
    /** مدت استفاده از امروز ساعت ۰۰:۰۰ تا الان (میلی‌ثانیه) */
    fun todayMs(c: Context, pkg: String): Long {
        val um = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        val ev = um.queryEvents(cal.timeInMillis, now)
        val e = UsageEvents.Event()
        var total = 0L
        var resumed = -1L
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.packageName != pkg) continue
            when (e.eventType) {
                1 -> if (resumed < 0) resumed = e.timeStamp
                2 -> if (resumed > 0) { total += e.timeStamp - resumed; resumed = -1 }
            }
        }
        if (resumed > 0) total += now - resumed
        return total
    }
}

fun goHome(c: Context) {
    c.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun RastaTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val ff = remember {
        try { FontFamily(Typeface.createFromAsset(ctx.assets, "vazir.ttf")) } catch (t: Throwable) { FontFamily.Default }
    }
    val b = Typography()
    val typo = Typography(
        b.displayLarge.copy(fontFamily = ff), b.displayMedium.copy(fontFamily = ff), b.displaySmall.copy(fontFamily = ff),
        b.headlineLarge.copy(fontFamily = ff), b.headlineMedium.copy(fontFamily = ff), b.headlineSmall.copy(fontFamily = ff),
        b.titleLarge.copy(fontFamily = ff), b.titleMedium.copy(fontFamily = ff), b.titleSmall.copy(fontFamily = ff),
        b.bodyLarge.copy(fontFamily = ff), b.bodyMedium.copy(fontFamily = ff), b.bodySmall.copy(fontFamily = ff),
        b.labelLarge.copy(fontFamily = ff), b.labelMedium.copy(fontFamily = ff), b.labelSmall.copy(fontFamily = ff)
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = lightColorScheme(primary = Navy, onPrimary = Cream, secondary = Gold, background = Cream, surface = Color.White, onSurface = Navy),
            typography = typo
        ) { Box(Modifier.fillMaxSize()) { content() } }
    }
}
