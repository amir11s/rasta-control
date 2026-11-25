package com.rasta.control

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class LockActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pkg = intent.getStringExtra("pkg") ?: ""
        val name = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { "این برنامه" }
        setContent {
            RastaTheme {
                Column(
                    Modifier.fillMaxSize().background(Navy).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Image(painterResource(R.drawable.logo), null, Modifier.size(110.dp).clip(CircleShape))
                    Spacer(Modifier.height(28.dp))
                    Text("🔒", fontSize = 44.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("زمان امروزِ $name تموم شد", color = Gold, fontSize = 22.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("فردا دوباره باز میشه.\nمسیر درست، نتیجه‌ای رستا", color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 26.sp)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { goHome(this@LockActivity); finish() },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)) { Text("برگشت به صفحه اصلی") }
                }
            }
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { goHome(this); finish() }
}
