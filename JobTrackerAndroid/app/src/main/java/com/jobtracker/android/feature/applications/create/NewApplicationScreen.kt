package com.jobtracker.android.feature.applications.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jobtracker.android.core.domain.model.AppStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewApplicationScreen(
    viewModel: NewApplicationViewModel,
    onCreated: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { if (state.created) onCreated() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New application") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmartFillCard(
                description = state.description,
                onDescriptionChange = viewModel::onDescriptionChange,
                onParse = viewModel::parseDescription,
                parsing = state.parsing,
                canParse = state.canParse,
                parseError = state.parseError,
                filledOnce = state.parseFilledOnce,
            )

            OutlinedTextField(
                value = state.company,
                onValueChange = { v -> viewModel.onField { copy(company = v) } },
                label = { Text("Company *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.role,
                onValueChange = { v -> viewModel.onField { copy(role = v) } },
                label = { Text("Role *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            StatusDropdown(
                current = state.status,
                onSelect = { v -> viewModel.onField { copy(status = v) } },
            )

            OutlinedTextField(
                value = state.source,
                onValueChange = { v -> viewModel.onField { copy(source = v) } },
                label = { Text("Source (e.g., LinkedIn)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.location,
                onValueChange = { v -> viewModel.onField { copy(location = v) } },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.appliedAt,
                onValueChange = { v -> viewModel.onField { copy(appliedAt = v) } },
                label = { Text("Applied date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.salaryMin,
                    onValueChange = { v -> viewModel.onField { copy(salaryMin = v) } },
                    label = { Text("Salary min") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.salaryMax,
                    onValueChange = { v -> viewModel.onField { copy(salaryMax = v) } },
                    label = { Text("Salary max") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.currency,
                    onValueChange = { v -> viewModel.onField { copy(currency = v) } },
                    label = { Text("Cur") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f),
                )
            }

            OutlinedTextField(
                value = state.jobLink,
                onValueChange = { v -> viewModel.onField { copy(jobLink = v) } },
                label = { Text("Job link") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.gotCall,
                    onCheckedChange = { v -> viewModel.onField { copy(gotCall = v) } },
                )
                Text("Got a call")
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { v -> viewModel.onField { copy(notes = v) } },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartFillCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onParse: () -> Unit,
    parsing: Boolean,
    canParse: Boolean,
    parseError: String?,
    filledOnce: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✨ Smart fill", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Paste a sentence or two about the job. We'll auto-fill the form below — you can still edit any field before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Job description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (filledOnce && parseError == null) {
                    Text(
                        text = "Filled — review fields below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Button(onClick = onParse, enabled = canParse) {
                    if (parsing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Parse")
                    }
                }
            }
            parseError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusDropdown(current: AppStatus, onSelect: (AppStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = current.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { expanded = true }) { Text("Change") }
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(status)
                    },
                )
            }
        }
    }
}
