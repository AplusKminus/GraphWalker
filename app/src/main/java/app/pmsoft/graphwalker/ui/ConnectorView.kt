package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    onNavigateToNode: (Long, Long) -> Unit,
    onNavigateBack: () -> Unit,
    triggerEditDelete: Boolean,
    onEditDeleteHandled: () -> Unit,
    applyContentPadding: Boolean = true,
    onNavigateToAddEdge: (Long) -> Unit = {}
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = if (applyContentPadding) PaddingValues(16.dp) else PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Connected Edges",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (edges.isNotEmpty()) {
                items(edges) { edge ->
                    EdgeItem(
                        edge = edge,
                        connectedConnectorName = connectedConnectors[edge.id] ?: "Unknown",
                        currentConnectorId = connectorId,
                        showWeight = graph?.hasEdgeWeights == true,
                        onNavigateToNode = {
                            val targetNodeId = targetNodeIds[edge.id]
                            val graphId = currentNode?.graphId
                            if (targetNodeId != null && targetNodeId != -1L && graphId != null) {
                                onNavigateToNode(graphId, targetNodeId)
                            }
                        }
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { onNavigateToAddEdge(connectorId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add edge",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Edge")
                }
            }

            if (edges.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No connected edges yet. Tap 'Add Edge' to create one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    onNavigateToNode: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNavigateToNode
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (edge.name.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = edge.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            val directionLabel = when {
                edge.bidirectional -> "↔️"
                edge.fromConnectorId == currentConnectorId -> "➡️"
                else -> "⬅️"
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
    }
}