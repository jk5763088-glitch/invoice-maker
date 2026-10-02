package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.InvoiceEntity
import com.example.data.InvoiceWithItems
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyInvoicesScreen(
    invoices: List<InvoiceWithItems>,
    searchQuery: String,
    statusFilter: String,
    sortDescending: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onToggleSortOrder: () -> Unit,
    onCreateNewInvoice: () -> Unit,
    onOpenInvoice: (InvoiceWithItems) -> Unit,
    onEditInvoice: (InvoiceWithItems) -> Unit,
    onDuplicateInvoice: (InvoiceWithItems) -> Unit,
    onTogglePaidStatus: (InvoiceEntity) -> Unit,
    onGenerateAndSharePdf: (InvoiceWithItems, Boolean) -> Unit,
    onDeleteInvoice: (InvoiceEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }

    val filteredInvoices = remember(invoices, searchQuery, statusFilter, sortDescending) {
        val list = invoices.filter { item ->
            val inv = item.invoice
            val matchesQuery = searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.invoiceDate.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "ALL" ||
                inv.paymentStatus.equals(statusFilter, ignoreCase = true)
            matchesQuery && matchesStatus
        }
        if (sortDescending) {
            list.sortedWith(compareByDescending<InvoiceWithItems> { it.invoice.invoiceDateMillis }.thenByDescending { it.invoice.id })
        } else {
            list.sortedWith(compareBy<InvoiceWithItems> { it.invoice.invoiceDateMillis }.thenBy { it.invoice.id })
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "My Invoices",
                subtitle = "${filteredInvoices.size} of ${invoices.size} invoices",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onToggleSortOrder,
                        modifier = Modifier.testTag("sort_invoices_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort by date"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewInvoice,
                modifier = Modifier.testTag("fab_create_invoice")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Invoice")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search & Filter Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by invoice #, customer name, or date...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("my_invoices_search_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "PAID" to "Paid",
                            "UNPAID" to "Unpaid",
                            "PARTIAL" to "Partial"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { onStatusFilterChange(key) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }
            }

            if (filteredInvoices.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (invoices.isEmpty()) "No invoices yet" else "No matching invoices",
                        subtitle = if (invoices.isEmpty()) {
                            "Create your first invoice in seconds and export it as a print-ready A4 PDF."
                        } else {
                            "Try adjusting your search query or payment status filter."
                        },
                        buttonText = "Create Your First Invoice",
                        buttonTestTag = "my_invoices_empty_create_button",
                        onActionClick = onCreateNewInvoice,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
            } else {
                items(
                    items = filteredInvoices,
                    key = { it.invoice.id }
                ) { item ->
                    val inv = item.invoice
                    val isPaid = inv.paymentStatus.equals("PAID", ignoreCase = true)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .clickable { onOpenInvoice(item) }
                            .testTag("invoice_list_card_${inv.invoiceNumber}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inv.invoiceNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = inv.customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Date: ${inv.invoiceDate} • Due: ${inv.dueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FormatUtils.formatMoney(inv.grandTotal, inv.currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PaymentStatusBadge(status = inv.paymentStatus)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))

                            // Action Row: Mark Paid/Unpaid, Edit, Duplicate, PDF, Share, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onTogglePaidStatus(inv) },
                                    modifier = Modifier.testTag("toggle_paid_${inv.invoiceNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPaid) "Mark Unpaid" else "Mark Paid")
                                }

                                Row {
                                    IconButton(
                                        onClick = { onEditInvoice(item) },
                                        modifier = Modifier.testTag("edit_invoice_${inv.invoiceNumber}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Invoice")
                                    }
                                    IconButton(
                                        onClick = { onDuplicateInvoice(item) },
                                        modifier = Modifier.testTag("duplicate_invoice_${inv.invoiceNumber}")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Invoice")
                                    }
                                    IconButton(
                                        onClick = { onGenerateAndSharePdf(item, false) },
                                        modifier = Modifier.testTag("pdf_invoice_${inv.invoiceNumber}")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Generate PDF")
                                    }
                                    IconButton(
                                        onClick = { onGenerateAndSharePdf(item, true) },
                                        modifier = Modifier.testTag("share_invoice_${inv.invoiceNumber}")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Invoice")
                                    }
                                    IconButton(
                                        onClick = { invoiceToDelete = inv },
                                        modifier = Modifier.testTag("delete_invoice_${inv.invoiceNumber}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Invoice",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    invoiceToDelete?.let { inv ->
        ConfirmDeleteDialog(
            title = "Delete Invoice ${inv.invoiceNumber}?",
            message = "Are you sure you want to permanently delete invoice ${inv.invoiceNumber} for ${inv.customerName}?",
            onConfirm = {
                onDeleteInvoice(inv)
                invoiceToDelete = null
            },
            onDismiss = { invoiceToDelete = null }
        )
    }
}
