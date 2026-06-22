package com.example.aituner.ui.screen

import com.example.aituner.R

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aituner.ui.theme.*
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.chat_title), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.chat_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎸", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.chat_greeting),
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                listOf(
                                    stringResource(R.string.chat_suggestion_1),
                                    stringResource(R.string.chat_suggestion_2),
                                    stringResource(R.string.chat_suggestion_3)
                                ).forEach { suggestion ->
                                    SuggestionChip(suggestion) {
                                        inputText = suggestion
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                items(uiState.messages) { msg ->
                    MessageBubble(message = msg)
                }

                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .padding(start = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.chat_thinking), color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Error banner
            if (uiState.error != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = AccentRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            uiState.error!!,
                            color = AccentRed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, stringResource(R.string.chat_close), tint = AccentRed)
                        }
                    }
                }
            }

            // Input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(stringResource(R.string.chat_hint), color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardDark,
                            unfocusedContainerColor = CardDark,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !uiState.isLoading) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Send,
                            stringResource(R.string.chat_send),
                            tint = if (inputText.isNotBlank()) AccentBlue else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardDark,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MessageBubble(message: AiChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) AccentBlue.copy(alpha = 0.2f) else CardDark,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (message.isStreaming) {
                    Text(
                        text = " ▍",
                        fontSize = 14.sp,
                        color = AccentCyan
                    )
                }
            }
        }
    }
}
