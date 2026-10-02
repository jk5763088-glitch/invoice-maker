package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BusinessProfileEntity
import com.example.data.InvoiceEntity
import com.example.data.InvoiceItemEntity
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils
import com.example.util.InvoiceTemplate
import com.example.util.PdfInvoiceGenerator
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvoicePreviewScreen(
    profile: BusinessProfileEntity,
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>,
    isDraftMode: Boolean,
    generatedPdfFile: File?,
    onSaveDraftInvoice: () -> Unit,
    onEditInvoice: () -> Unit,
    onGeneratePdf: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var selectedTemplate by remember(invoice.templateId) {
        mutableStateOf(InvoiceTemplate.fromId(invoice.templateId))
    }
    var pdfViewerBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val file = generatedPdfFile
        if (uri != null && file != null) {
            PdfInvoiceGenerator.copyPdfToUri(context, file, uri)
                .onSuccess { onShowMessage("PDF saved to selected location!") }
                .onFailure { err -> onShowMessage("Failed to save PDF: ${err.localizedMessage}") }
        }
    }

    val effectiveInvoice = remember(invoice, selectedTemplate) {
        invoice.copy(templateId = selectedTemplate.id)
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "Invoice Preview",
                subtitle = "${effectiveInvoice.invoiceNumber} • A4 Print Layout",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onEditInvoice,
                        modifier = Modifier.testTag("preview_edit_invoice_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Invoice")
                    }
                }
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
            // Template Selector Strip
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Template Style",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InvoiceTemplate.entries.forEach { tpl ->
                                FilterChip(
                                    selected = selectedTemplate == tpl,
                                    onClick = { selectedTemplate = tpl },
                                    label = { Text(tpl.title) },
                                    modifier = Modifier.testTag("preview_template_${tpl.id}")
                                )
                            }
                        }
                    }
                }
            }

            // A4 Paper Invoice Sheet Card
            item {
                A4InvoicePaperSheet(
                    profile = profile,
                    invoice = effectiveInvoice,
                    items = items,
                    template = selectedTemplate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                )
            }

            // Primary Actions: Save Draft (if unsaved) + Generate PDF
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isDraftMode) {
                        OutlinedButton(
                            onClick = onSaveDraftInvoice,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("preview_save_invoice_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Invoice", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onGeneratePdf(effectiveInvoice, items) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("generate_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Post-PDF Generation Action Panel (Open, Share, Save, Print)
            if (generatedPdfFile != null && generatedPdfFile.exists()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .testTag("pdf_actions_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "A4 PDF Ready: ${generatedPdfFile.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val bmp = PdfInvoiceGenerator.renderPdfFirstPageBitmap(generatedPdfFile)
                                        if (bmp != null) {
                                            pdfViewerBitmap = bmp
                                        } else {
                                            PdfInvoiceGenerator.openPdfInExternalViewer(context, generatedPdfFile)
                                                .onFailure { err -> onShowMessage(err.localizedMessage ?: "Cannot open PDF") }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_pdf_button")
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF")
                                }

                                Button(
                                    onClick = {
                                        PdfInvoiceGenerator.sharePdf(context, generatedPdfFile, effectiveInvoice)
                                            .onFailure { err ->
                                                onShowMessage("Share failed: ${err.localizedMessage}")
                                            }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_pdf_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share PDF")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        savePdfLauncher.launch(generatedPdfFile.name)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_pdf_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save PDF")
                                }

                                OutlinedButton(
                                    onClick = {
                                        PdfInvoiceGenerator.printPdf(
                                            context = context,
                                            file = generatedPdfFile,
                                            jobName = "Invoice_${effectiveInvoice.invoiceNumber}"
                                        ).onFailure { err ->
                                            onShowMessage("Print unavailable: ${err.localizedMessage}")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("print_pdf_button")
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Print PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // In-app High-Res Rendered PDF Viewer Modal
    pdfViewerBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { pdfViewerBitmap = null },
            title = {
                Text(
                    text = "PDF Document (${generatedPdfFile?.name ?: ""})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Rendered A4 PDF Page",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = generatedPdfFile
                        pdfViewerBitmap = null
                        if (file != null) {
                            PdfInvoiceGenerator.openPdfInExternalViewer(context, file)
                                .onFailure { err ->
                                    onShowMessage(err.localizedMessage ?: "Viewed in-app PDF reader")
                                }
                        }
                    }
                ) {
                    Text("External Viewer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pdfViewerBitmap = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun A4InvoicePaperSheet(
    profile: BusinessProfileEntity,
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>,
    template: InvoiceTemplate,
    modifier: Modifier = Modifier
) {
    val headerBgColor = when (template) {
        InvoiceTemplate.SIMPLE -> Color.White
        InvoiceTemplate.MODERN -> Color(0xFF0F4C81)
        InvoiceTemplate.BUSINESS -> Color(0xFF0D9488)
        InvoiceTemplate.EXECUTIVE -> Color(0xFF18181B)
    }
    val headerTextColor = if (template == InvoiceTemplate.SIMPLE) Color(0xFF0F172A) else Color.White

    Card(
        modifier = modifier.testTag("a4_invoice_preview_sheet"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val logoFile = profile.logoPath.takeIf { it.isNotBlank() }?.let { File(it) }
                    if (logoFile != null && logoFile.exists()) {
                        AsyncImage(
                            model = logoFile,
                            contentDescription = "Business Logo",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = profile.businessName.ifBlank { "Your Business Name" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerTextColor
                        )
                        val subContact = listOfNotNull(
                            profile.ownerName.takeIf { it.isNotBlank() },
                            profile.phone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
                            profile.whatsapp.takeIf { it.isNotBlank() }?.let { "WA: $it" }
                        ).joinToString(" • ")
                        if (subContact.isNotBlank()) {
                            Text(
                                text = subContact,
                                style = MaterialTheme.typography.bodySmall,
                                color = headerTextColor.copy(alpha = 0.85f)
                            )
                        }
                        val subAddr = listOfNotNull(
                            profile.address.takeIf { it.isNotBlank() },
                            profile.city.takeIf { it.isNotBlank() },
                            profile.email.takeIf { it.isNotBlank() }
                        ).joinToString(", ")
                        if (subAddr.isNotBlank()) {
                            Text(
                                text = subAddr,
                                style = MaterialTheme.typography.bodySmall,
                                color = headerTextColor.copy(alpha = 0.85f)
                            )
                        }
                        if (profile.taxNtnNumber.isNotBlank()) {
                            Text(
                                text = "Tax/NTN: ${profile.taxNtnNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = headerTextColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "INVOICE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = headerTextColor
                    )
                    Text(
                        text = "#${invoice.invoiceNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PaymentStatusBadge(status = invoice.paymentStatus)
                }
            }

            if (template == InvoiceTemplate.SIMPLE) {
                HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.5.dp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Bill To & Invoice Meta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BILL TO",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = invoice.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (invoice.customerPhone.isNotBlank()) {
                            Text(
                                text = invoice.customerPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                        }
                        if (invoice.customerEmail.isNotBlank()) {
                            Text(
                                text = invoice.customerEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                        }
                        if (invoice.customerAddress.isNotBlank()) {
                            Text(
                                text = invoice.customerAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Date: ${invoice.invoiceDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Due Date: ${invoice.dueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Currency: ${invoice.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Items Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Item",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.weight(2.2f)
                    )
                    Text(
                        text = "Qty",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // Items Rows
                items.forEachIndexed { idx, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(2.2f)) {
                                Text(
                                    text = "${idx + 1}. ${item.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                if (item.description.isNotBlank()) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (item.discountAmount > 0 || item.taxPercent > 0) {
                                    val sub = buildString {
                                        if (item.discountAmount > 0) append("Disc: -${FormatUtils.formatMoney(item.discountAmount, invoice.currency)} ")
                                        if (item.taxPercent > 0) append("Tax: ${FormatUtils.formatQuantity(item.taxPercent)}%")
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Text(
                                text = FormatUtils.formatQuantity(item.quantity),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.8f)
                            )
                            Text(
                                text = FormatUtils.formatMoney(item.unitPrice, invoice.currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = FormatUtils.formatMoney(item.itemTotal, invoice.currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                    }
                }

                // Totals Block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Column(
                        modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PaperTotalRow("Subtotal", FormatUtils.formatMoney(invoice.subtotal, invoice.currency))
                        PaperTotalRow("Discount", "- ${FormatUtils.formatMoney(invoice.discountTotal, invoice.currency)}")
                        PaperTotalRow("Tax", "+ ${FormatUtils.formatMoney(invoice.taxTotal, invoice.currency)}")
                        HorizontalDivider(color = Color(0xFFCBD5E1))
                        PaperTotalRow(
                            label = "Grand Total",
                            value = FormatUtils.formatMoney(invoice.grandTotal, invoice.currency),
                            bold = true
                        )
                        PaperTotalRow("Paid Amount", FormatUtils.formatMoney(invoice.amountPaid, invoice.currency))
                        PaperTotalRow(
                            label = "Remaining Balance",
                            value = FormatUtils.formatMoney(invoice.remainingBalance, invoice.currency),
                            bold = true
                        )
                    }
                }

                // Notes & Terms
                if (invoice.notes.isNotBlank() || invoice.termsAndConditions.isNotBlank()) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    if (invoice.notes.isNotBlank()) {
                        Text(
                            text = "Notes: ${invoice.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                    if (invoice.termsAndConditions.isNotBlank()) {
                        Text(
                            text = "Terms: ${invoice.termsAndConditions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperTotalRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF1E293B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
    }
}
