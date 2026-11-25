package com.rasta.control

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

data class AppInfo(val pkg: String, val label: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent { RastaTheme { Home() } }
    }
}

fun loadApps(c: Context): List<AppInfo> {
    val pm = c.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
        .filter { it.pkg != c.packageName }.distinctBy { it.pkg }.sortedBy { it.label }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var ver by remember { mutableIntStateOf(0) }
    var sel by remember { mutableStateOf<AppInfo?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    LaunchedEffect(Unit) { while (true) { delay(10_000); tick++ } }
    val apps = remember { loadApps(ctx) }
    val limits = remember(tick, ver) { Store.limits(ctx) }
    val usageOk = remember(tick) { Usage.granted(ctx) }
    val accOk = remember(tick) { Usage.accessibilityOn(ctx) }
    val sorted = remember(limits, apps) { apps.sortedBy { if (limits.containsKey(it.pkg)) 0 else 1 } }

    LazyColumn(
        Modifier.fillMaxSize().background(Cream).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.logo), null, Modifier.size(110.dp).clip(CircleShape))
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Rasta ", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = Navy)
                    Text("Control", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = Gold)
                }
                Text("رستا کنترل", fontSize = 18.sp, color = Navy)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(120.dp).height(2.dp).background(Gold))
                Spacer(Modifier.height(6.dp))
                Text("مسیر درست، نتیجه‌ای رستا", fontSize = 13.sp, color = Navy.copy(alpha = .7f))
            }
        }
        if (!usageOk) item {
            PermCard("۱) دسترسی به آمار استفاده", "برای محاسبه‌ی زمان استفاده از هر برنامه") {
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
        if (!accOk) item {
            PermCard("۲) فعال‌سازی سرویس دسترسی‌پذیری", "«رستا کنترل» را پیدا و روشن کنید تا برنامه‌ها قفل شوند") {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        item { Text("برنامه‌ها را انتخاب کن و زمان مجاز روزانه‌اش را بگذار", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        items(sorted, key = { it.pkg }) { app ->
            val lim = limits[app.pkg]
            val used = remember(tick, ver, app.pkg, usageOk) { if (lim != null && usageOk) Usage.todayMs(ctx, app.pkg) / 60_000 else 0L }
            val locked = lim != null && used >= lim
            val bmp = remember(app.pkg) { app.icon.toBitmap(96, 96).asImageBitmap() }
            Card(
                onClick = { sel = app }, shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(if (lim != null) 1.5.dp else 1.dp, if (lim != null) Gold else Mist),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(bmp, null, Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontWeight = FontWeight.Bold, color = Navy, maxLines = 1)
                        Text(
                            if (lim == null) "بدون محدودیت" else "امروز $used از $lim دقیقه",
                            fontSize = 12.sp, color = if (locked) Color(0xFFB3261E) else Gold
                        )
                    }
                    if (locked) Text("🔒", fontSize = 20.sp)
                }
            }
        }
    }

    sel?.let { app ->
        var text by remember(app.pkg) { mutableStateOf((limits[app.pkg] ?: 60).toString()) }
        AlertDialog(
            onDismissRequest = { sel = null },
            containerColor = Cream,
            title = { Text(app.label, color = Navy) },
            text = {
                Column {
                    Text("چند دقیقه در روز مجاز است؟ بعد از آن تا فردا قفل می‌شود.", fontSize = 13.sp, color = Navy)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.filter(Char::isDigit).take(4) }, singleLine = true,
                        label = { Text("دقیقه") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 30, 60, 120).forEach { m ->
                            AssistChip(onClick = { text = m.toString() }, label = { Text("$m") },
                                border = AssistChipDefaults.assistChipBorder(true, borderColor = Gold))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let { Store.set(ctx, app.pkg, it); ver++ }
                    sel = null
                }) { Text("ذخیره", color = Navy, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { Store.set(ctx, app.pkg, null); ver++; sel = null }) {
                    Text(if (limits.containsKey(app.pkg)) "حذف محدودیت" else "انصراف", color = Gold)
                }
            }
        )
    }
}

@Composable
fun PermCard(title: String, sub: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Navy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Gold, fontWeight = FontWeight.Bold)
            Text(sub, color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)) { Text("باز کردن تنظیمات") }
        }
    }
}
