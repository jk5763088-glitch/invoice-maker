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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CustomerEntity
import com.example.data.ProductEntity
import com.example.ui.DraftInvoiceItem
import com.example.ui.InvoiceDraftState
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils
import com.example.util.PaymentStatus
import com.example.util.SupportedCurrency
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateInvoiceScreen(
    draft: InvoiceDraftState,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    defaultTaxPercent: Double,
    onUpdateDraft: ((InvoiceDraftState) -> InvoiceDraftState) -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onSaveOrUpdateItem: (DraftInvoiceItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onPreviewClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<DraftInvoiceItem?>(null) }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = if (draft.editingInvoiceId == 0L) "Create Invoice" else "Edit Invoice",
                subtitle = draft.invoiceNumber,
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
            // Validation Error Banner
            if (!draft.validationError.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .testTag("invoice_validation_error")
                    ) {
                        Text(
                            text = draft.validationError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 1. Invoice Information Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Invoice Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = draft.invoiceNumber,
                                onValueChange = { num -> onUpdateDraft { it.copy(invoiceNumber = num) } },
                                label = { Text("Invoice Number *") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_number_input")
                            )
                            OutlinedTextField(
                                value = draft.currency,
                                onValueChange = { cur ->
                                    val match = SupportedCurrency.fromCode(cur)
                                    onUpdateDraft { it.copy(currency = match.code) }
                                },
                                label = { Text("Currency") },
                                singleLine = true,
                                readOnly = true,
                                modifier = Modifier.width(110.dp)
                            )
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SupportedCurrency.entries.forEach { cur ->
                                FilterChip(
                                    selected = draft.currency.equals(cur.code, ignoreCase = true),
                                    onClick = { onUpdateDraft { it.copy(currency = cur.code) } },
                                    label = { Text(cur.code) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = draft.invoiceDate,
                                onValueChange = { d -> onUpdateDraft { it.copy(invoiceDate = d) } },
                                label = { Text("Invoice Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_date_input")
                            )
                            OutlinedTextField(
                                value = draft.dueDate,
                                onValueChange = { d -> onUpdateDraft { it.copy(dueDate = d) } },
                                label = { Text("Due Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_due_date_input")
                            )
                        }

                        Text(
                            text = "Payment Status",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = draft.paymentStatus.equals(st.key, ignoreCase = true),
                                    onClick = { onUpdateDraft { it.copy(paymentStatus = st.key) } },
                                    label = { Text(st.label) },
                                    modifier = Modifier.testTag("status_chip_${st.key}")
                                )
                            }
                        }

                        if (draft.paymentStatus.equals("PARTIAL", ignoreCase = true)) {
                            OutlinedTextField(
                                value = draft.partialPaidInput,
                                onValueChange = { v -> onUpdateDraft { it.copy(partialPaidInput = v) } },
                                label = { Text("Amount Paid (${draft.currency})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("partial_paid_input")
                            )
                        }
                    }
                }
            }

            // 2. Customer Information Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (customers.isNotEmpty()) {
                                TextButton(
                                    onClick = { showCustomerPicker = true },
                                    modifier = Modifier.testTag("select_saved_customer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select Customer")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = draft.customerName,
                            onValueChange = { v -> onUpdateDraft { it.copy(customerName = v, customerId = null) } },
                            label = { Text("Customer Name *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_customer_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = draft.customerPhone,
                                onValueChange = { v -> onUpdateDraft { it.copy(customerPhone = v) } },
                                label = { Text("Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_customer_phone_input")
                            )
                            OutlinedTextField(
                                value = draft.customerEmail,
                                onValueChange = { v -> onUpdateDraft { it.copy(customerEmail = v) } },
                                label = { Text("Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_customer_email_input")
                            )
                        }

                        OutlinedTextField(
                            value = draft.customerAddress,
                            onValueChange = { v -> onUpdateDraft { it.copy(customerAddress = v) } },
                            label = { Text("Billing Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_customer_address_input")
                        )
                    }
                }
            }

            // 3. Items Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Invoice Items (${draft.items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    editingItem = null
                                    showItemDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_invoice_item_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Item")
                            }
                        }

                        if (draft.items.isEmpty()) {
                            Text(
                                text = "No items added yet. Tap 'Add Item' to add products or services.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            draft.items.forEachIndexed { idx, item ->
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${idx + 1}. ${item.name}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (item.description.isNotBlank()) {
                                            Text(
                                                text = item.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val detailLine = buildString {
                                            append("${FormatUtils.formatQuantity(item.quantity)} ${item.unit} × ${FormatUtils.formatMoney(item.unitPrice, draft.currency)}")
                                            if (item.discountAmount > 0) append(" • Disc: -${FormatUtils.formatMoney(item.discountAmount, draft.currency)}")
                                            if (item.taxPercent > 0) append(" • Tax: +${FormatUtils.formatQuantity(item.taxPercent)}%")
                                        }
                                        Text(
                                            text = detailLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = FormatUtils.formatMoney(item.itemTotal, draft.currency),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = {
                                                editingItem = item
                                                showItemDialog = true
                                            },
                                            modifier = Modifier.testTag("edit_item_${idx}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                                        }
                                        IconButton(
                                            onClick = { onRemoveItem(item.localId) },
                                            modifier = Modifier.testTag("delete_item_${idx}")
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Item",
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

            // 4. Totals Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryLine("Subtotal", FormatUtils.formatMoney(draft.subtotal, draft.currency))
                        SummaryLine("Discount", "- ${FormatUtils.formatMoney(draft.discountTotal, draft.currency)}")
                        SummaryLine("Tax", "+ ${FormatUtils.formatMoney(draft.taxTotal, draft.currency)}")
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Grand Total",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = FormatUtils.formatMoney(draft.grandTotal, draft.currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("draft_grand_total_text")
                            )
                        }
                        SummaryLine("Amount Paid", FormatUtils.formatMoney(draft.amountPaid, draft.currency))
                        SummaryLine(
                            label = "Remaining Balance",
                            value = FormatUtils.formatMoney(draft.remainingBalance, draft.currency),
                            bold = true
                        )
                    }
                }
            }

            // 5. Notes & Terms Card
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
                        OutlinedTextField(
                            value = draft.notes,
                            onValueChange = { v -> onUpdateDraft { it.copy(notes = v) } },
                            label = { Text("Invoice Notes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_notes_input")
                        )
                        OutlinedTextField(
                            value = draft.termsAndConditions,
                            onValueChange = { v -> onUpdateDraft { it.copy(termsAndConditions = v) } },
                            label = { Text("Terms & Conditions") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_terms_input")
                        )
                    }
                }
            }

            // 6. Action Buttons (Preview & Save)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPreviewClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("preview_invoice_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSaveClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_invoice_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Invoice", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Saved Customer", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers, key = { it.id }) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCustomer(cust)
                                    showCustomerPicker = false
                                }
                                .testTag("pick_customer_${cust.name}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                if (cust.phone.isNotBlank()) {
                                    Text(cust.phone, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showItemDialog) {
        InvoiceItemDialog(
            initialItem = editingItem,
            savedProducts = products,
            currency = draft.currency,
            defaultTaxPercent = defaultTaxPercent,
            onDismiss = { showItemDialog = false },
            onSaveItem = { item ->
                onSaveOrUpdateItem(item)
                showItemDialog = false
            }
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun InvoiceItemDialog(
    initialItem: DraftInvoiceItem?,
    savedProducts: List<ProductEntity>,
    currency: String,
    defaultTaxPercent: Double,
    onDismiss: () -> Unit,
    onSaveItem: (DraftInvoiceItem) -> Unit
) {
    var name by remember(initialItem) { mutableStateOf(initialItem?.name ?: "") }
    var description by remember(initialItem) { mutableStateOf(initialItem?.description ?: "") }
    var unit by remember(initialItem) { mutableStateOf(initialItem?.unit ?: "pcs") }
    var qtyText by remember(initialItem) {
        mutableStateOf(if (initialItem != null) FormatUtils.formatQuantity(initialItem.quantity) else "1")
    }
    var priceText by remember(initialItem) {
        mutableStateOf(if (initialItem != null) initialItem.unitPrice.toString() else "")
    }
    var discountText by remember(initialItem) {
        mutableStateOf(if (initialItem != null && initialItem.discountAmount > 0) initialItem.discountAmount.toString() else "0")
    }
    var taxText by remember(initialItem) {
        mutableStateOf(if (initialItem != null) initialItem.taxPercent.toString() else defaultTaxPercent.toString())
    }
    var productId by remember(initialItem) { mutableStateOf(initialItem?.productId) }
    var showProductPicker by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val previewItem = DraftInvoiceItem(
        localId = initialItem?.localId ?: UUID.randomUUID().toString(),
        productId = productId,
        name = name,
        description = description,
        unit = unit,
        quantity = qtyText.toDoubleOrNull() ?: 0.0,
        unitPrice = priceText.toDoubleOrNull() ?: 0.0,
        discountAmount = discountText.toDoubleOrNull() ?: 0.0,
        taxPercent = taxText.toDoubleOrNull() ?: 0.0
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem == null) "Add Item" else "Edit Item",
                    fontWeight = FontWeight.Bold
                )
                if (savedProducts.isNotEmpty()) {
                    TextButton(
                        onClick = { showProductPicker = !showProductPicker },
                        modifier = Modifier.testTag("select_saved_product_button")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Saved")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (showProductPicker && savedProducts.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Tap a saved product to auto-fill:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            savedProducts.take(6).forEach { prod ->
                                TextButton(
                                    onClick = {
                                        productId = prod.id
                                        name = prod.name
                                        description = prod.description
                                        unit = prod.unit
                                        priceText = prod.defaultPrice.toString()
                                        taxText = prod.taxPercent.toString()
                                        showProductPicker = false
                                        localError = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pick_product_${prod.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(prod.name, fontWeight = FontWeight.SemiBold)
                                        Text(FormatUtils.formatMoney(prod.defaultPrice, currency))
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text("Product / Service Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_description_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = {
                            qtyText = it
                            localError = null
                        },
                        label = { Text("Quantity *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_quantity_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        localError = null
                    },
                    label = { Text("Unit Price ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_price_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_discount_input")
                    )
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it },
                        label = { Text("Tax (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_tax_input")
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item Total:", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = FormatUtils.formatMoney(previewItem.itemTotal, currency),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        localError = "Product/service name is required."
                        return@Button
                    }
                    val qty = qtyText.toDoubleOrNull() ?: 0.0
                    if (qty <= 0.0) {
                        localError = "Quantity must be greater than 0."
                        return@Button
                    }
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (price < 0.0) {
                        localError = "Unit price cannot be negative."
                        return@Button
                    }
                    val disc = (discountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val tax = (taxText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    onSaveItem(
                        DraftInvoiceItem(
                            localId = initialItem?.localId ?: UUID.randomUUID().toString(),
                            productId = productId,
                            name = name.trim(),
                            description = description.trim(),
                            unit = unit.trim().ifBlank { "pcs" },
                            quantity = qty,
                            unitPrice = price,
                            discountAmount = disc,
                            taxPercent = tax
                        )
                    )
                },
                modifier = Modifier.testTag("confirm_save_item_button")
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
