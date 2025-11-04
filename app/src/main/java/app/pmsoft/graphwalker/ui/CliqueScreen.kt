package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.data.model.CliqueWithNodes
import app.pmsoft.graphwalker.repository.GraphRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CliqueScreen(
    cliqueId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long) -> Unit,
    onNavigateToGraphOverview: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao(),
        database.cliqueDao()
    )
    val cliqueWithNodes by repository.getCliqueWithNodesById(cliqueId).collectAsState(initial = null)
    val graph by repository.getGraphById(cliqueWithNodes?.clique?.graphId ?: 0L).collectAsState(initial = null)
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    LaunchedEffect(cliqueWithNodes) {
        cliqueWithNodes?.let { 
            editedName = it.clique.name 
        }
    }

    cliqueWithNodes?.let { clique ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(clique.clique.name) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToGraphOverview) {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        }
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showDropdownMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showDropdownMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Clique Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (graph?.hasEdgeWeights == true) {
                            Text(
                                text = "Edge Weight: ${clique.clique.edgeWeight}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "Nodes: ${clique.nodes.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nodes in this Clique",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (clique.nodes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No nodes in this clique",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clique.nodes) { node ->
                            NodeCard(
                                node = node,
                                onClick = { onNavigateToNode(node.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            EditCliqueDialog(
                cliqueName = editedName,
                onNameChange = { editedName = it },
                onDismiss = { 
                    showEditDialog = false
                    editedName = clique.clique.name
                },
                onSave = { newName ->
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.updateClique(
                            clique.clique.copy(name = newName.trim())
                        )
                    }
                    showEditDialog = false
                }
            )
        }

        if (showDeleteDialog) {
            DeleteCliqueDialog(
                cliqueName = clique.clique.name,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.deleteClique(clique.clique)
                    }
                    showDeleteDialog = false
                    onNavigateBack()
                }
            )
        }
    } ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeCard(
    node: Node,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (node.tags.isNotEmpty()) {
                Text(
                    text = "Tags: ${node.tags.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditCliqueDialog(
    cliqueName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Clique",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = cliqueName,
                    onValueChange = onNameChange,
                    label = { Text("Clique Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(cliqueName) },
                        enabled = cliqueName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteCliqueDialog(
    cliqueName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Clique") },
        text = { Text("Are you sure you want to delete the clique \"$cliqueName\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}