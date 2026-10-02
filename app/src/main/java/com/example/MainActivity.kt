package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.InvoiceRepository
import com.example.ui.AppScreen
import com.example.ui.InvoiceViewModel
import com.example.ui.screens.BusinessProfileScreen
import com.example.ui.screens.CreateInvoiceScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoicePreviewScreen
import com.example.ui.screens.MyInvoicesScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PdfInvoiceGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = InvoiceRepository(database.invoiceDao())

        setContent {
            val invoiceViewModel: InvoiceViewModel = viewModel(
                factory = InvoiceViewModel.provideFactory(repository)
            )
            val appSettings by invoiceViewModel.appSettings.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = appSettings.themeMode) {
                InvoiceMakerApp(viewModel = invoiceViewModel)
            }
        }
    }
}

@Composable
fun InvoiceMakerApp(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val invoices by viewModel.invoicesWithItems.collectAsStateWithLifecycle()
    val draftState by viewModel.draftState.collectAsStateWithLifecycle()
    val generatedPdfFile by viewModel.generatedPdfFile.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val searchQuery by viewModel.invoiceSearchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.invoiceStatusFilter.collectAsStateWithLifecycle()
    val sortDescending by viewModel.invoiceSortDescending.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val defaultCurrency = businessProfile.defaultCurrency.ifBlank { appSettings.defaultCurrency }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Home -> {
                    HomeScreen(
                        businessProfile = businessProfile,
                        appSettings = appSettings,
                        invoices = invoices,
                        onCreateNewInvoice = { viewModel.startNewInvoice() },
                        onNavigate = { target -> viewModel.navigateTo(target) },
                        onOpenInvoicePreview = { invoiceId ->
                            viewModel.navigateTo(AppScreen.InvoicePreview(invoiceId = invoiceId, useDraft = false))
                        }
                    )
                }

                is AppScreen.BusinessProfile -> {
                    BusinessProfileScreen(
                        initialProfile = businessProfile,
                        onPickLogoUri = { ctx, uri, callback ->
                            viewModel.copyLogoUriToInternalStorage(ctx, uri, callback)
                        },
                        onSaveProfile = { updated ->
                            viewModel.saveBusinessProfile(updated) {
                                viewModel.navigateBack()
                            }
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.Customers -> {
                    CustomersScreen(
                        customers = customers,
                        invoices = invoices,
                        defaultCurrency = defaultCurrency,
                        onSaveCustomer = { id, name, phone, email, address ->
                            viewModel.saveCustomer(id, name, phone, email, address)
                        },
                        onDeleteCustomer = { cust -> viewModel.deleteCustomer(cust) },
                        onCreateInvoiceForCustomer = { cust ->
                            viewModel.startNewInvoice(preselectedCustomer = cust)
                        },
                        onOpenInvoicePreview = { invId ->
                            viewModel.navigateTo(AppScreen.InvoicePreview(invoiceId = invId, useDraft = false))
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.Products -> {
                    ProductsScreen(
                        products = products,
                        defaultCurrency = defaultCurrency,
                        defaultTaxPercent = appSettings.defaultTaxPercent,
                        onSaveProduct = { id, name, desc, price, tax, unit ->
                            viewModel.saveProduct(id, name, desc, price, tax, unit)
                        },
                        onDeleteProduct = { prod -> viewModel.deleteProduct(prod) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.CreateInvoice -> {
                    CreateInvoiceScreen(
                        draft = draftState,
                        customers = customers,
                        products = products,
                        defaultTaxPercent = appSettings.defaultTaxPercent,
                        onUpdateDraft = { updater -> viewModel.updateDraftField(updater) },
                        onSelectCustomer = { cust -> viewModel.selectCustomerForDraft(cust) },
                        onSaveOrUpdateItem = { item -> viewModel.addOrUpdateDraftItem(item) },
                        onRemoveItem = { localId -> viewModel.removeDraftItem(localId) },
                        onPreviewClick = { viewModel.previewDraftInvoice() },
                        onSaveClick = { viewModel.saveDraftInvoice() },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.InvoicePreview -> {
                    val targetInvoiceWithItems = if (screen.useDraft || screen.invoiceId == null) {
                        null
                    } else {
                        invoices.firstOrNull { it.invoice.id == screen.invoiceId }
                    }

                    val previewInvoice = targetInvoiceWithItems?.invoice ?: draftState.toInvoiceEntity()
                    val previewItems = targetInvoiceWithItems?.items
                        ?: draftState.items.map { it.toEntity(previewInvoice.id) }

                    InvoicePreviewScreen(
                        profile = businessProfile,
                        invoice = previewInvoice,
                        items = previewItems,
                        isDraftMode = screen.useDraft || previewInvoice.id == 0L,
                        generatedPdfFile = generatedPdfFile,
                        onSaveDraftInvoice = { viewModel.saveDraftInvoice() },
                        onEditInvoice = {
                            if (targetInvoiceWithItems != null) {
                                viewModel.startEditInvoice(targetInvoiceWithItems)
                            } else {
                                viewModel.navigateBack()
                            }
                        },
                        onGeneratePdf = { inv, itemList ->
                            if (screen.useDraft && inv.id == 0L) {
                                viewModel.saveDraftInvoice { _ ->
                                    viewModel.generatePdfForInvoice(context, inv, itemList)
                                }
                            } else {
                                viewModel.generatePdfForInvoice(context, inv, itemList)
                            }
                        },
                        onShowMessage = { msg -> viewModel.showMessage(msg) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.MyInvoices -> {
                    MyInvoicesScreen(
                        invoices = invoices,
                        searchQuery = searchQuery,
                        statusFilter = statusFilter,
                        sortDescending = sortDescending,
                        onSearchQueryChange = { q -> viewModel.setInvoiceSearchQuery(q) },
                        onStatusFilterChange = { f -> viewModel.setInvoiceStatusFilter(f) },
                        onToggleSortOrder = { viewModel.toggleInvoiceSortOrder() },
                        onCreateNewInvoice = { viewModel.startNewInvoice() },
                        onOpenInvoice = { invWithItems ->
                            viewModel.navigateTo(
                                AppScreen.InvoicePreview(invoiceId = invWithItems.invoice.id, useDraft = false)
                            )
                        },
                        onEditInvoice = { invWithItems ->
                            viewModel.startEditInvoice(invWithItems)
                        },
                        onDuplicateInvoice = { invWithItems ->
                            viewModel.duplicateInvoice(invWithItems)
                        },
                        onTogglePaidStatus = { inv ->
                            val nextStatus = if (inv.paymentStatus.equals("PAID", ignoreCase = true)) "UNPAID" else "PAID"
                            viewModel.markInvoiceStatus(inv, nextStatus)
                        },
                        onGenerateAndSharePdf = { invWithItems, shouldShare ->
                            viewModel.generatePdfForInvoice(
                                context = context,
                                invoice = invWithItems.invoice,
                                items = invWithItems.items
                            ) { file ->
                                if (file != null && shouldShare) {
                                    PdfInvoiceGenerator.sharePdf(context, file, invWithItems.invoice)
                                } else if (file != null) {
                                    viewModel.navigateTo(
                                        AppScreen.InvoicePreview(
                                            invoiceId = invWithItems.invoice.id,
                                            useDraft = false
                                        )
                                    )
                                }
                            }
                        },
                        onDeleteInvoice = { inv -> viewModel.deleteInvoice(inv) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.Reports -> {
                    ReportsScreen(
                        allInvoices = invoices,
                        defaultCurrency = defaultCurrency,
                        filterByPeriod = { list, period -> viewModel.filterInvoicesByPeriod(list, period) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is AppScreen.Settings -> {
                    SettingsScreen(
                        settings = appSettings,
                        onOpenBusinessProfile = { viewModel.navigateTo(AppScreen.BusinessProfile) },
                        onSaveSettings = { updated -> viewModel.saveAppSettings(updated) },
                        onExportBackup = { ctx, uri -> viewModel.exportBackupToUri(ctx, uri) },
                        onImportBackup = { ctx, uri -> viewModel.importBackupFromUri(ctx, uri) },
                        onDeleteAllData = { viewModel.deleteAllData() },
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
        }
    }
}
