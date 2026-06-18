package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.theme.*
import com.example.utils.CrashHandler
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashHandler.init(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val lastCrash = CrashHandler.getLastCrash(this)

        try {
            setContent {
                MyApplicationTheme {
                    if (lastCrash != null) {
                        var copied by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MidnightSlate
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Uè uagliò! Errore di Nicole! 💔",
                                    fontSize = 18.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFFF0F0FA),
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E34)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lastCrash,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = androidx.compose.ui.graphics.Color(0xFFF0F0FA),
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        try {
                                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("crash", lastCrash))
                                            copied = true
                                        } catch (e: Exception) { }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFB1C5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (copied) "✓ Copiato!" else "📋 Copia errore", color = androidx.compose.ui.graphics.Color(0xFF0F0F1A))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        try {
                                            deleteDatabase("nicoleporchetta_chat_db")
                                            getSharedPreferences("nicole_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                                            CrashHandler.clearLastCrash(this@MainActivity)
                                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            startActivity(intent)
                                            finish()
                                            Process.killProcess(Process.myPid())
                                        } catch (e: Exception) { }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF6D95)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🛠️ Ripristina App", color = androidx.compose.ui.graphics.Color(0xFFF0F0FA))
                                }
                            }
                        }
                    } else {
                        ChatScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            // Compose itself crashed — show error with pure Android Views (no Compose dependency)
            showNativeCrashScreen(e)
        }
    }

    private fun showNativeCrashScreen(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val crashText = sw.toString()

        // Save crash to prefs
        try {
            val prefs = getSharedPreferences("nicole_crash_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("last_crash", crashText).commit()
        } catch (_: Exception) {}

        val dp = { v: Int -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "CRASH DIAGNOSTICA\nComposable Compose fallita all'avvio"
            setTextColor(Color.parseColor("#FF6D95"))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val crashLog = TextView(this).apply {
            text = crashText
            setTextColor(Color.WHITE)
            textSize = 11f
            setBackgroundColor(Color.parseColor("#1E1E34"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val copyBtn = Button(this).apply {
            text = "Copia Log Errore"
            setBackgroundColor(Color.parseColor("#FFB1C5"))
            setTextColor(Color.parseColor("#0F0F1A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(16), 0, dp(8)) }
            setOnClickListener {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("crash", crashText))
                    text = "✓ Copiato negli appunti!"
                } catch (_: Exception) {}
            }
        }

        scrollView.addView(crashLog)
        root.addView(title)
        root.addView(scrollView)
        root.addView(copyBtn)

        setContentView(root)
    }
}
