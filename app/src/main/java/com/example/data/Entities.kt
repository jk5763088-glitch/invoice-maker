package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val taxNtnNumber: String = "",
    val logoPath: String = "",
    val defaultCurrency: String = "USD"
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultCurrency: String = "USD",
    val selectedTemplate: String = "MODERN",
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Int = 1,
    val defaultTaxPercent: Double = 0.0,
    val defaultPaymentTerms: String = "Payment is due within 15 days of invoice date.",
    val defaultInvoiceNotes: String = "Thank you for your business!",
    val themeMode: String = "SYSTEM",
    val isPremium: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val defaultPrice: Double = 0.0,
    val taxPercent: Double = 0.0,
    val unit: String = "pcs",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "invoices",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceDate: String,
    val invoiceDateMillis: Long,
    val dueDate: String,
    val paymentStatus: String, // PAID, UNPAID, PARTIAL
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val currency: String = "USD",
    val templateId: String = "MODERN",
    val subtotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val notes: String = "",
    val termsAndConditions: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val productId: Long? = null,
    val name: String,
    val description: String = "",
    val unit: String = "pcs",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val itemTotal: Double = 0.0
)

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val businessProfile: BusinessProfileEntity?,
    val appSettings: AppSettingsEntity?,
    val customers: List<CustomerEntity>,
    val products: List<ProductEntity>,
    val invoices: List<InvoiceEntity>,
    val invoiceItems: List<InvoiceItemEntity>
)
