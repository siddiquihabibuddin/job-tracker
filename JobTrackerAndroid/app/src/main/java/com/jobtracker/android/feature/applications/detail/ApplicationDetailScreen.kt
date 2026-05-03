package com.jobtracker.android.feature.applications.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jobtracker.android.core.domain.model.ActivityItem
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.Note
import com.jobtracker.android.feature.applications.list.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    viewModel: ApplicationDetailViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.application?.company ?: "Application") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val app = state.application
        if (app == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(app.role, style = MaterialTheme.typography.titleLarge)
            StatusChip(status = app.status)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick edit", style = MaterialTheme.typography.titleMedium)
                    StatusInlineEdit(current = app.status, onSelect = viewModel::setStatus)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = app.gotCall, onCheckedChange = viewModel::setGotCall)
                        Text("Got a call")
                    }
                    RejectDateEdit(
                        current = app.rejectDate,
                        onSet = viewModel::setRejectDate,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Overview", style = MaterialTheme.typography.titleMedium)
                    Field("Company", app.company)
                    Field("Role", app.role)
                    Field("Source", app.source ?: "—")
                    Field("Location", app.location ?: "—")
                    Field("Applied at", app.appliedAt ?: "—")
                    Field("Created at", app.createdAt ?: "—")
                    Field("Reject date", app.rejectDate ?: "—")
                    Field("Salary", formatSalary(app.salaryMin, app.salaryMax, app.currency))
                    Field("Job link", app.jobLink ?: "—")
                }
            }

            NotesCard(
                draft = state.noteDraft,
                onDraftChange = viewModel::onNoteDraftChange,
                onAdd = viewModel::addNote,
                addingNote = state.addingNote,
                notesAddedThisSession = state.notesAddedThisSession,
            )

            ActivityCard(
                items = state.activity,
                loading = state.activityLoading,
                onRefresh = viewModel::loadActivity,
            )

            if (state.updating) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this application?") },
            text = { Text("This action is a soft delete on the server. It can be undone via the backend.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatusInlineEdit(current: AppStatus, onSelect: (AppStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Status", modifier = Modifier.padding(end = 12.dp))
        Button(onClick = { expanded = true }) { Text(current.displayName) }
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

@Composable
private fun RejectDateEdit(current: String?, onSet: (String?) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(current) { mutableStateOf(current.orEmpty()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Reject date", modifier = Modifier.padding(end = 12.dp))
        if (!editing) {
            Text(current ?: "—", modifier = Modifier.padding(end = 8.dp))
            TextButton(onClick = { editing = true }) { Text("Edit") }
        } else {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            TextButton(onClick = {
                editing = false
                onSet(draft.takeIf { it.isNotBlank() })
            }) { Text("Save") }
            TextButton(onClick = { editing = false }) { Text("Cancel") }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatSalary(min: Double?, max: Double?, currency: String?): String {
    if (min == null && max == null) return "—"
    val cur = currency ?: ""
    return when {
        min != null && max != null -> "$cur${min.toLong()}–${max.toLong()}".trim()
        min != null -> "$cur${min.toLong()}+".trim()
        max != null -> "up to $cur${max.toLong()}".trim()
        else -> "—"
    }
}

@Composable
private fun NotesCard(
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    addingNote: Boolean,
    notesAddedThisSession: List<Note>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notes", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = { Text("Add a note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onAdd,
                    enabled = !addingNote && draft.isNotBlank(),
                ) {
                    if (addingNote) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Add note")
                    }
                }
            }
            if (notesAddedThisSession.isNotEmpty()) {
                Text(
                    text = "Added this session",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                notesAddedThisSession.reversed().forEach { note ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(note.body, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = note.createdAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = "Notes you add appear here. Older notes are recorded in the activity feed below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    items: List<ActivityItem>,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Activity", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRefresh, enabled = !loading) { Text("Refresh") }
            }
            when {
                loading && items.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                items.isEmpty() -> {
                    Text(
                        "No activity yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    items.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.eventType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = item.occurredAt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(item.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
