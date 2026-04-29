package com.jobtracker.android.feature.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jobtracker.android.core.domain.model.TrendPoint
import kotlin.math.max

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF3B82F6), // blue-500 — matches "Blue: applications" legend
    callsColor: Color = Color(0xFF22C55E), // green-500 — matches "Green: calls" legend
) {
    if (points.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxCount = max(1L, points.maxOf { max(it.count, it.callsCount) })
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp).padding(horizontal = 8.dp)) {
        val w = size.width
        val h = size.height
        val padBottom = 24f
        val padTop = 8f
        val plotH = h - padTop - padBottom

        // Grid: 4 horizontal lines.
        repeat(5) { i ->
            val y = padTop + plotH * i / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
            )
        }

        if (points.size < 2) return@Canvas
        val stepX = w / (points.size - 1f)

        fun pathFor(values: List<Long>): Path {
            val path = Path()
            values.forEachIndexed { idx, value ->
                val x = idx * stepX
                val ratio = value.toFloat() / maxCount.toFloat()
                val y = padTop + plotH * (1f - ratio)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        drawPath(
            path = pathFor(points.map { it.count }),
            color = lineColor,
            style = Stroke(width = 4f),
        )
        if (points.any { it.callsCount > 0 }) {
            drawPath(
                path = pathFor(points.map { it.callsCount }),
                color = callsColor,
                style = Stroke(width = 3f),
            )
        }
    }
}
