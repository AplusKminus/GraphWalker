package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pmsoft.graphwalker.data.entity.Edge
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModel

@Composable
fun ConnectorView(
    connectorId: Long,
    paddingValues: PaddingValues,
    viewModel: ConnectorViewModel,
    onNavigateToNode: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    triggerEditDelete: Boolean,
    onEditDeleteHandled: () -> Unit,
    applyContentPadding: Boolean = true,
    onNavigateToAddEdge: (Long) -> Unit = {},
) {
    val graph by viewModel.graph.collectAsState()
    val connector by viewModel.connector.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val currentNode by viewModel.currentNode.collectAsState()
    val connectedConnectors by viewModel.connectedConnectors.collectAsState()
    val targetNodeIds by viewModel.targetNodeIds.collectAsState()
    var showEditDeleteDialog by remember { mutableStateOf(false) }
    var newConnectorName by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Handle edit/delete trigger from parent
    LaunchedEffect(triggerEditDelete) {
        if (triggerEditDelete) {
            newConnectorName = connector?.name ?: ""
            showEditDeleteDialog = true
            onEditDeleteHandled()
        }
    }

    if (connector != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(if (applyContentPadding) Modifier.padding(16.dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Connected Edges",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            edges.forEach { edge ->
                EdgeItem(
                    edge = edge,
                    connectedConnectorName = connectedConnectors[edge.id] ?: "Unknown",
                    currentConnectorId = connectorId,
                    showWeight = graph?.hasEdgeWeights == true,
                    hasEdgeLabels = graph?.hasEdgeLabels == true,
                    onNavigateToNode = {
                        val targetNodeId = targetNodeIds[edge.id]
                        val graphId = currentNode?.graphId
                        if (targetNodeId != null && targetNodeId != -1L && graphId != null) {
                            onNavigateToNode(targetNodeId)
                        }
                    },
                    onRenameEdge = { edgeId, newName ->
                        viewModel.updateEdgeName(edgeId, newName)
                    },
                    onDeleteEdge = { edgeId ->
                        viewModel.deleteEdge(edgeId)
                    }
                )
            }

            OutlinedButton(
                onClick = { onNavigateToAddEdge(connectorId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add edge",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Edge")
            }

            if (edges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No connected edges yet. Tap 'Add Edge' to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Combined edit/delete dialog
    if (showEditDeleteDialog) {
        Dialog(onDismissRequest = {
            showEditDeleteDialog = false
            newConnectorName = ""
            showDeleteConfirmation = false
        }) {
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
                    if (!showDeleteConfirmation) {
                        // Edit mode
                        Text(
                            text = "Edit Connector",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        OutlinedTextField(
                            value = newConnectorName,
                            onValueChange = { newConnectorName = it },
                            label = { Text("Connector Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showDeleteConfirmation = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }

                            Row {
                                TextButton(
                                    onClick = {
                                        showEditDeleteDialog = false
                                        newConnectorName = ""
                                    }
                                ) {
                                    Text("Cancel")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        val currentConnector = connector
                                        if (newConnectorName.isNotBlank() && currentConnector != null) {
                                            viewModel.updateConnectorName(currentConnector.id, newConnectorName.trim())
                                            showEditDeleteDialog = false
                                            newConnectorName = ""
                                        }
                                    },
                                    enabled = newConnectorName.isNotBlank()
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    } else {
                        // Delete confirmation mode
                        Text(
                            text = "Delete Connector",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Are you sure you want to delete '${connector?.name}'? This will also delete all associated edges.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showDeleteConfirmation = false }
                            ) {
                                Text("Cancel")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val currentConnector = connector
                                    if (currentConnector != null) {
                                        viewModel.deleteConnector(currentConnector.id)
                                        showEditDeleteDialog = false
                                        showDeleteConfirmation = false
                                        onNavigateBack() // Navigate back after deletion
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeItem(
    edge: Edge,
    connectedConnectorName: String,
    currentConnectorId: Long,
    showWeight: Boolean = false,
    hasEdgeLabels: Boolean = false,
    onNavigateToNode: () -> Unit = {},
    onRenameEdge: (Long, String) -> Unit = { _, _ -> },
    onDeleteEdge: (Long) -> Unit = {},
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var newEdgeName by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable { onNavigateToNode() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (edge.name.isNotBlank()) {
                    Text(
                        text = edge.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                val directionLabel = when {
                    edge.bidirectional -> "↔"
                    edge.fromConnectorId == currentConnectorId -> "→"
                    else -> "←"
                }
                Text(
                    text = "$directionLabel $connectedConnectorName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showWeight) {
                    Text(
                        text = "Weight: ${edge.weight}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showContextMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    if (hasEdgeLabels) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                showContextMenu = false
                                newEdgeName = edge.name
                                showRenameDialog = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showContextMenu = false
                            showDeleteConfirmation = true
                        }
                    )
                }
            }
        }
    }
    
    // Rename dialog
    if (showRenameDialog) {
        Dialog(onDismissRequest = {
            showRenameDialog = false
            newEdgeName = ""
        }) {
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
                        text = "Rename Edge",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    OutlinedTextField(
                        value = newEdgeName,
                        onValueChange = { newEdgeName = it },
                        label = { Text("Edge Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showRenameDialog = false
                                newEdgeName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (newEdgeName.isNotBlank()) {
                                    onRenameEdge(edge.id, newEdgeName.trim())
                                    showRenameDialog = false
                                    newEdgeName = ""
                                }
                            },
                            enabled = newEdgeName.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        Dialog(onDismissRequest = {
            showDeleteConfirmation = false
        }) {
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
                        text = "Delete Edge",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Text(
                        text = if (edge.name.isNotBlank()) {
                            "Are you sure you want to delete the edge '${edge.name}'?"
                        } else {
                            "Are you sure you want to delete this edge?"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirmation = false }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                onDeleteEdge(edge.id)
                                showDeleteConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
