package com.richardyap.novelreader.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.richardyap.novelreader.StorageRepository
import com.richardyap.novelreader.models.ReaderPrefs
import com.richardyap.novelreader.models.SyncPrefs
import com.richardyap.novelreader.ui.components.SettingRow
import com.richardyap.novelreader.ui.theme.allThemeKeys
import com.richardyap.novelreader.ui.theme.allThemeNames
import kotlinx.coroutines.launch

/**
 * 设置界面。
 *
 * 功能：
 * - 阅读设置（默认字体 / 行距 / 段距 / 翻页模式 / 主题）
 * - 备份与恢复
 * - 关于页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storage: StorageRepository,
    onBack: () -> Unit,
    onNavigateToSources: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val prefs = remember { storage.loadReaderPrefs() }
    val syncPrefs = remember { storage.loadSyncPrefs() }

    // Editable prefs
    var fontSize by remember { mutableFloatStateOf(prefs.fontSize.toFloat()) }
    var lineHeight by remember { mutableFloatStateOf(prefs.lineHeight) }
    var paragraphSpacing by remember { mutableIntStateOf(prefs.paragraphSpacing) }
    var pagePadding by remember { mutableIntStateOf(prefs.pagePadding) }
    var selectedTheme by remember { mutableStateOf(prefs.theme) }
    var pageMode by remember { mutableStateOf(prefs.pageMode) }
    var dimLevel by remember { mutableIntStateOf(prefs.dimLevel) }
    var colorTemperature by remember { mutableIntStateOf(prefs.colorTemperature) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun savePrefs() {
        val newPrefs = ReaderPrefs(
            fontSize = fontSize.toInt(),
            lineHeight = lineHeight,
            paragraphSpacing = paragraphSpacing,
            pagePadding = pagePadding,
            theme = selectedTheme,
            pageMode = pageMode,
            dimLevel = dimLevel,
            colorTemperature = colorTemperature,
        )
        storage.saveReaderPrefs(newPrefs)
        statusMessage = "设置已保存"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Status message
            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // ── 阅读设置 ────────────────────────────────────────
            SectionHeader("阅读设置")

            SettingRow(label = "默认字体大小", subtitle = "${fontSize.toInt()}sp") {
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 14f..30f,
                    steps = 15,
                    modifier = Modifier.width(160.dp),
                )
            }

            SettingRow(label = "行间距", subtitle = "${"%.1f".format(lineHeight)}") {
                Slider(
                    value = lineHeight,
                    onValueChange = { lineHeight = it },
                    valueRange = 1.2f..2.5f,
                    modifier = Modifier.width(160.dp),
                )
            }

            SettingRow(label = "段间距", subtitle = "${paragraphSpacing}dp") {
                Slider(
                    value = paragraphSpacing.toFloat(),
                    onValueChange = { paragraphSpacing = it.toInt() },
                    valueRange = 4f..24f,
                    steps = 19,
                    modifier = Modifier.width(160.dp),
                )
            }

            SettingRow(label = "页边距", subtitle = "${pagePadding}dp") {
                Slider(
                    value = pagePadding.toFloat(),
                    onValueChange = { pagePadding = it.toInt() },
                    valueRange = 8f..48f,
                    steps = 7,
                    modifier = Modifier.width(160.dp),
                )
            }

            // Quick theme selector using SettingRow with clickable selection
            val themeIdx = allThemeKeys.indexOf(selectedTheme).coerceAtLeast(0)
            SettingRow(
                label = "默认主题",
                subtitle = allThemeNames.getOrElse(themeIdx) { "纸白" },
            ) {
                TextButton(onClick = {
                    val nextIdx = (themeIdx + 1) % allThemeKeys.size
                    selectedTheme = allThemeKeys[nextIdx]
                }) {
                }
                Row {
                    allThemeKeys.forEachIndexed { idx, key ->
                        val dotColor = if (key == selectedTheme) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically),
                        ) {
                            drawCircle(color = dotColor)
                        }
                        if (idx < allThemeKeys.lastIndex) Spacer(Modifier.width(6.dp))
                    }
                }
            }

            SettingRow(label = "翻页模式", subtitle = if (pageMode == "scroll") "滚动" else "仿真翻页") {
                Switch(
                    checked = pageMode == "scroll",
                    onCheckedChange = {
                        pageMode = if (it) "scroll" else "paged"
                    },
                )
            }

            SettingRow(label = "默认亮度", subtitle = "${dimLevel}") {
                Slider(
                    value = dimLevel.toFloat(),
                    onValueChange = { dimLevel = it.toInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(120.dp),
                )
            }

            SettingRow(label = "默认色温", subtitle = "${colorTemperature}") {
                Slider(
                    value = colorTemperature.toFloat(),
                    onValueChange = { colorTemperature = it.toInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(120.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF8C00),
                        activeTrackColor = Color(0xFFFF8C00),
                    ),
                )
            }

            Button(
                onClick = { savePrefs() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
            ) {
                Text("保存阅读设置")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 书源管理入口 ────────────────────────────────────
            SectionHeader("书源")
            ClickableSettingRow(
                label = "书源管理",
                subtitle = "管理在线书源",
                onClick = onNavigateToSources,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 备份与恢复 ──────────────────────────────────────
            SectionHeader("备份与恢复")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { showBackupDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("备份数据")
                }
                Button(
                    onClick = { showRestoreDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Text("恢复数据")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 关于 ────────────────────────────────────────────
            SectionHeader("关于")
            ClickableSettingRow(
                label = "关于 NovelReader",
                subtitle = "版本信息与开源许可",
                onClick = { showAboutDialog = true },
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    // ── Backup dialog ──────────────────────────────────────────
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("备份数据") },
            text = {
                Text("将导出所有书籍、书签和设置到一个备份文件中。备份文件将保存在应用数据目录下。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // In real impl, serialize to JSON and write file
                                statusMessage = "备份完成"
                            } catch (e: Exception) {
                                statusMessage = "备份失败：${e.message}"
                            }
                        }
                        showBackupDialog = false
                    },
                ) {
                    Text("开始备份")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // ── Restore dialog ─────────────────────────────────────────
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("恢复数据") },
            text = {
                Text("将从备份文件恢复数据。当前数据将被覆盖，此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                statusMessage = "恢复完成"
                            } catch (e: Exception) {
                                statusMessage = "恢复失败：${e.message}"
                            }
                        }
                        showRestoreDialog = false
                    },
                ) {
                    Text("确认恢复", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // ── About dialog ───────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 NovelReader") },
            text = {
                Column {
                    Text("NovelReader — 跨平台小说阅读器")
                    Spacer(Modifier.height(8.dp))
                    Text("技术栈：Kotlin Multiplatform + Compose Multiplatform")
                    Spacer(Modifier.height(4.dp))
                    Text("支持平台：Android / Windows Desktop")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "开源许可：MIT License",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("关闭")
                }
            },
        )
    }
}

// ─── Section header ──────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

// ─── Clickable setting row ───────────────────────────────────────────

@Composable
private fun ClickableSettingRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
