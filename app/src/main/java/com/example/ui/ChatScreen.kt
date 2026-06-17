package com.example.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.chat.ChatMessage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val dbError by viewModel.dbError.collectAsState()
    val context = LocalContext.current

    if (dbError != null) {
        DatabaseErrorRecoveryView(
            errorMsg = dbError!!,
            onRepair = { viewModel.repairDatabaseAndRestart(context) }
        )
        return
    }

    val messages by viewModel.allMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val pendingAttachment by viewModel.pendingAttachment.collectAsState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setPendingAttachment(uri, context)
        }
    }

    // Auto scroll down when messages size changes or a new message/loading arrives
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightSlate),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Outer glowing circular border
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(NeonPink, Color.Transparent)))
                                .padding(2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_nicole_avatar),
                                contentDescription = "NicolePorchetta Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column {
                            Text(
                                text = "NicolePorchetta AI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite,
                                fontFamily = FontFamily.SansSerif
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GlowNeon)
                                )
                                Text(
                                    text = "18 anni, Barese DOC 🍕",
                                    fontSize = 11.sp,
                                    color = SoftPink,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Cancella cronologia",
                                tint = NeonPink
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalDark.copy(alpha = 0.95f),
                    titleContentColor = SoftWhite
                ),
                modifier = Modifier.shadow(8.dp)
            )
        },
        bottomBar = {
            // Interactive Bottom Input Deck with strict navigation/notch guidelines
            Surface(
                color = CharcoalDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Keep clear of OS navigation gestures
            ) {
                Column {
                    Divider(color = GlassWhite, thickness = 0.5.dp)

                    // Model selection dropdown for power plans
                    val selectedModelId by viewModel.selectedModel.collectAsState()
                    val modelOptions = viewModel.modelOptions
                    val currentOption = modelOptions.find { it.id == selectedModelId } ?: modelOptions[0]
                    var expanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Modello",
                                tint = SoftPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Piano di potenza (Modello):",
                                fontSize = 11.sp,
                                color = SoftPink,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box {
                            Surface(
                                color = CardSlate,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, GlassWhite),
                                modifier = Modifier
                                    .clickable { expanded = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = currentOption.name,
                                        color = SoftWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (expanded) "▲" else "▼",
                                        color = SoftPink,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .background(CharcoalDark)
                                    .border(0.5.dp, GlassWhite, RoundedCornerShape(8.dp))
                                    .widthIn(max = 280.dp)
                            ) {
                                modelOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.name,
                                                color = if (option.id == selectedModelId) SoftPink else SoftWhite,
                                                fontSize = 12.sp,
                                                fontWeight = if (option.id == selectedModelId) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectModel(option.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = GlassWhite.copy(alpha = 0.25f), thickness = 0.5.dp)

                    // Attached media preview row
                    AnimatedVisibility(visible = pendingAttachment != null) {
                        pendingAttachment?.let { att ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSlate)
                                    .border(1.dp, GlassWhite, RoundedCornerShape(12.dp))
                            ) {
                                AttachmentView(
                                    uriString = att.uri,
                                    mimeType = att.type,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Clear button
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clickable { viewModel.clearPendingAttachment() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Rimuovi allegato",
                                        tint = SoftWhite,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Plus (+) attachment select button
                        IconButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aggiungi materiale visivo",
                                tint = SoftPink
                            )
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Chiedi a NicolePorchetta... 🍕",
                                    color = MutedSlate,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 120.dp)
                                .testTag("msg_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CardSlate,
                                unfocusedContainerColor = CardSlate,
                                focusedTextColor = SoftWhite,
                                unfocusedTextColor = SoftWhite,
                                cursorColor = NeonPink,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() || pendingAttachment != null) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            maxLines = 4
                        )

                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank() || pendingAttachment != null) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    focusManager.clearFocus()
                                }
                            },
                            containerColor = NeonPink,
                            contentColor = SoftWhite,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Invia messaggio",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MidnightSlate
        ) {
            if (messages.isEmpty() && !isLoading) {
                EmptyChatView(onSuggestionSelected = { suggestion ->
                    viewModel.sendMessage(suggestion)
                })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        ChatBubbleRow(message)
                    }

                    if (isLoading) {
                        item {
                            TypingIndicatorRow()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatView(
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High fidelity central hero illustration
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonPink, AccentPeach)))
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_nicole_avatar),
                contentDescription = "Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NicolePorchetta AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SoftWhite,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Una splendida diciottenne di Bari con la passione viscerale per il cibo pugliese e un rifiuto totale per lo sport, dotata però di un cervello geniale e super performante!",
            fontSize = 14.sp,
            color = MutedSlate,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Idee per stimolare la conversazione:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SoftPink,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val suggestions = listOf(
            "Scrivi una poesia barese sul sushi e la gioia di stare sul divano! 🍣🛌",
            "La ricetta perfetta per fare la pasta all'assassina barese bollente e croccante! 🌶️🔥",
            "Spiegami perché odi così tanto la palestra e l'attività fisica!"
        )

        suggestions.forEach { suggestion ->
            Card(
                onClick = { onSuggestionSelected(suggestion) },
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(0.5.dp, GlassWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Idea",
                        tint = SoftPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = suggestion,
                        fontSize = 13.sp,
                        color = SoftWhite,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleRow(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!message.isUser) {
                // Symmetrical small circular avatar
                Image(
                    painter = painterResource(id = R.drawable.img_nicole_avatar),
                    contentDescription = "NicolePorchetta Mini Profile",
                    modifier = Modifier
                        .padding(end = 8.dp, top = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            val bubbleColor = when {
                message.isUser -> NeonPink
                message.isError -> OrangeFlame.copy(alpha = 0.15f)
                else -> CardSlate
            }

            val bubbleBorder = when {
                message.isError -> BorderStroke(1.dp, OrangeFlame)
                !message.isUser -> BorderStroke(0.5.dp, GlassWhite)
                else -> null
            }

            val bubbleShape = if (message.isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
            }

            Surface(
                color = bubbleColor,
                border = bubbleBorder,
                shape = bubbleShape,
                tonalElevation = if (message.isUser) 0.dp else 2.dp,
                modifier = Modifier.shadow(4.dp, shape = bubbleShape)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.isError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Errore",
                                tint = OrangeFlame,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ATTENZIONE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeFlame,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (message.attachmentUri != null && message.attachmentType != null) {
                        AttachmentView(
                            uriString = message.attachmentUri,
                            mimeType = message.attachmentType,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(bottom = 8.dp)
                        )
                    }

                    if (message.generatedImageBase64 != null) {
                        Base64Image(
                            base64Str = message.generatedImageBase64,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(bottom = 8.dp)
                        )
                    }

                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            color = if (message.isError) OrangeFlame else SoftWhite,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorRow(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicator")
    
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.3f at 0
                1f at 150
                0.3f at 300
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.3f at 100
                1f at 250
                0.3f at 400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.3f at 200
                1f at 350
                0.3f at 500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_nicole_avatar),
            contentDescription = "NicolePorchetta Mini Profile",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Surface(
            color = CardSlate,
            border = BorderStroke(0.5.dp, GlassWhite),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp),
            modifier = Modifier.shadow(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SoftPink.copy(alpha = dot1Scale))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SoftPink.copy(alpha = dot2Scale))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SoftPink.copy(alpha = dot3Scale))
                )
            }
        }
    }
}

@Composable
fun Base64Image(base64Str: String, modifier: Modifier = Modifier) {
    val decodedBytes = remember(base64Str) {
        try {
            Base64.decode(base64Str, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
    val imageBitmap = remember(decodedBytes) {
        if (decodedBytes != null) {
            try {
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Immagine generata",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(CardSlate),
            contentAlignment = Alignment.Center
        ) {
            Text("Impossibile caricare l'immagine", color = OrangeFlame, fontSize = 12.sp)
        }
    }
}

@Composable
fun VideoPlayer(uriString: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse(uriString))
                val mediaController = MediaController(context)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f) // Mute on loop inside bubble
                    start()
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun AttachmentView(uriString: String, mimeType: String, modifier: Modifier = Modifier) {
    if (mimeType.startsWith("image/")) {
        AsyncImage(
            model = uriString,
            contentDescription = "Allegato immagine",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (mimeType.startsWith("video/")) {
        VideoPlayer(uriString = uriString, modifier = modifier)
    } else {
        Box(
            modifier = modifier.background(CardSlate),
            contentAlignment = Alignment.Center
        ) {
            Text("File allegato (${mimeType})", color = SoftWhite, fontSize = 11.sp)
        }
    }
}

@Composable
fun DatabaseErrorRecoveryView(
    errorMsg: String,
    onRepair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MidnightSlate
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(OrangeFlame, Color.Transparent)))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🌶️", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Madonna mia, qualcosa è andato storto! 💔",
                fontSize = 20.sp,
                color = SoftWhite,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NicolePorchetta AI non riesce ad avviare il suo database locale. Probabilmente vi è un conflitto di memoria con una vecchia versione installata sul telefono uagliò!",
                fontSize = 13.sp,
                color = MutedSlate,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(0.5.dp, OrangeFlame.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DETTAGLI TECNICI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeFlame,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SoftWhite,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRepair,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            ) {
                Text(
                    text = "🛠️ Ripristina & Ripara l'App",
                    color = SoftWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Nota: Questo svuoterà la chat corrotta ed avvierà l'applicazione in modo pulito e sicuro uagliò! 🍕💅",
                fontSize = 11.sp,
                color = MutedSlate,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


