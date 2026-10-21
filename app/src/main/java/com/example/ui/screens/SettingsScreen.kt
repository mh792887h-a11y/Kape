package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AiProvider
import com.example.domain.model.ThemeMode
import com.example.util.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentProvider by viewModel.selectedProvider.collectAsState()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsState()
    val selectedGrokModel by viewModel.selectedGrokModel.collectAsState()
    val savedGeminiKey by viewModel.geminiApiKey.collectAsState()
    val savedGrokKey by viewModel.grokApiKey.collectAsState()
    val hasBuildConfigGeminiKey = viewModel.hasBuildConfigGeminiKey

    var inputGeminiKey by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }

    var inputGrokKey by remember(savedGrokKey) { mutableStateOf(savedGrokKey) }
    var isGrokKeyVisible by remember { mutableStateOf(false) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: AI Provider Selection (Gemini vs Grok)
                SettingsCard(
                    title = "مزود الذكاء الاصطناعي النشط",
                    icon = Icons.Default.SmartToy
                ) {
                    Text(
                        text = "اختر المحرك الذي يغذي شخصية الروبوت العبقرية الساخرة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val providers = listOf(AiProvider.GEMINI, AiProvider.GROK)
                    val selectedTabIndex = providers.indexOf(currentProvider)

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        providers.forEachIndexed { index, provider ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { viewModel.setSelectedProvider(provider) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (provider == AiProvider.GEMINI) "⚡ Gemini (Google)" else "🚀 Grok (xAI)",
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("provider_tab_${provider.name}")
                            )
                        }
                    }
                }

                // Section 2: Model Selection for Active Provider
                SettingsCard(
                    title = "نماذج ${currentProvider.displayName}",
                    icon = if (currentProvider == AiProvider.GROK) Icons.Default.RocketLaunch else Icons.Default.SmartToy
                ) {
                    val availableModels = AppConstants.getModelsForProvider(currentProvider)
                    val currentModel = if (currentProvider == AiProvider.GROK) selectedGrokModel else selectedGeminiModel

                    Text(
                        text = if (currentProvider == AiProvider.GROK) {
                            "جميع نماذج Grok من xAI مدعومة بروح فكاهية عالية وسرعة استجابة فائقة."
                        } else {
                            "جميع نماذج Gemini من Google مدعومة مع شخصية الروبوت العبثية والواثقة."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = isModelDropdownExpanded,
                        onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("model_selector_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false }
                        ) {
                            availableModels.forEach { modelName ->
                                val defaultModel = AppConstants.getDefaultModelForProvider(currentProvider)
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = modelName,
                                                fontWeight = if (modelName == currentModel) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (modelName == defaultModel) {
                                                Text(
                                                    text = "(النموذج الافتراضي الموصى به)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (currentProvider == AiProvider.GROK) {
                                            viewModel.setSelectedGrokModel(modelName)
                                        } else {
                                            viewModel.setSelectedGeminiModel(modelName)
                                        }
                                        isModelDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("dropdown_item_$modelName")
                                )
                            }
                        }
                    }
                }

                // Section 3: Grok (xAI) API Key Card
                SettingsCard(
                    title = "مفتاح Grok API (xAI)",
                    icon = Icons.Default.Key
                ) {
                    Text(
                        text = "احصل على مفتاح Grok من console.x.ai، ثم الصقه هنا لتفعيل نماذج Grok مع الشخصية الكوميدية الساخرة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = inputGrokKey,
                        onValueChange = { inputGrokKey = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grok_api_key_input_field"),
                        label = { Text("أدخل مفتاح xai-...") },
                        visualTransformation = if (isGrokKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isGrokKeyVisible = !isGrokKeyVisible }) {
                                Icon(
                                    imageVector = if (isGrokKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "إظهار/إخفاء المفتاح"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (savedGrokKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.clearGrokApiKey()
                                    inputGrokKey = ""
                                    Toast.makeText(context, "تم مسح مفتاح Grok", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("clear_grok_key_button")
                            ) {
                                Text(
                                    text = "إزالة المفتاح",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = {
                                viewModel.saveGrokApiKey(inputGrokKey)
                                Toast.makeText(context, "تم حفظ مفتاح Grok بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_grok_key_button")
                        ) {
                            Text(text = "حفظ مفتاح Grok")
                        }
                    }
                }

                // Section 4: Gemini API Key Card
                SettingsCard(
                    title = "مفتاح Gemini API (Google)",
                    icon = Icons.Default.Key
                ) {
                    Text(
                        text = stringResource(R.string.api_key_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (hasBuildConfigGeminiKey) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "✓ تم اكتشاف مفتاح Gemini مدمج في إعدادات المشروع (BuildConfig). يمكنك استخدام المفتاح المدمج مباشرة أو تخصيص مفتاح هنا.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputGeminiKey,
                        onValueChange = { inputGeminiKey = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        label = { Text("أدخل مفتاح AIzaSy...") },
                        visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                                Icon(
                                    imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "إظهار/إخفاء المفتاح"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (savedGeminiKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.clearGeminiApiKey()
                                    inputGeminiKey = ""
                                    Toast.makeText(context, "تمت إزالة مفتاح Gemini", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("clear_api_key_button")
                            ) {
                                Text(
                                    text = "إزالة المفتاح",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = {
                                viewModel.saveGeminiApiKey(inputGeminiKey)
                                Toast.makeText(context, context.getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Text(text = "حفظ مفتاح Gemini")
                        }
                    }
                }

                // Section 5: Appearance / Theme
                SettingsCard(
                    title = stringResource(R.string.theme_setting),
                    icon = Icons.Default.Brightness4
                ) {
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_dark),
                        selected = currentTheme == ThemeMode.DARK,
                        onSelect = { viewModel.setThemeMode(ThemeMode.DARK) },
                        testTag = "theme_option_dark"
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_light),
                        selected = currentTheme == ThemeMode.LIGHT,
                        onSelect = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        testTag = "theme_option_light"
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_system),
                        selected = currentTheme == ThemeMode.SYSTEM,
                        onSelect = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        testTag = "theme_option_system"
                    )
                }

                // Section 6: Clear Conversations Data
                SettingsCard(
                    title = stringResource(R.string.clear_chats),
                    icon = Icons.Default.DeleteSweep
                ) {
                    Text(
                        text = "حذف جميع المحادثات والرسائل المخزنة محلياً في قاعدة بيانات الهاتف.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_chats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.clear_chats))
                    }
                }

                // Section 7: About and Security Notice
                SettingsCard(
                    title = stringResource(R.string.about_app),
                    icon = Icons.Default.Info
                ) {
                    Text(
                        text = "يدعم التطبيق الآن محركات Google Gemini و xAI Grok مع كافة النماذج. تم ضبط الشخصية الفكاهية العبقرية لتكون غير مملة أبداً ومليئة بالطاقة والابتكار الساخر مع احترام معايير السلامة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "الإصدار: 2.0.0 (Gemini + Grok Ultra-Funny Edition)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Confirmation Dialog for Clearing All Chats
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text(text = stringResource(R.string.clear_chats)) },
                text = { Text(text = stringResource(R.string.confirm_clear_all)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllConversations {
                                showClearConfirmDialog = false
                                Toast.makeText(context, "تم مسح جميع المحادثات", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_clear_all_btn")
                    ) {
                        Text(text = stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
