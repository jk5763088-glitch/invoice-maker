package com.example.data

import com.example.util.FormatUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val dao: InvoiceDao) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val businessProfileFlow: Flow<BusinessProfileEntity?> = dao.getBusinessProfileFlow()
    val appSettingsFlow: Flow<AppSettingsEntity?> = dao.getAppSettingsFlow()
    val customersFlow: Flow<List<CustomerEntity>> = dao.getAllCustomersFlow()
    val productsFlow: Flow<List<ProductEntity>> = dao.getAllProductsFlow()
    val invoicesWithItemsFlow: Flow<List<InvoiceWithItems>> = dao.getAllInvoicesWithItemsFlow()

    suspend fun ensureInitialized() {
        if (dao.getBusinessProfileOnce() == null) {
            dao.upsertBusinessProfile(BusinessProfileEntity(id = 1))
        }
        if (dao.getAppSettingsOnce() == null) {
            dao.upsertAppSettings(AppSettingsEntity(id = 1))
        }
    }

    suspend fun getBusinessProfileOnce(): BusinessProfileEntity {
        return dao.getBusinessProfileOnce() ?: BusinessProfileEntity(id = 1)
    }

    suspend fun saveBusinessProfile(profile: BusinessProfileEntity) {
        dao.upsertBusinessProfile(profile.copy(id = 1))
        val settings = getAppSettingsOnce()
        if (settings.defaultCurrency != profile.defaultCurrency) {
            dao.upsertAppSettings(settings.copy(defaultCurrency = profile.defaultCurrency))
        }
    }

    suspend fun getAppSettingsOnce(): AppSettingsEntity {
        return dao.getAppSettingsOnce() ?: AppSettingsEntity(id = 1)
    }

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        dao.upsertAppSettings(settings.copy(id = 1))
        val profile = getBusinessProfileOnce()
        if (profile.defaultCurrency != settings.defaultCurrency) {
            dao.upsertBusinessProfile(profile.copy(defaultCurrency = settings.defaultCurrency))
        }
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            dao.insertCustomer(customer)
        } else {
            dao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        dao.deleteCustomer(customer)
    }

    suspend fun saveProduct(product: ProductEntity): Long {
        return if (product.id == 0L) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
    }

    suspend fun generateNextUniqueInvoiceNumber(): String {
        val settings = getAppSettingsOnce()
        var candidateNum = settings.nextInvoiceNumber.coerceAtLeast(1)
        var formatted = FormatUtils.formatInvoiceNumber(settings.invoicePrefix, candidateNum)
        while (dao.countInvoiceWithNumber(formatted, 0L) > 0) {
            candidateNum++
            formatted = FormatUtils.formatInvoiceNumber(settings.invoicePrefix, candidateNum)
        }
        return formatted
    }

    suspend fun isInvoiceNumberUnique(invoiceNumber: String, excludeInvoiceId: Long): Boolean {
        if (invoiceNumber.isBlank()) return false
        return dao.countInvoiceWithNumber(invoiceNumber.trim(), excludeInvoiceId) == 0
    }

    suspend fun getInvoiceWithItems(invoiceId: Long): InvoiceWithItems? {
        return dao.getInvoiceWithItemsById(invoiceId)
    }

    suspend fun saveInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Long {
        val isNew = invoice.id == 0L
        val savedId = dao.saveInvoiceWithItems(invoice, items)
        if (isNew) {
            val settings = getAppSettingsOnce()
            val digitsOnly = invoice.invoiceNumber.filter { it.isDigit() }.toIntOrNull()
            val nextNum = if (digitsOnly != null && digitsOnly >= settings.nextInvoiceNumber) {
                digitsOnly + 1
            } else {
                settings.nextInvoiceNumber + 1
            }
            dao.upsertAppSettings(settings.copy(nextInvoiceNumber = nextNum))
        }
        return savedId
    }

    suspend fun updateInvoiceStatus(invoice: InvoiceEntity, newStatus: String) {
        val updated = when (newStatus) {
            "PAID" -> invoice.copy(
                paymentStatus = "PAID",
                amountPaid = invoice.grandTotal,
                remainingBalance = 0.0
            )
            "UNPAID" -> invoice.copy(
                paymentStatus = "UNPAID",
                amountPaid = 0.0,
                remainingBalance = invoice.grandTotal
            )
            else -> invoice.copy(paymentStatus = newStatus)
        }
        dao.updateInvoice(updated)
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        dao.deleteInvoice(invoice)
    }

    suspend fun exportBackupJson(): String {
        val payload = BackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            businessProfile = dao.getBusinessProfileOnce(),
            appSettings = dao.getAppSettingsOnce(),
            customers = dao.getAllCustomersOnce(),
            products = dao.getAllProductsOnce(),
            invoices = dao.getAllInvoicesOnce(),
            invoiceItems = dao.getAllInvoiceItemsOnce()
        )
        val adapter = moshi.adapter(BackupPayload::class.java).indent("  ")
        return adapter.toJson(payload)
    }

    suspend fun importBackupJson(json: String): Result<Int> {
        return try {
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(json)
                ?: return Result.failure(IllegalArgumentException("Invalid backup file format."))
            dao.deleteAllData()
            payload.businessProfile?.let { dao.upsertBusinessProfile(it.copy(id = 1)) }
            payload.appSettings?.let { dao.upsertAppSettings(it.copy(id = 1)) }
            payload.customers.forEach { dao.insertCustomer(it) }
            payload.products.forEach { dao.insertProduct(it) }
            payload.invoices.forEach { dao.insertInvoice(it) }
            if (payload.invoiceItems.isNotEmpty()) {
                dao.insertInvoiceItems(payload.invoiceItems)
            }
            ensureInitialized()
            Result.success(payload.invoices.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllData() {
        dao.deleteAllData()
        ensureInitialized()
    }
}
