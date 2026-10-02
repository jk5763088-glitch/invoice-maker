package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppSettingsEntity
import com.example.data.BusinessProfileEntity
import com.example.data.InvoiceWithItems
import com.example.ui.AppScreen
import com.example.ui.components.AdMobBannerBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentStatusBadge
import com.example.util.FormatUtils
import java.io.File

@Composable
fun HomeScreen(
    businessProfile: BusinessProfileEntity,
    appSettings: AppSettingsEntity,
    invoices: List<InvoiceWithItems>,
    onCreateNewInvoice: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onOpenInvoicePreview: (Long) -> Unit
) {
    val currency = businessProfile.defaultCurrency.ifBlank { appSettings.defaultCurrency }
    val totalInvoices = invoices.size
    val paidInvoices = invoices.count { it.invoice.paymentStatus.equals("PAID", ignoreCase = true) }
    val unpaidInvoices = invoices.count { !it.invoice.paymentStatus.equals("PAID", ignoreCase = true) }
    val totalAmount = invoices.sumOf { it.invoice.grandTotal }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. App Header with Business Logo & Name
            item {
                HomeHeaderCard(
                    businessProfile = businessProfile,
                    onBusinessProfileClick = { onNavigate(AppScreen.BusinessProfile) },
                    onSettingsClick = { onNavigate(AppScreen.Settings) }
                )
            }

            // 2. Primary CTA: Create New Invoice
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F4C81),
                                        Color(0xFF0D9488)
                                    )
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.22f
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Quick A4 Billing & PDF",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFCCFBF1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create & Share Professional Invoices in Seconds",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCreateNewInvoice,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F4C81)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("home_create_invoice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create New Invoice",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Dashboard Overview Metrics
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dashboard Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { onNavigate(AppScreen.Reports) },
                            modifier = Modifier.testTag("home_reports_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Reports")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Total Invoices",
                            value = totalInvoices.toString(),
                            icon = Icons.Default.Description,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_total_invoices")
                        )
                        DashboardMetricCard(
                            title = "Total Amount",
                            value = FormatUtils.formatMoney(totalAmount, currency),
                            icon = Icons.Default.BusinessCenter,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_total_amount")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Paid Invoices",
                            value = paidInvoices.toString(),
                            icon = Icons.Default.CheckCircle,
                            accentColor = Color(0xFF15803D),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_paid_invoices")
                        )
                        DashboardMetricCard(
                            title = "Unpaid Invoices",
                            value = unpaidInvoices.toString(),
                            icon = Icons.Default.PendingActions,
                            accentColor = Color(0xFFB91C1C),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_unpaid_invoices")
                        )
                    }
                }
            }

            // 4. Main Navigation Grid (My Invoices, Customers, Products/Services, Business Profile, Reports, Settings)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Manage Your Business",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeActionTile(
                            title = "My Invoices",
                            subtitle = "$totalInvoices saved",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            testTag = "nav_my_invoices_button",
                            onClick = { onNavigate(AppScreen.MyInvoices) },
                            modifier = Modifier.weight(1f)
                        )
                        HomeActionTile(
                            title = "Customers",
                            subtitle = "Client directory",
                            icon = Icons.Default.People,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            testTag = "nav_customers_button",
                            onClick = { onNavigate(AppScreen.Customers) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeActionTile(
                            title = "Products/Services",
                            subtitle = "Price catalog",
                            icon = Icons.Default.Inventory2,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            testTag = "nav_products_button",
                            onClick = { onNavigate(AppScreen.Products) },
                            modifier = Modifier.weight(1f)
                        )
                        HomeActionTile(
                            title = "Business Profile",
                            subtitle = businessProfile.businessName.ifBlank { "Set shop details" },
                            icon = Icons.Default.Storefront,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            testTag = "nav_business_profile_button",
                            onClick = { onNavigate(AppScreen.BusinessProfile) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeActionTile(
                            title = "Reports",
                            subtitle = "Sales & analytics",
                            icon = Icons.Default.BarChart,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            testTag = "nav_reports_button",
                            onClick = { onNavigate(AppScreen.Reports) },
                            modifier = Modifier.weight(1f)
                        )
                        HomeActionTile(
                            title = "Settings",
                            subtitle = "Templates & backup",
                            icon = Icons.Default.Settings,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            testTag = "nav_settings_button",
                            onClick = { onNavigate(AppScreen.Settings) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Recent Invoices Section or Empty State
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Invoices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (invoices.isNotEmpty()) {
                            TextButton(onClick = { onNavigate(AppScreen.MyInvoices) }) {
                                Text("See All")
                            }
                        }
                    }

                    if (invoices.isEmpty()) {
                        EmptyStateCard(
                            title = "No invoices yet",
                            subtitle = "Create your first professional A4 invoice and share it via WhatsApp or Email.",
                            buttonText = "Create Your First Invoice",
                            buttonTestTag = "empty_create_first_invoice_button",
                            onActionClick = onCreateNewInvoice
                        )
                    }
                }
            }

            items(
                items = invoices.take(5),
                key = { it.invoice.id }
            ) { item ->
                val inv = item.invoice
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .clickable { onOpenInvoicePreview(inv.id) }
                        .testTag("recent_invoice_card_${inv.invoiceNumber}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = inv.invoiceNumber,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = inv.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Date: ${inv.invoiceDate}",
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
                }
            }
        }

        // Non-intrusive AdMob Banner at bottom of Home Screen
        AdMobBannerBar(
            isPremium = appSettings.isPremium,
            onUpgradeClick = { onNavigate(AppScreen.Settings) }
        )
    }
}

@Composable
private fun HomeHeaderCard(
    businessProfile: BusinessProfileEntity,
    onBusinessProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { onBusinessProfileClick() }
        ) {
            val logoFile = businessProfile.logoPath.takeIf { it.isNotBlank() }?.let { File(it) }
            if (logoFile != null && logoFile.exists()) {
                AsyncImage(
                    model = logoFile,
                    contentDescription = "Business Logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Invoice Maker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = businessProfile.businessName.ifBlank { "Easy Invoice & PDF • Tap to set business profile" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.testTag("header_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
