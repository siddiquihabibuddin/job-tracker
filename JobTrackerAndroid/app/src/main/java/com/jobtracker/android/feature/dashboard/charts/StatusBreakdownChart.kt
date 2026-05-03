package com.jobtracker.android.feature.dashboard.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jobtracker.android.core.domain.model.AppStatus
import kotlin.math.max

@Composable
fun StatusBreakdownChart(
    byStatus: Map<String, Long>,
    modifier: Modifier = Modifier,
) {
    val rows = remember(byStatus) { computeRows(byStatus) }
    if (rows.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val maxValue = remember(rows) { max(1L, rows.maxOf { it.count }) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(96.dp),
                )
                val ratio = row.count.toFloat() / maxValue.toFloat()
                Box(modifier = Modifier.weight(1f).height(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(0.02f))
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(row.color),
                    )
                }
                Text(
                    text = row.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp).width(36.dp),
                )
            }
        }
    }
}

private data class Row(val label: String, val count: Long, val color: Color)

private fun computeRows(byStatus: Map<String, Long>): List<Row> =
    AppStatus.entries.mapNotNull { status ->
        val count = byStatus[status.name] ?: 0L
        if (count == 0L) null else Row(label = status.displayName, count = count, color = colorFor(status))
    }

private fun colorFor(status: AppStatus): Color = when (status) {
    AppStatus.APPLIED -> Color(0xFF3B82F6)
    AppStatus.PHONE -> Color(0xFFA855F7)
    AppStatus.ONSITE -> Color(0xFFEA580C)
    AppStatus.OFFER -> Color(0xFF16A34A)
    AppStatus.ACCEPTED -> Color(0xFF059669)
    AppStatus.REJECTED -> Color(0xFFDC2626)
    AppStatus.WITHDRAWN -> Color(0xFF6B7280)
}
