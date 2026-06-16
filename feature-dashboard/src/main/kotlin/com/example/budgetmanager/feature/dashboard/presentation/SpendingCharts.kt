package com.example.budgetmanager.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetmanager.core.ui.theme.ChartPalette
import com.example.budgetmanager.domain.usecase.CategorySpend
import com.example.budgetmanager.domain.usecase.MonthlyExpense
import androidx.compose.foundation.Canvas

private fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}

@Composable
fun CategoryBreakdownCard(
    breakdown: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) return
    val total = breakdown.sumOf { it.amount }.coerceAtLeast(0.01)
    val colors = breakdown.mapIndexed { index, spend ->
        parseHexColor(spend.colorHex, ChartPalette[index % ChartPalette.size])
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Donut
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                ) {
                    val stroke = 28f
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    var startAngle = -90f
                    breakdown.forEachIndexed { index, spend ->
                        val sweep = (spend.amount / total * 360f).toFloat()
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                        startAngle += sweep
                    }
                }
                Spacer(Modifier.width(20.dp))
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    breakdown.take(5).forEachIndexed { index, spend ->
                        val pct = (spend.amount / total * 100).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors[index])
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = spend.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendCard(
    trend: List<MonthlyExpense>,
    modifier: Modifier = Modifier
) {
    if (trend.isEmpty()) return
    val maxValue = trend.maxOf { it.total }.coerceAtLeast(1.0)
    val barColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Monthly Spending",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEach { month ->
                    val fraction = (month.total / maxValue).toFloat().coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = if (month.total > 0) "₹${"%.0f".format(month.total)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(barColor.copy(alpha = 0.85f))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = month.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
