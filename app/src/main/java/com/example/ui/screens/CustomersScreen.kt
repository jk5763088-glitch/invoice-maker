package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CustomerEntity
import com.example.data.InvoiceWithItems
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils

@Composable
fun CustomersScreen(
    customers: List<CustomerEntity>,
    invoices: List<InvoiceWithItems>,
    defaultCurrency: String,
    onSaveCustomer: (Long, String, String, String, String) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onCreateInvoiceForCustomer: (CustomerEntity) -> Unit,
    onOpenInvoicePreview: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedCustomerForHistory by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "Customers",
                subtitle = "${customers.size} saved clients",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showEditDialog = true
                },
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Customer")
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
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customers by name, phone, or email...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .testTag("customer_search_input")
                )
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No customers yet",
                        subtitle = "Save your regular clients to fill their details on new invoices with one tap and track their unpaid balances.",
                        buttonText = "Add Customer",
                        buttonTestTag = "empty_add_customer_button",
                        onActionClick = {
                            editingCustomer = null
                            showEditDialog = true
                        },
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
            } else {
                items(
                    items = filteredCustomers,
                    key = { it.id }
                ) { customer ->
                    val custInvoices = invoices.filter {
                        it.invoice.customerId == customer.id ||
                            it.invoice.customerName.equals(customer.name, ignoreCase = true)
                    }
                    val totalBilled = custInvoices.sumOf { it.invoice.grandTotal }
                    val unpaidBalance = custInvoices.sumOf { it.invoice.remainingBalance }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .clickable { selectedCustomerForHistory = customer }
                            .testTag("customer_card_${customer.name}"),
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
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (customer.phone.isNotBlank()) {
                                        Text(
                                            text = "Tel: ${customer.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (customer.email.isNotBlank()) {
                                        Text(
                                            text = customer.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (customer.address.isNotBlank()) {
                                        Text(
                                            text = customer.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingCustomer = customer
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_customer_${customer.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Customer"
                                        )
                                    }
                                    IconButton(
                                        onClick = { customerToDelete = customer },
                                        modifier = Modifier.testTag("delete_customer_${customer.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Customer",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Billed (${custInvoices.size} inv)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatUtils.formatMoney(totalBilled, defaultCurrency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Unpaid Balance",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatUtils.formatMoney(unpaidBalance, defaultCurrency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unpaidBalance > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                TextButton(
                                    onClick = { onCreateInvoiceForCustomer(customer) },
                                    modifier = Modifier.testTag("new_invoice_for_${customer.name}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Invoice")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        CustomerEditDialog(
            initial = editingCustomer,
            onDismiss = { showEditDialog = false },
            onSave = { id, name, phone, email, address ->
                onSaveCustomer(id, name, phone, email, address)
                showEditDialog = false
            }
        )
    }

    customerToDelete?.let { cust ->
        ConfirmDeleteDialog(
            title = "Delete Customer?",
            message = "Are you sure you want to delete '${cust.name}'? Existing invoices for this customer will remain intact.",
            onConfirm = {
                onDeleteCustomer(cust)
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }

    selectedCustomerForHistory?.let { cust ->
        val custInvoices = invoices.filter {
            it.invoice.customerId == cust.id ||
                it.invoice.customerName.equals(cust.name, ignoreCase = true)
        }
        val totalBilled = custInvoices.sumOf { it.invoice.grandTotal }
        val unpaidBalance = custInvoices.sumOf { it.invoice.remainingBalance }

        AlertDialog(
            onDismissRequest = { selectedCustomerForHistory = null },
            title = {
                Column {
                    Text(text = cust.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Total Billed: ${FormatUtils.formatMoney(totalBilled, defaultCurrency)} • Unpaid: ${FormatUtils.formatMoney(unpaidBalance, defaultCurrency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                if (custInvoices.isEmpty()) {
                    Text("No invoices created for ${cust.name} yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        custInvoices.take(8).forEach { invWithItems ->
                            val inv = invWithItems.invoice
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomerForHistory = null
                                        onOpenInvoicePreview(inv.id)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(inv.invoiceNumber, fontWeight = FontWeight.Bold)
                                        Text(inv.invoiceDate, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            FormatUtils.formatMoney(inv.grandTotal, inv.currency),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        PaymentStatusBadge(inv.paymentStatus)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = cust
                        selectedCustomerForHistory = null
                        onCreateInvoiceForCustomer(target)
                    }
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Invoice")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedCustomerForHistory = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CustomerEditDialog(
    initial: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }
    var email by remember(initial) { mutableStateOf(initial?.email ?: "") }
    var address by remember(initial) { mutableStateOf(initial?.address ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Customer" else "Edit Customer",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                    },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_email_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Billing Address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_address_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        localError = "Customer name cannot be empty."
                        return@Button
                    }
                    if (email.isNotBlank() && !FormatUtils.isValidEmail(email)) {
                        localError = "Please enter a valid email address."
                        return@Button
                    }
                    onSave(initial?.id ?: 0L, name, phone, email, address)
                },
                modifier = Modifier.testTag("dialog_save_customer_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
