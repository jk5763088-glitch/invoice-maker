package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BusinessProfileEntity
import com.example.data.CustomerEntity
import com.example.data.InvoiceRepository
import com.example.data.ProductEntity
import com.example.ui.DraftInvoiceItem
import com.example.ui.InvoiceDraftState
import com.example.util.FormatUtils
import com.example.util.PdfInvoiceGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: InvoiceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = InvoiceRepository(database.invoiceDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Invoice Maker", appName)
    }

    @Test
    fun `complete end to end invoice maker workflow`() = runBlocking {
        // 1. Initialize defaults
        repository.ensureInitialized()

        // 2. Create Business Profile
        val profile = BusinessProfileEntity(
            id = 1,
            businessName = "Prime Tech Solutions",
            ownerName = "Ali Raza",
            phone = "+92 300 1234567",
            whatsapp = "+92 300 1234567",
            email = "billing@primetech.pk",
            address = "Plot 14, Blue Area",
            city = "Islamabad",
            taxNtnNumber = "NTN-884920-1",
            defaultCurrency = "PKR"
        )
        repository.saveBusinessProfile(profile)
        val loadedProfile = repository.getBusinessProfileOnce()
        assertEquals("Prime Tech Solutions", loadedProfile.businessName)
        assertEquals("PKR", loadedProfile.defaultCurrency)

        // 3. Add Customer
        val customerId = repository.saveCustomer(
            CustomerEntity(
                name = "Crescent Traders",
                phone = "+92 321 9876543",
                email = "accounts@crescent.com",
                address = "Main Boulevard, Lahore"
            )
        )
        assertTrue(customerId > 0L)

        // 4. Add Products / Services
        val prod1Id = repository.saveProduct(
            ProductEntity(
                name = "POS Thermal Printer",
                description = "80mm Auto-Cutter USB/LAN",
                defaultPrice = 25000.0,
                taxPercent = 10.0,
                unit = "pcs"
            )
        )
        val prod2Id = repository.saveProduct(
            ProductEntity(
                name = "On-Site Installation",
                description = "Setup and staff training",
                defaultPrice = 5000.0,
                taxPercent = 5.0,
                unit = "service"
            )
        )
        assertTrue(prod1Id > 0L && prod2Id > 0L)

        // 5. Generate first unique invoice number & create draft with multiple items
        val invNum1 = repository.generateNextUniqueInvoiceNumber()
        assertEquals("INV-0001", invNum1)

        val item1 = DraftInvoiceItem(
            productId = prod1Id,
            name = "POS Thermal Printer",
            description = "80mm Auto-Cutter USB/LAN",
            unit = "pcs",
            quantity = 2.0,
            unitPrice = 25000.0,
            discountAmount = 2000.0,
            taxPercent = 10.0
        )
        val item2 = DraftInvoiceItem(
            productId = prod2Id,
            name = "On-Site Installation",
            description = "Setup and staff training",
            unit = "service",
            quantity = 1.0,
            unitPrice = 5000.0,
            discountAmount = 0.0,
            taxPercent = 5.0
        )

        val draft = InvoiceDraftState(
            invoiceNumber = invNum1,
            invoiceDate = FormatUtils.todayDateString(),
            dueDate = FormatUtils.dueDateString(15),
            paymentStatus = "UNPAID",
            customerId = customerId,
            customerName = "Crescent Traders",
            customerPhone = "+92 321 9876543",
            customerEmail = "accounts@crescent.com",
            customerAddress = "Main Boulevard, Lahore",
            currency = "PKR",
            templateId = "MODERN",
            items = listOf(item1, item2),
            notes = "Thank you for choosing Prime Tech!",
            termsAndConditions = "1 Year Hardware Warranty."
        )

        // 7. Verify calculations
        // Item 1: gross = 50,000, after disc = 48,000, tax 10% = 4,800, itemTotal = 52,800
        // Item 2: gross = 5,000, after disc = 5,000, tax 5% = 250, itemTotal = 5,250
        assertEquals(55000.0, draft.subtotal, 0.01)
        assertEquals(2000.0, draft.discountTotal, 0.01)
        assertEquals(5050.0, draft.taxTotal, 0.01)
        assertEquals(58050.0, draft.grandTotal, 0.01)
        assertEquals(0.0, draft.amountPaid, 0.01)
        assertEquals(58050.0, draft.remainingBalance, 0.01)

        // 8 & 9. Generate A4 PDF across all 3 templates (Simple, Modern, Business)
        val invoiceEntity = draft.toInvoiceEntity()
        val itemEntities = draft.items.map { it.toEntity() }
        for (tpl in listOf("SIMPLE", "MODERN", "BUSINESS")) {
            val pdfResult = PdfInvoiceGenerator.generateInvoicePdf(
                context = context,
                profile = loadedProfile,
                invoice = invoiceEntity.copy(templateId = tpl),
                items = itemEntities,
                templateId = tpl
            )
            assertTrue("PDF should succeed for $tpl", pdfResult.isSuccess)
            val pdfFile = pdfResult.getOrNull()
            assertNotNull(pdfFile)
            assertTrue(pdfFile!!.exists() && pdfFile.length() > 500L)
        }

        // 12. Save Invoice
        val savedInvoiceId = repository.saveInvoice(invoiceEntity, itemEntities)
        assertTrue(savedInvoiceId > 0L)
        assertFalse(repository.isInvoiceNumberUnique("INV-0001", excludeInvoiceId = 0L))

        // 13. Edit Invoice & 14. Mark Invoice as Paid
        val savedWithItems = repository.getInvoiceWithItems(savedInvoiceId)
        assertNotNull(savedWithItems)
        assertEquals(2, savedWithItems!!.items.size)

        repository.updateInvoiceStatus(savedWithItems.invoice, "PAID")
        val updatedPaid = repository.getInvoiceWithItems(savedInvoiceId)!!
        assertEquals("PAID", updatedPaid.invoice.paymentStatus)
        assertEquals(58050.0, updatedPaid.invoice.amountPaid, 0.01)
        assertEquals(0.0, updatedPaid.invoice.remainingBalance, 0.01)

        // 16. Generate another invoice & verify next number is INV-0002
        val invNum2 = repository.generateNextUniqueInvoiceNumber()
        assertEquals("INV-0002", invNum2)
        val secondId = repository.saveInvoice(
            invoiceEntity.copy(
                id = 0L,
                invoiceNumber = invNum2,
                customerName = "Metro Mart"
            ),
            itemEntities
        )
        assertTrue(secondId > 0L)

        // 17 & 18. Backup Export & Import persistence verification
        val backupJson = repository.exportBackupJson()
        assertTrue(backupJson.contains("INV-0001"))
        assertTrue(backupJson.contains("INV-0002"))
        assertTrue(backupJson.contains("Crescent Traders"))

        repository.deleteAllData()
        val restoreRes = repository.importBackupJson(backupJson)
        assertTrue(restoreRes.isSuccess)
        assertEquals(2, restoreRes.getOrNull())
    }
}
