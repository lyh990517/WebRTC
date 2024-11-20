package com.example.call.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.call.state.CallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface ChatMessage {
    val type: ChatType

    data class Message(
        override val type: ChatType,
        val message: String,
    ) : ChatMessage

    data class Image(
        override val type: ChatType,
        val images: List<ImageBitmap>,
    ) : ChatMessage

    data class File(
        override val type: ChatType,
        val files: List<java.io.File>,
    ) : ChatMessage

    enum class ChatType {
        ME,
        OTHER
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chatting(
    state: CallState.Success,
    onMessage: (ChatMessage) -> Unit,
    onInputChange: () -> Unit,
    onInputStopped: () -> Unit,
    onToggleChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    var showNewMessageNotification by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.otherUserOnInput) {
        if (state.otherUserOnInput) {
            delay(800)
            onInputStopped()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Chatting", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onToggleChat) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { chatMessage ->
                    val isSent = chatMessage.type == ChatMessage.ChatType.ME

                    when (chatMessage) {
                        is ChatMessage.File -> {

                        }

                        is ChatMessage.Image -> {

                        }

                        is ChatMessage.Message -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = if (isSent) listOf(
                                                    Color(0xFF9CE09F), Color(0xFF60BE7B)
                                                ) else listOf(
                                                    Color(0xFF4692E1), Color(0xFF1C73D1)
                                                )
                                            ),
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isSent) 12.dp else 0.dp,
                                                bottomEnd = if (isSent) 0.dp else 12.dp
                                            )
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .widthIn(max = 250.dp)
                                ) {
                                    Text(
                                        text = chatMessage.message,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(state.messages.size) {
                if (lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
                    showNewMessageNotification =
                        lastVisibleItem != null
                                && lastVisibleItem.index < state.messages.size - 1
                }
            }

            Column {
                AnimatedVisibility(
                    visible = state.otherUserOnInput,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "other user on input",
                                color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showNewMessageNotification,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val lastChat = state.messages[state.messages.size - 1]

                            when (lastChat) {
                                is ChatMessage.File -> {
                                    Text(
                                        text = "File Received",
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = {
                                        scope.launch {
                                            lazyListState.animateScrollToItem(state.messages.size - 1)
                                        }
                                        showNewMessageNotification = false
                                    }) {
                                        Text("View", color = Color.White)
                                    }
                                }

                                is ChatMessage.Image -> {
                                    Text(
                                        text = "Image Received",
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = {
                                        scope.launch {
                                            lazyListState.animateScrollToItem(state.messages.size - 1)
                                        }
                                        showNewMessageNotification = false
                                    }) {
                                        Text("View", color = Color.White)
                                    }
                                }

                                is ChatMessage.Message -> {
                                    Text(
                                        text = lastChat.message,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = {
                                        scope.launch {
                                            lazyListState.animateScrollToItem(state.messages.size - 1)
                                        }
                                        showNewMessageNotification = false
                                    }) {
                                        Text("View", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                MessageInputUi(
                    onMessage = { message ->
                        when (message) {
                            is ChatMessage.File -> {

                            }

                            is ChatMessage.Image -> {
                                scope.launch {
                                    val isAtBottom =
                                        lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == state.messages.size - 1
                                    onMessage(message)

                                    if (isAtBottom) {
                                        lazyListState.animateScrollToItem(state.messages.size - 1)
                                    }
                                }
                            }

                            is ChatMessage.Message -> {
                                if (message.message.isNotBlank()) {
                                    scope.launch {
                                        val isAtBottom =
                                            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == state.messages.size - 1
                                        onMessage(message)

                                        if (isAtBottom) {
                                            lazyListState.animateScrollToItem(state.messages.size - 1)
                                        }
                                    }
                                }
                            }
                        }
                    }, onInputChange = onInputChange
                )
            }
        }
    }
}


@Composable
private fun MessageInputUi(onMessage: (ChatMessage) -> Unit, onInputChange: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var isAdditionalUiVisible by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImage = uris.mapNotNull { uri ->
            val stream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isAdditionalUiVisible) {
                        isAdditionalUiVisible = false
                    } else {
                        isAdditionalUiVisible = true
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = if (isAdditionalUiVisible) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isAdditionalUiVisible) "Close" else "Add",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    onInputChange()
                },
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    when {
                        selectedImage.isNotEmpty() -> {
                            onMessage(
                                ChatMessage.Image(
                                    type = ChatMessage.ChatType.ME,
                                    images = selectedImage
                                )
                            )
                        }

                        else -> {
                            if (message.isNotBlank()) {
                                onMessage(
                                    ChatMessage.Message(
                                        type = ChatMessage.ChatType.ME,
                                        message = message
                                    )
                                )
                                message = ""
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Send",
                        style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isAdditionalUiVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)) + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Select Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image")
                    }

                    Button(
                        onClick = {
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Select File")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select File")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImage) { image ->
                        Image(
                            bitmap = image,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun MessageInputUiPreview() {
    MessageInputUi({}) { }
}
