package com.jobtracker.android.feature.applications.list

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jobtracker.android.core.domain.model.AppStatus

@Composable
fun StatusChip(status: AppStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        AppStatus.APPLIED -> Color(0xFF3B82F6)
        AppStatus.PHONE -> Color(0xFFA855F7)
        AppStatus.ONSITE -> Color(0xFFEA580C)
        AppStatus.OFFER -> Color(0xFF16A34A)
        AppStatus.ACCEPTED -> Color(0xFF059669)
        AppStatus.REJECTED -> Color(0xFFDC2626)
        AppStatus.WITHDRAWN -> Color(0xFF6B7280)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(status.displayName) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.15f),
            disabledLabelColor = color,
        ),
        modifier = modifier,
    )
}
