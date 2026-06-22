package com.example.aituner.ui.screen

import com.example.aituner.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aituner.ai.AiConfig
import com.example.aituner.ai.AiProviderId
import com.example.aituner.ai.AiSettingsRepository
import com.example.aituner.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

data class ProviderPreset(
    val id: AiProviderId,
    val name: String,
    val description: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val models: List<String>
)

val PROVIDER_PRESETS = listOf(
    ProviderPreset(AiProviderId.OLLAMA, "Ollama (本地)", "离线运行，需安装 Ollama", "http://localhost:11434", "llama3.2", listOf("llama3.2", "llama3.1", "llama3", "qwen2.5", "mistral", "phi3")),
    ProviderPreset(AiProviderId.OPENAI, "OpenAI", "ChatGPT / GPT-4o", "https://api.openai.com", "gpt-4o-mini", listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo")),
    ProviderPreset(AiProviderId.CLAUDE, "Claude", "Anthropic Claude", "https://api.anthropic.com", "claude-3-haiku-20240307", listOf("claude-3-5-sonnet-20241022", "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307")),
    ProviderPreset(AiProviderId.CUSTOM, "自定义", "任何 OpenAI 兼容 API", "https://your-api.com", "gpt-3.5-turbo", listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    aiSettingsRepo: AiSettingsRepository,
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var currentConfig by remember { mutableStateOf<AiConfig?>(null) }
    LaunchedEffect(Unit) {
        currentConfig = aiSettingsRepo.config.first()
    }

    var selectedPreset by remember { mutableStateOf(currentConfig?.providerId ?: AiProviderId.CUSTOM) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig?.baseUrl ?: "") }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig?.apiKey ?: "") }
    var model by remember(currentConfig) { mutableStateOf(currentConfig?.model ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    val copyFn = rememberCopyToClipboard()

    // When preset changes, update defaults
    LaunchedEffect(selectedPreset) {
        val preset = PROVIDER_PRESETS.find { it.id == selectedPreset }
        if (preset != null) {
            baseUrl = preset.defaultBaseUrl
            if (model.isBlank()) model = preset.defaultModel
        }
        if (selectedPreset == AiProviderId.OLLAMA) {
            apiKey = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.chat_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // ─── Provider Selection ───
            Text(
                stringResource(R.string.settings_provider),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            PROVIDER_PRESETS.forEach { preset ->
                val isSelected = selectedPreset == preset.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedPreset = preset.id
                            focusManager.clearFocus()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AccentBlue.copy(alpha = 0.15f) else CardDark,
                    border = if (isSelected)
                        androidx.compose.foundation.BorderStroke(2.dp, AccentBlue)
                    else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (preset.id) {
                                AiProviderId.OLLAMA -> Icons.Default.Computer
                                AiProviderId.OPENAI -> Icons.Default.Hub
                                AiProviderId.CLAUDE -> Icons.Default.AutoAwesome
                                AiProviderId.CUSTOM -> Icons.Default.Tune
                            },
                            contentDescription = null,
                            tint = if (isSelected) AccentBlue else TextSecondary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                preset.name,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                preset.description,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPreset = preset.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentBlue
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Config fields ───
            Text(
                stringResource(R.string.settings_connection),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.settings_base_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = darkFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(12.dp))

            // Model Dropdown
            var modelExpanded by remember { mutableStateOf(false) }
            val presetModels = PROVIDER_PRESETS.find { it.id == selectedPreset }?.models ?: emptyList()

            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.settings_model)) },
                    readOnly = false,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = darkFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    presetModels.forEach { modelOption ->
                        DropdownMenuItem(
                            text = { Text(modelOption) },
                            onClick = {
                                model = modelOption
                                modelExpanded = false
                            }
                        )
                    }
                    // 允许手动输入
                    DropdownMenuItem(
                        text = { Text("手动输入...", color = TextMuted) },
                        onClick = {
                            modelExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(if (selectedPreset == AiProviderId.OLLAMA) stringResource(R.string.settings_api_key_optional) else stringResource(R.string.settings_api_key)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = darkFieldColors(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            stringResource(R.string.settings_show),
                            tint = TextSecondary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            if (selectedPreset == AiProviderId.OLLAMA) {
                Spacer(Modifier.height(8.dp))
                val ollamaModel = if (model.isBlank()) "llama3.2" else model
                Text(
                    text = stringResource(R.string.settings_ollama_hint, ollamaModel),
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        aiSettingsRepo.save(
                            AiConfig(
                                providerId = selectedPreset,
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                model = model.ifBlank { "gpt-3.5-turbo" }
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // ─── Crash Log ───
            Divider(color = TextMuted.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            var showCrashLog by remember { mutableStateOf(false) }
            var crashLog by remember { mutableStateOf("") }
            val noCrashMsg = stringResource(R.string.settings_no_crash)

            OutlinedButton(
                onClick = {
                    crashLog = com.example.aituner.AiTunerApp.readLastCrash() ?: noCrashMsg
                    showCrashLog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.BugReport, null, tint = TextMuted)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_crash_log), color = TextSecondary)
            }

            Spacer(Modifier.height(8.dp))

            // 调试日志按钮
            var showDebugLog by remember { mutableStateOf(false) }
            var debugLog by remember { mutableStateOf("") }
            val noLogMsg = stringResource(R.string.settings_no_log)

            OutlinedButton(
                onClick = {
                    debugLog = com.example.aituner.AiTunerApp.readDebugLog() ?: noLogMsg
                    showDebugLog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Terminal, null, tint = TextMuted)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_debug_log), color = TextSecondary)
            }

            if (showDebugLog) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardDark
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.settings_debug_title), fontWeight = FontWeight.Bold, color = AccentGreen)
                            Row {
                                IconButton(onClick = { copyFn(debugLog) }) {
                                    Icon(Icons.Default.ContentCopy, "复制", tint = TextMuted)
                                }
                                IconButton(onClick = { com.example.aituner.AiTunerApp.clearDebugLog() }) {
                                    Icon(Icons.Default.Delete, "清空", tint = TextMuted)
                                }
                                IconButton(onClick = { showDebugLog = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, stringResource(R.string.chat_close), tint = TextMuted)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(debugLog, fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            if (showCrashLog) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardDark
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.settings_crash_title), fontWeight = FontWeight.Bold, color = AccentRed)
                            Row {
                                IconButton(onClick = { copyFn(crashLog) }) {
                                    Icon(Icons.Default.ContentCopy, "复制", tint = TextMuted)
                                }
                                IconButton(onClick = { showCrashLog = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, stringResource(R.string.chat_close), tint = TextMuted)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            crashLog,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Suppress("ComposableInvokationsInComposition")
@Composable
private fun rememberCopyToClipboard(): (String) -> Unit {
    val context = LocalContext.current
    return { text ->
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("log", text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, context.getString(R.string.settings_copied), android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardDark,
    unfocusedContainerColor = CardDark,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue
)
