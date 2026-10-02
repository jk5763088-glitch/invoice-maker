package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettingsEntity
import com.example.data.BusinessProfileEntity
import com.example.data.CustomerEntity
import com.example.data.InvoiceEntity
import com.example.data.InvoiceItemEntity
import com.example.data.InvoiceRepository
import com.example.data.InvoiceWithItems
import com.example.data.ProductEntity
import com.example.util.FormatUtils
import com.example.util.PdfInvoiceGenerator
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AppScreen {
    data object Home : AppScreen()
    data object CreateInvoice : AppScreen()
    data class InvoicePreview(val invoiceId: Long? = null, val useDraft: Boolean = false) : AppScreen()
    data object MyInvoices : AppScreen()
    data object Customers : AppScreen()
    data object Products : AppScreen()
    data object BusinessProfile : AppScreen()
    data object Reports : AppScreen()
    data object Settings : AppScreen()
}

data class DraftInvoiceItem(
    val localId: String = UUID.randomUUID().toString(),
    val productId: Long? = null,
    val name: String = "",
    val description: String = "",
    val unit: String = "pcs",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0
) {
    val grossAmount: Double
        get() = (quantity * unitPrice).coerceAtLeast(0.0)

    val netAfterDiscount: Double
        get() = (grossAmount - discountAmount).coerceAtLeast(0.0)

    val taxAmount: Double
        get() = netAfterDiscount * (taxPercent.coerceAtLeast(0.0) / 100.0)

    val itemTotal: Double
        get() = netAfterDiscount + taxAmount

    fun toEntity(invoiceId: Long = 0L): InvoiceItemEntity {
        return InvoiceItemEntity(
            invoiceId = invoiceId,
            productId = productId,
            name = name.trim(),
            description = description.trim(),
            unit = unit.trim().ifBlank { "pcs" },
            quantity = quantity,
            unitPrice = unitPrice,
            discountAmount = discountAmount,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            itemTotal = itemTotal
        )
    }
}

data class InvoiceDraftState(
    val editingInvoiceId: Long = 0L,
    val invoiceNumber: String = "INV-0001",
    val invoiceDate: String = FormatUtils.todayDateString(),
    val dueDate: String = FormatUtils.dueDateString(15),
    val paymentStatus: String = "UNPAID", // PAID, UNPAID, PARTIAL
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val currency: String = "USD",
    val templateId: String = "MODERN",
    val items: List<DraftInvoiceItem> = emptyList(),
    val partialPaidInput: String = "",
    val notes: String = "",
    val termsAndConditions: String = "",
    val validationError: String? = null
) {
    val subtotal: Double
        get() = items.sumOf { it.grossAmount }

    val discountTotal: Double
        get() = items.sumOf { it.discountAmount }

    val taxTotal: Double
        get() = items.sumOf { it.taxAmount }

    val grandTotal: Double
        get() = (subtotal - discountTotal + taxTotal).coerceAtLeast(0.0)

    val amountPaid: Double
        get() = when (paymentStatus.uppercase()) {
            "PAID" -> grandTotal
            "UNPAID" -> 0.0
            else -> (partialPaidInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, grandTotal)
        }

    val remainingBalance: Double
        get() = (grandTotal - amountPaid).coerceAtLeast(0.0)

    fun toInvoiceEntity(): InvoiceEntity {
        return InvoiceEntity(
            id = editingInvoiceId,
            invoiceNumber = invoiceNumber.trim(),
            invoiceDate = invoiceDate.trim(),
            invoiceDateMillis = FormatUtils.parseDateToMillis(invoiceDate.trim()),
            dueDate = dueDate.trim(),
            paymentStatus = paymentStatus.uppercase(),
            customerId = customerId,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            customerEmail = customerEmail.trim(),
            customerAddress = customerAddress.trim(),
            currency = currency,
            templateId = templateId,
            subtotal = subtotal,
            discountTotal = discountTotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal,
            amountPaid = amountPaid,
            remainingBalance = remainingBalance,
            notes = notes.trim(),
            termsAndConditions = termsAndConditions.trim()
        )
    }
}

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

