package com.example

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.theme.*
import com.example.utils.CrashHandler

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize global crash interceptor before anything else executes!
        CrashHandler.init(this)
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val lastCrash = CrashHandler.getLastCrash(this)

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
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(OrangeFlame, Color.Transparent)))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌶️", fontSize = 36.sp)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Uè uagliò! Errore di Nicole! 💔💅",
                                fontSize = 18.sp,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "L'applicazione si è arrestata improvvisamente. Puoi copiare i dettagli dell'errore qui sotto per aiutarci, oppure tentare un ripristino istantaneo!",
                                fontSize = 13.sp,
                                color = MutedSlate,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardSlate),
                                border = BorderStroke(0.5.dp, OrangeFlame.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "LOG ERRORE (Invialo a noi!)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeFlame,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = lastCrash,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SoftWhite,
                                        lineHeight = 15.sp,
                                        maxLines = 12,
                                        modifier = Modifier.verticalScroll(rememberScrollState())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    try {
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Nicole Crash Log", lastCrash)
                                        clipboard.setPrimaryClip(clip)
                                        copied = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (copied) GlowNeon else SoftPink),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (copied) "✓ Errore Copiato in Clipboard" else "📋 Copia Log Errore",
                                    color = MidnightSlate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    try {
                                        deleteDatabase("nicoleporchetta_chat_db")
                                        val sharedPrefs = getSharedPreferences("nicole_prefs", Context.MODE_PRIVATE)
                                        sharedPrefs.edit().clear().commit()
                                        CrashHandler.clearLastCrash(this@MainActivity)

                                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        startActivity(intent)
                                        finish()
                                        Process.killProcess(Process.myPid())
                                        java.lang.System.exit(0)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .shadow(8.dp, RoundedCornerShape(24.dp))
                            ) {
                                Text(
                                    text = "🛠️ Ripristina & Ripara l'App",
                                    color = SoftWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tranquillo uagliò, riparando l'app potrai ricominciare a parlare con NicolePorchetta in men che non si dica! 🍕💅",
                                fontSize = 11.sp,
                                color = MutedSlate,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
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
    }
}
