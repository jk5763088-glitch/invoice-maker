package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    // Business Profile
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getBusinessProfileFlow(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getBusinessProfileOnce(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusinessProfile(profile: BusinessProfileEntity)

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppSettings(settings: AppSettingsEntity)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // Products / Services
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // Invoices & Items
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY invoiceDateMillis DESC, id DESC")
    fun getAllInvoicesWithItemsFlow(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceWithItemsById(invoiceId: Long): InvoiceWithItems?

    @Query("SELECT * FROM invoices ORDER BY id ASC")
    suspend fun getAllInvoicesOnce(): List<InvoiceEntity>

    @Query("SELECT * FROM invoice_items ORDER BY id ASC")
    suspend fun getAllInvoiceItemsOnce(): List<InvoiceItemEntity>

    @Query("SELECT COUNT(*) FROM invoices WHERE UPPER(invoiceNumber) = UPPER(:invoiceNumber) AND id != :excludeId")
    suspend fun countInvoiceWithNumber(invoiceNumber: String, excludeId: Long = 0): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Transaction
    suspend fun saveInvoiceWithItems(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Long {
        val savedInvoiceId = if (invoice.id == 0L) {
            insertInvoice(invoice)
        } else {
            updateInvoice(invoice)
            deleteItemsForInvoice(invoice.id)
            invoice.id
        }
        val itemsWithId = items.map { it.copy(id = 0, invoiceId = savedInvoiceId) }
        insertInvoiceItems(itemsWithId)
        return savedInvoiceId
    }

    // Clear all data
    @Query("DELETE FROM invoice_items")
    suspend fun clearAllInvoiceItems()

    @Query("DELETE FROM invoices")
    suspend fun clearAllInvoices()

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Query("DELETE FROM business_profile")
    suspend fun clearBusinessProfile()

    @Query("DELETE FROM app_settings")
    suspend fun clearAppSettings()

    @Transaction
    suspend fun deleteAllData() {
        clearAllInvoiceItems()
        clearAllInvoices()
        clearAllCustomers()
        clearAllProducts()
        clearBusinessProfile()
        clearAppSettings()
    }
}