class InvoiceViewModel(
    private val repository: InvoiceRepository
) : ViewModel() {

    private val _backStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.Home))
    val currentScreen: StateFlow<AppScreen> = _backStack
        .combine(MutableStateFlow(Unit)) { stack, _ -> stack.lastOrNull() ?: AppScreen.Home }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppScreen.Home)

    val businessProfile: StateFlow<BusinessProfileEntity> = repository.businessProfileFlow
        .combine(MutableStateFlow(Unit)) { profile, _ -> profile ?: BusinessProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessProfileEntity())

    val appSettings: StateFlow<AppSettingsEntity> = repository.appSettingsFlow
        .combine(MutableStateFlow(Unit)) { settings, _ -> settings ?: AppSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val customers: StateFlow<List<CustomerEntity>> = repository.customersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.productsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoicesWithItems: StateFlow<List<InvoiceWithItems>> = repository.invoicesWithItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _draftState = MutableStateFlow(InvoiceDraftState())
    val draftState: StateFlow<InvoiceDraftState> = _draftState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    // Filter/search state for My Invoices
    private val _invoiceSearchQuery = MutableStateFlow("")
    val invoiceSearchQuery: StateFlow<String> = _invoiceSearchQuery.asStateFlow()

    private val _invoiceStatusFilter = MutableStateFlow("ALL") // ALL, PAID, UNPAID, PARTIAL
    val invoiceStatusFilter: StateFlow<String> = _invoiceStatusFilter.asStateFlow()

    private val _invoiceSortDescending = MutableStateFlow(true)
    val invoiceSortDescending: StateFlow<Boolean> = _invoiceSortDescending.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun navigateTo(screen: AppScreen) {
        _backStack.update { stack ->
            if (screen == AppScreen.Home) {
                listOf(AppScreen.Home)
            } else if (stack.lastOrNull() == screen) {
                stack
            } else {
                stack + screen
            }
        }
    }

    fun navigateBack(): Boolean {
        val currentStack = _backStack.value
        return if (currentStack.size > 1) {
            _backStack.value = currentStack.dropLast(1)
            true
        } else {
            false
        }
    }

    // --- Invoice Creation / Editing ---
    fun startNewInvoice(preselectedCustomer: CustomerEntity? = null) {
        viewModelScope.launch {
            val settings = repository.getAppSettingsOnce()
            val profile = repository.getBusinessProfileOnce()
            val nextNumber = repository.generateNextUniqueInvoiceNumber()
            val currency = profile.defaultCurrency.ifBlank { settings.defaultCurrency }
            _generatedPdfFile.value = null
            _draftState.value = InvoiceDraftState(
                editingInvoiceId = 0L,
                invoiceNumber = nextNumber,
                invoiceDate = FormatUtils.todayDateString(),
                dueDate = FormatUtils.dueDateString(15),
                paymentStatus = "UNPAID",
                customerId = preselectedCustomer?.id,
                customerName = preselectedCustomer?.name ?: "",
                customerPhone = preselectedCustomer?.phone ?: "",
                customerEmail = preselectedCustomer?.email ?: "",
                customerAddress = preselectedCustomer?.address ?: "",
                currency = currency,
                templateId = settings.selectedTemplate,
                items = emptyList(),
                partialPaidInput = "",
                notes = settings.defaultInvoiceNotes,
                termsAndConditions = settings.defaultPaymentTerms,
                validationError = null
            )
            navigateTo(AppScreen.CreateInvoice)
        }
    }

    fun startEditInvoice(invoiceWithItems: InvoiceWithItems) {
        val inv = invoiceWithItems.invoice
        _generatedPdfFile.value = null
        _draftState.value = InvoiceDraftState(
            editingInvoiceId = inv.id,
            invoiceNumber = inv.invoiceNumber,
            invoiceDate = inv.invoiceDate,
            dueDate = inv.dueDate,
            paymentStatus = inv.paymentStatus,
            customerId = inv.customerId,
            customerName = inv.customerName,
            customerPhone = inv.customerPhone,
            customerEmail = inv.customerEmail,
            customerAddress = inv.customerAddress,
            currency = inv.currency,
            templateId = inv.templateId,
            items = invoiceWithItems.items.map { item ->
                DraftInvoiceItem(
                    localId = UUID.randomUUID().toString(),
                    productId = item.productId,
                    name = item.name,
                    description = item.description,
                    unit = item.unit,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discountAmount = item.discountAmount,
                    taxPercent = item.taxPercent
                )
            },
            partialPaidInput = if (inv.paymentStatus == "PARTIAL") inv.amountPaid.toString() else "",
            notes = inv.notes,
            termsAndConditions = inv.termsAndConditions,
            validationError = null
        )
        navigateTo(AppScreen.CreateInvoice)
    }

    fun duplicateInvoice(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val nextNumber = repository.generateNextUniqueInvoiceNumber()
            val inv = invoiceWithItems.invoice
            val duplicatedEntity = inv.copy(
                id = 0L,
                invoiceNumber = nextNumber,
                invoiceDate = FormatUtils.todayDateString(),
                invoiceDateMillis = System.currentTimeMillis(),
                dueDate = FormatUtils.dueDateString(15),
                paymentStatus = "UNPAID",
                amountPaid = 0.0,
                remainingBalance = inv.grandTotal,
                createdAt = System.currentTimeMillis()
            )
            val newId = repository.saveInvoice(duplicatedEntity, invoiceWithItems.items)
            showMessage("Invoice duplicated as $nextNumber")
            _generatedPdfFile.value = null
            navigateTo(AppScreen.InvoicePreview(invoiceId = newId, useDraft = false))
        }
    }

    fun updateDraftField(updater: (InvoiceDraftState) -> InvoiceDraftState) {
        _draftState.update { current ->
            updater(current).copy(validationError = null)
        }
    }

    fun selectCustomerForDraft(customer: CustomerEntity) {
        _draftState.update { current ->
            current.copy(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                customerEmail = customer.email,
                customerAddress = customer.address,
                validationError = null
            )
        }
    }

    fun addOrUpdateDraftItem(item: DraftInvoiceItem) {
        _draftState.update { current ->
            val existingIdx = current.items.indexOfFirst { it.localId == item.localId }
            val updatedItems = if (existingIdx >= 0) {
                current.items.toMutableList().apply { set(existingIdx, item) }
            } else {
                current.items + item
            }
            current.copy(items = updatedItems, validationError = null)
        }
    }

    fun removeDraftItem(localId: String) {
        _draftState.update { current ->
            current.copy(
                items = current.items.filterNot { it.localId == localId },
                validationError = null
            )
        }
    }

    suspend fun validateDraft(): String? {
        val draft = _draftState.value
        if (draft.invoiceNumber.isBlank()) {
            return "Invoice number cannot be empty."
        }
        val isUnique = repository.isInvoiceNumberUnique(draft.invoiceNumber, draft.editingInvoiceId)
        if (!isUnique) {
            return "Invoice number '${draft.invoiceNumber}' already exists. Please use a unique invoice number."
        }
        if (draft.customerName.isBlank()) {
            return "Customer name cannot be empty."
        }
        if (draft.customerEmail.isNotBlank() && !FormatUtils.isValidEmail(draft.customerEmail)) {
            return "Please enter a valid customer email address."
        }
        if (draft.items.isEmpty()) {
            return "Invoice must contain at least one item."
        }
        for (item in draft.items) {
            if (item.name.isBlank()) {
                return "All invoice items must have a name."
            }
            if (item.quantity <= 0.0) {
                return "Quantity for '${item.name}' must be greater than 0."
            }
            if (item.unitPrice < 0.0) {
                return "Unit price for '${item.name}' cannot be negative."
            }
            if (item.discountAmount < 0.0) {
                return "Discount for '${item.name}' cannot be negative."
            }
        }
        return null
    }

    fun previewDraftInvoice() {
        viewModelScope.launch {
            val error = validateDraft()
            if (error != null) {
                _draftState.update { it.copy(validationError = error) }
                showMessage(error)
                return@launch
            }
            _generatedPdfFile.value = null
            navigateTo(AppScreen.InvoicePreview(invoiceId = null, useDraft = true))
        }
    }

    fun saveDraftInvoice(onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val error = validateDraft()
            if (error != null) {
                _draftState.update { it.copy(validationError = error) }
                showMessage(error)
                return@launch
            }
            val draft = _draftState.value
            // Auto-save new customer to Customers list if not already saved
            var resolvedCustomerId = draft.customerId
            if (resolvedCustomerId == null && draft.customerName.isNotBlank()) {
                val existing = customers.value.firstOrNull {
                    it.name.equals(draft.customerName.trim(), ignoreCase = true)
                }
                resolvedCustomerId = existing?.id ?: repository.saveCustomer(
                    CustomerEntity(
                        name = draft.customerName.trim(),
                        phone = draft.customerPhone.trim(),
                        email = draft.customerEmail.trim(),
                        address = draft.customerAddress.trim()
                    )
                )
            }

            val entity = draft.copy(customerId = resolvedCustomerId).toInvoiceEntity()
            val itemEntities = draft.items.map { it.toEntity(entity.id) }
            val savedId = repository.saveInvoice(entity, itemEntities)
            _draftState.update { it.copy(editingInvoiceId = savedId, customerId = resolvedCustomerId) }
            showMessage("Invoice ${entity.invoiceNumber} saved successfully!")
            if (onSaved != null) {
                onSaved(savedId)
            } else {
                navigateTo(AppScreen.InvoicePreview(invoiceId = savedId, useDraft = false))
            }
        }
    }

    fun generatePdfForInvoice(
        context: Context,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        onResult: (File?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val profile = repository.getBusinessProfileOnce()
            val result = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generateInvoicePdf(
                    context = context,
                    profile = profile,
                    invoice = invoice,
                    items = items,
                    templateId = invoice.templateId
                )
            }
            result.onSuccess { file ->
                _generatedPdfFile.value = file
                showMessage("A4 PDF generated: ${file.name}")
                onResult(file)
            }.onFailure { err ->
                showMessage("PDF generation failed: ${err.localizedMessage ?: "Unknown error"}")
                onResult(null)
            }
        }
    }

    fun setInvoiceSearchQuery(query: String) {
        _invoiceSearchQuery.value = query
    }

    fun setInvoiceStatusFilter(filter: String) {
        _invoiceStatusFilter.value = filter
    }

    fun toggleInvoiceSortOrder() {
        _invoiceSortDescending.update { !it }
    }

    fun markInvoiceStatus(invoice: InvoiceEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoice, newStatus)
            showMessage("Invoice ${invoice.invoiceNumber} marked as $newStatus")
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            showMessage("Invoice ${invoice.invoiceNumber} deleted")
        }
    }

    // --- Customers ---
    fun saveCustomer(
        id: Long = 0L,
        name: String,
        phone: String,
        email: String,
        address: String,
        onComplete: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            showMessage("Customer name cannot be empty.")
            return
        }
        if (email.isNotBlank() && !FormatUtils.isValidEmail(email)) {
            showMessage("Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            repository.saveCustomer(
                CustomerEntity(
                    id = id,
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim()
                )
            )
            showMessage(if (id == 0L) "Customer added" else "Customer updated")
            onComplete()
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showMessage("Customer '${customer.name}' deleted")
        }
    }

    // --- Products / Services ---
    fun saveProduct(
        id: Long = 0L,
        name: String,
        description: String,
        defaultPrice: Double,
        taxPercent: Double,
        unit: String,
        onComplete: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            showMessage("Product/service name cannot be empty.")
            return
        }
        if (defaultPrice < 0.0) {
            showMessage("Price cannot be negative.")
            return
        }
        viewModelScope.launch {
            repository.saveProduct(
                ProductEntity(
                    id = id,
                    name = name.trim(),
                    description = description.trim(),
                    defaultPrice = defaultPrice,
                    taxPercent = taxPercent.coerceAtLeast(0.0),
                    unit = unit.trim().ifBlank { "pcs" }
                )
            )
            showMessage(if (id == 0L) "Product/Service added" else "Product/Service updated")
            onComplete()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showMessage("Product '${product.name}' deleted")
        }
    }

    // --- Business Profile & Logo ---
    fun saveBusinessProfile(profile: BusinessProfileEntity, onComplete: () -> Unit = {}) {
        if (profile.email.isNotBlank() && !FormatUtils.isValidEmail(profile.email)) {
            showMessage("Please enter a valid business email address.")
            return
        }
        viewModelScope.launch {
            repository.saveBusinessProfile(profile)
            showMessage("Business Profile saved!")
            onComplete()
        }
    }

    fun copyLogoUriToInternalStorage(context: Context, uri: Uri, onSavedPath: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val logosDir = File(context.filesDir, "logos")
                    if (!logosDir.exists()) logosDir.mkdirs()
                    val destFile = File(logosDir, "business_logo_${System.currentTimeMillis()}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile.absolutePath
                } catch (_: Exception) {
                    null
                }
            }
            if (result != null) {
                onSavedPath(result)
                showMessage("Business logo updated")
            } else {
                showMessage("Could not load selected image")
            }
        }
    }

    // --- App Settings, Backup & Restore ---
    fun saveAppSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveAppSettings(settings)
            showMessage("Settings updated")
        }
    }

    fun exportBackupToUri(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { repository.exportBackupJson() }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                showMessage("Backup exported successfully!")
            } catch (e: Exception) {
                showMessage("Backup export failed: ${e.localizedMessage ?: "Storage error"}")
            }
        }
    }

    fun importBackupFromUri(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
                }
                if (json.isNullOrBlank()) {
                    showMessage("Selected backup file is empty.")
                    return@launch
                }
                val res = withContext(Dispatchers.IO) { repository.importBackupJson(json) }
                res.onSuccess { count ->
                    showMessage("Backup restored successfully ($count invoices loaded)!")
                }.onFailure { err ->
                    showMessage("Restore failed: ${err.localizedMessage ?: "Invalid backup file"}")
                }
            } catch (e: Exception) {
                showMessage("Could not read backup file: ${e.localizedMessage ?: "Storage error"}")
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            _generatedPdfFile.value = null
            showMessage("All application data has been deleted.")
        }
    }

    fun filterInvoicesByPeriod(
        allInvoices: List<InvoiceWithItems>,
        period: ReportPeriod
    ): List<InvoiceWithItems> {
        if (period == ReportPeriod.ALL_TIME) return allInvoices
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val startMillis = when (period) {
            ReportPeriod.TODAY -> cal.timeInMillis
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            ReportPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            ReportPeriod.ALL_TIME -> 0L
        }
        return allInvoices.filter { it.invoice.invoiceDateMillis >= startMillis }
    }

    companion object {
        fun provideFactory(repository: InvoiceRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InvoiceViewModel(repository) as T
                }
            }
        }
    }
}
