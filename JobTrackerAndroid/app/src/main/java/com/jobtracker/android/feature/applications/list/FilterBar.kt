package com.jobtracker.android.feature.applications.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.feature.applications.ApplicationsRepository.Filters

private val MONTHS = (1..12).toList()
private val YEARS = (2023..2026).toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    filters: Filters,
    onStatus: (AppStatus?) -> Unit,
    onSearch: (String) -> Unit,
    onMonth: (Int?) -> Unit,
    onYear: (Int?) -> Unit,
    onGotCall: (Boolean?) -> Unit,
    onSortBy: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = filters.search,
            onValueChange = onSearch,
            label = { Text("Search company / role") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            item {
                FilterChip(
                    selected = filters.status == null,
                    onClick = { onStatus(null) },
                    label = { Text("All") },
                )
            }
            items(AppStatus.entries.toList(), key = { it.name }) { status ->
                FilterChip(
                    selected = filters.status == status,
                    onClick = { onStatus(if (filters.status == status) null else status) },
                    label = { Text(status.displayName) },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DropdownPicker(
                label = filters.month?.let { "Month: $it" } ?: "All months",
                options = listOf<Int?>(null) + MONTHS,
                onSelect = onMonth,
                render = { it?.toString() ?: "All months" },
            )
            DropdownPicker(
                label = filters.year?.let { "Year: $it" } ?: "All years",
                options = listOf<Int?>(null) + YEARS,
                onSelect = onYear,
                render = { it?.toString() ?: "All years" },
            )
            DropdownPicker(
                label = when (filters.gotCall) { true -> "Call: yes"; false -> "Call: no"; null -> "Call: any" },
                options = listOf(null, true, false),
                onSelect = onGotCall,
                render = { when (it) { true -> "Yes"; false -> "No"; null -> "Any" } },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            DropdownPicker(
                label = "Sort: ${filters.sortBy}",
                options = listOf("appliedAt", "createdAt"),
                onSelect = onSortBy,
                render = { it },
            )
            TextButton(onClick = onClear, modifier = Modifier.padding(start = 8.dp)) {
                Text("Clear filters")
            }
        }
    }
}

@Composable
private fun <T> DropdownPicker(
    label: String,
    options: List<T>,
    onSelect: (T) -> Unit,
    render: (T) -> String,
) {
    var expanded by remember { mutableStateOf(false) }
    AssistChip(
        onClick = { expanded = true },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(),
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(render(option)) },
                onClick = {
                    expanded = false
                    onSelect(option)
                },
            )
        }
    }
}
