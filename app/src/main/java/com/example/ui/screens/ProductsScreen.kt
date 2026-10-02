package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProductEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils

@Composable
fun ProductsScreen(
    products: List<ProductEntity>,
    defaultCurrency: String,
    defaultTaxPercent: Double,
    onSaveProduct: (Long, String, String, Double, Double, String) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "Products & Services",
                subtitle = "${products.size} saved items",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showEditDialog = true
                },
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
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
                    placeholder = { Text("Search products or services...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .testTag("product_search_input")
                )
            }

            if (filteredProducts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No products or services yet",
                        subtitle = "Save your frequently billed items, prices, and tax rates for 1-tap selection when creating invoices.",
                        buttonText = "Add Product / Service",
                        buttonTestTag = "empty_add_product_button",
                        onActionClick = {
                            editingProduct = null
                            showEditDialog = true
                        },
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
            } else {
                items(
                    items = filteredProducts,
                    key = { it.id }
                ) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .testTag("product_card_${product.name}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (product.description.isNotBlank()) {
                                    Text(
                                        text = product.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${FormatUtils.formatMoney(product.defaultPrice, defaultCurrency)} / ${product.unit}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (product.taxPercent > 0.0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Tax: ${FormatUtils.formatQuantity(product.taxPercent)}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingProduct = product
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_product_${product.name}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Product")
                                }
                                IconButton(
                                    onClick = { productToDelete = product },
                                    modifier = Modifier.testTag("delete_product_${product.name}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Product",
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

    if (showEditDialog) {
        ProductEditDialog(
            initial = editingProduct,
            defaultTaxPercent = defaultTaxPercent,
            onDismiss = { showEditDialog = false },
            onSave = { id, name, desc, price, tax, unit ->
                onSaveProduct(id, name, desc, price, tax, unit)
                showEditDialog = false
            }
        )
    }

    productToDelete?.let { prod ->
        ConfirmDeleteDialog(
            title = "Delete Product / Service?",
            message = "Are you sure you want to delete '${prod.name}' from your catalog?",
            onConfirm = {
                onDeleteProduct(prod)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@Composable
fun ProductEditDialog(
    initial: ProductEntity?,
    defaultTaxPercent: Double,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Double, Double, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var priceText by remember(initial) {
        mutableStateOf(if (initial != null) initial.defaultPrice.toString() else "")
    }
    var taxText by remember(initial) {
        mutableStateOf(if (initial != null) initial.taxPercent.toString() else defaultTaxPercent.toString())
    }
    var unit by remember(initial) { mutableStateOf(initial?.unit ?: "pcs") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Product / Service" else "Edit Product / Service",
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
                    label = { Text("Product / Service Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_product_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_product_desc_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            localError = null
                        },
                        label = { Text("Unit Price *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_product_price_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs, hr, kg)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_product_unit_input")
                    )
                }
                OutlinedTextField(
                    value = taxText,
                    onValueChange = { taxText = it },
                    label = { Text("Tax Percentage (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_product_tax_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        localError = "Product/service name is required."
                        return@Button
                    }
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (price < 0.0) {
                        localError = "Price cannot be negative."
                        return@Button
                    }
                    val tax = (taxText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    onSave(initial?.id ?: 0L, name, description, price, tax, unit)
                },
                modifier = Modifier.testTag("dialog_save_product_button")
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
