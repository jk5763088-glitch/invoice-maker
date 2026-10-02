package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils
import com.example.util.InvoiceTemplate
import com.example.util.SupportedCurrency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    onOpenBusinessProfile: () -> Unit,
    onSaveSettings: (AppSettingsEntity) -> Unit,
    onExportBackup: (Context, Uri) -> Unit,
    onImportBackup: (Context, Uri) -> Unit,
    onDeleteAllData: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var defaultCurrency by remember(settings) { mutableStateOf(settings.defaultCurrency) }
    var selectedTemplate by remember(settings) { mutableStateOf(settings.selectedTemplate) }
    var invoicePrefix by remember(settings) { mutableStateOf(settings.invoicePrefix) }
    var nextNumberText by remember(settings) { mutableStateOf(settings.nextInvoiceNumber.toString()) }
    var defaultTaxText by remember(settings) { mutableStateOf(settings.defaultTaxPercent.toString()) }
    var defaultTerms by remember(settings) { mutableStateOf(settings.defaultPaymentTerms) }
    var defaultNotes by remember(settings) { mutableStateOf(settings.defaultInvoiceNotes) }
    var themeMode by remember(settings) { mutableStateOf(settings.themeMode) }
    var isPremium by remember(settings) { mutableStateOf(settings.isPremium) }

    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackup(context, uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackup(context, uri)
        }
    }

    fun buildUpdatedSettings(): AppSettingsEntity {
        return AppSettingsEntity(
            id = 1,
            defaultCurrency = defaultCurrency,
            selectedTemplate = selectedTemplate,
            invoicePrefix = invoicePrefix.trim().ifBlank { "INV-" },
            nextInvoiceNumber = (nextNumberText.toIntOrNull() ?: 1).coerceAtLeast(1),
            defaultTaxPercent = (defaultTaxText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
            defaultPaymentTerms = defaultTerms.trim(),
            defaultInvoiceNotes = defaultNotes.trim(),
            themeMode = themeMode,
            isPremium = isPremium
        )
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "Settings",
                subtitle = "Templates, numbering, currency & backup",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Business Profile Shortcut
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .clickable { onOpenBusinessProfile() }
                        .testTag("settings_business_profile_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Business Profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Edit shop name, logo, phone, WhatsApp, and NTN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Invoice Template Selection (Simple, Modern, Business, Executive Pro)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Invoice PDF Template",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        InvoiceTemplate.entries.forEach { tpl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTemplate = tpl.id }
                                    .padding(vertical = 4.dp)
                                    .testTag("settings_template_${tpl.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTemplate.equals(tpl.id, ignoreCase = true),
                                    onClick = { selectedTemplate = tpl.id }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tpl.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = tpl.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Currency & Invoice Numbering & Defaults
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Currency & Invoice Defaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SupportedCurrency.entries.forEach { cur ->
                                FilterChip(
                                    selected = defaultCurrency.equals(cur.code, ignoreCase = true),
                                    onClick = { defaultCurrency = cur.code },
                                    label = { Text("${cur.code} (${cur.symbol})") },
                                    modifier = Modifier.testTag("settings_currency_${cur.code}")
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = invoicePrefix,
                                onValueChange = { invoicePrefix = it },
                                label = { Text("Invoice Prefix") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_prefix_input")
                            )
                            OutlinedTextField(
                                value = nextNumberText,
                                onValueChange = { nextNumberText = it },
                                label = { Text("Starting Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_starting_number_input")
                            )
                        }

                        OutlinedTextField(
                            value = defaultTaxText,
                            onValueChange = { defaultTaxText = it },
                            label = { Text("Default Tax Percentage (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_default_tax_input")
                        )

                        OutlinedTextField(
                            value = defaultNotes,
                            onValueChange = { defaultNotes = it },
                            label = { Text("Default Invoice Notes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_default_notes_input")
                        )

                        OutlinedTextField(
                            value = defaultTerms,
                            onValueChange = { defaultTerms = it },
                            label = { Text("Default Payment Terms") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_default_terms_input")
                        )
                    }
                }
            }

            // 4. Theme & Pro / Remove Ads
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Appearance & Pro Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                                FilterChip(
                                    selected = themeMode.equals(key, ignoreCase = true),
                                    onClick = {
                                        themeMode = key
                                        onSaveSettings(buildUpdatedSettings().copy(themeMode = key))
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_chip_$key")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Pro Mode (Remove Ads & Pro Templates)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Disable AdMob banners & unlock Executive Pro template",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isPremium,
                                onCheckedChange = { checked ->
                                    isPremium = checked
                                    onSaveSettings(buildUpdatedSettings().copy(isPremium = checked))
                                },
                                modifier = Modifier.testTag("pro_mode_switch")
                            )
                        }
                    }
                }
            }

            // 5. Save Settings Button
            item {
                Button(
                    onClick = { onSaveSettings(buildUpdatedSettings()) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .height(52.dp)
                        .testTag("save_settings_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            }

            // 6. Offline Backup & Restore
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Offline Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export all invoices, customers, products, and business profile to a local JSON file or restore from an existing backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportBackupLauncher.launch("invoice_maker_backup_${FormatUtils.todayDateString()}.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Backup")
                            }
                            OutlinedButton(
                                onClick = {
                                    importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_button")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Backup")
                            }
                        }
                    }
                }
            }

            // 7. About, Privacy Policy & Delete All Data
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAboutDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("about_app_button")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("About")
                            }
                            OutlinedButton(
                                onClick = { showPrivacyDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("privacy_policy_button")
                            ) {
                                Icon(Icons.Default.PrivacyTip, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Privacy Policy")
                            }
                        }

                        Button(
                            onClick = { showDeleteAllConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteAllConfirm) {
        ConfirmDeleteDialog(
            title = "Delete All App Data?",
            message = "This will permanently erase all saved invoices, customers, products, and business profile from this device. This action cannot be undone.",
            confirmButtonText = "Delete Everything",
            onConfirm = {
                onDeleteAllData()
                showDeleteAllConfirm = false
            },
            onDismiss = { showDeleteAllConfirm = false }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Invoice Maker – Easy Invoice & PDF", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Version 1.0\n\nBuilt for small shops, freelancers, home businesses, and service providers. Create professional A4 PDF invoices offline, track customer balances, and share bills instantly via WhatsApp or Email."
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Offline Data Policy", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "1. Offline-First Storage: All business profile details, customer records, product catalogs, and invoices are stored locally on your device using an encrypted-ready SQLite/Room database.\n\n2. Zero Cloud Uploads: We never upload your customer lists or financial records to external servers.\n\n3. User-Controlled Backups: Data only leaves your device when you explicitly share a PDF invoice or export a backup file."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }
}
