package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.InvoiceWithItems
import com.example.ui.ReportPeriod
import com.example.ui.components.ScreenTopBar
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    allInvoices: List<InvoiceWithItems>,
    defaultCurrency: String,
    filterByPeriod: (List<InvoiceWithItems>, ReportPeriod) -> List<InvoiceWithItems>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    val filtered = remember(allInvoices, selectedPeriod) {
        filterByPeriod(allInvoices, selectedPeriod)
    }

    val totalSales = filtered.sumOf { it.invoice.grandTotal }
    val paidAmount = filtered.sumOf { it.invoice.amountPaid }
    val unpaidAmount = filtered.sumOf { it.invoice.remainingBalance }
    val invoiceCount = filtered.size

    // Monthly Sales Grouping (last 6 months / existing months)
    val monthlySales = remember(allInvoices) {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)
        allInvoices
            .groupBy { monthFormat.format(Date(it.invoice.invoiceDateMillis)) }
            .mapValues { entry -> entry.value.sumOf { it.invoice.grandTotal } }
            .entries
            .take(6)
            .reversed()
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "Dashboard Reports",
                subtitle = "Sales, collections & monthly performance",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Period Filter Chips
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Report Time Range",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = selectedPeriod == period,
                                    onClick = { selectedPeriod = period },
                                    label = { Text(period.label) },
                                    modifier = Modifier.testTag("report_period_${period.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Key Metrics Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportStatCard(
                            label = "Total Sales",
                            value = FormatUtils.formatMoney(totalSales, defaultCurrency),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatCard(
                            label = "Number of Invoices",
                            value = invoiceCount.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportStatCard(
                            label = "Paid Amount",
                            value = FormatUtils.formatMoney(paidAmount, defaultCurrency),
                            color = Color(0xFF15803D),
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatCard(
                            label = "Unpaid Amount",
                            value = FormatUtils.formatMoney(unpaidAmount, defaultCurrency),
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Monthly Sales Chart Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Monthly Sales Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (monthlySales.isEmpty()) {
                            Text(
                                text = "Create invoices to view your monthly sales chart.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxVal = (monthlySales.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
                            val barColor = MaterialTheme.colorScheme.primary

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                val count = monthlySales.size
                                val slotWidth = size.width / count.coerceAtLeast(1)
                                val barWidth = (slotWidth * 0.55f).coerceAtMost(90f)

                                monthlySales.forEachIndexed { idx, entry ->
                                    val ratio = (entry.value / maxVal).toFloat().coerceIn(0.06f, 1f)
                                    val barHeight = size.height * ratio
                                    val left = (idx * slotWidth) + (slotWidth - barWidth) / 2f
                                    val top = size.height - barHeight
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(12f, 12f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            monthlySales.forEach { (month, total) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(month, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = FormatUtils.formatMoney(total, defaultCurrency),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
