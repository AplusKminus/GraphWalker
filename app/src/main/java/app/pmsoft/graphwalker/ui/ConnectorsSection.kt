package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.pmsoft.graphwalker.data.entity.Connector

@Composable
fun ConnectorsSection(
    connectors: List<Connector>,
    edgeCounts: Map<Long, Int>,
    onNavigateToConnector: (Long) -> Unit,
    onConnectorCreated: (String) -> Unit
) {
    var showAddConnectorDialog by remember { mutableStateOf(false) }
    var newConnectorName by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Connectors",
            style = MaterialTheme.typography.headlineSmall
        )

        if (connectors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No connectors yet. Tap 'Add Connector' to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            connectors.forEach { connector ->
                ConnectorItem(
                    connector = connector,
                    edgeCount = edgeCounts[connector.id] ?: 0,
                    onClick = { onNavigateToConnector(connector.id) }
                )
            }
        }

        OutlinedButton(
            onClick = { showAddConnectorDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add connector",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Connector")
        }
    }
    
    AddConnectorDialog(
        showDialog = showAddConnectorDialog,
        newConnectorName = newConnectorName,
        onNewConnectorNameChange = { newConnectorName = it },
        onDismiss = {
            showAddConnectorDialog = false
            newConnectorName = ""
        },
        onAddConnector = { connectorName ->
            if (connectorName.isNotBlank()) {
                onConnectorCreated(connectorName.trim())
                showAddConnectorDialog = false
                newConnectorName = ""
            }
        }
    )
}

@Composable
fun AddConnectorDialog(
    showDialog: Boolean,
    newConnectorName: String,
    onNewConnectorNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddConnector: (String) -> Unit
) {
    if (showDialog) {
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
                        text = "Add Connector",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    OutlinedTextField(
                        value = newConnectorName,
                        onValueChange = onNewConnectorNameChange,
                        label = { Text("Connector Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { onAddConnector(newConnectorName) },
                            enabled = newConnectorName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}