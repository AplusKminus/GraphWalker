package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.entity.Edge
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModel
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorView(
    connectorId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao()
    )
    val viewModel: ConnectorViewModel = viewModel(
        factory = ConnectorViewModelFactory(repository, connectorId)
    )

    val connector by viewModel.connector.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val connectedConnectors by viewModel.connectedConnectors.collectAsState()
    val targetNodeIds by viewModel.targetNodeIds.collectAsState()
    val currentNode by viewModel.currentNode.collectAsState()
    val graph by viewModel.graph.collectAsState()
    
    var showAddEdgeView by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showEditDeleteDialog by remember { mutableStateOf(false) }
    var newConnectorName by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(connector?.name ?: "Connector") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (connector != null) {
                        Box {
                            IconButton(onClick = { showContextMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit / Delete") },
                                    onClick = {
                                        showContextMenu = false
                                        newConnectorName = connector?.name ?: ""
                                        showEditDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (connector != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
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
                        onClick = { showAddEdgeView = true },
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
    }
    
    if (showAddEdgeView) {
        AddEdgeView(
            viewModel = viewModel,
            onBack = { showAddEdgeView = false },
            onCreateEdge = { targetConnectorId, edgeName, weight, bidirectional ->
                viewModel.createEdge(targetConnectorId, edgeName, weight, bidirectional)
                showAddEdgeView = false
            }
        )
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
                edge.bidirectional -> "↔\uFE0F"
                edge.fromConnectorId == currentConnectorId -> "➡\uFE0F"
                else -> "⬅\uFE0F"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEdgeView(
    viewModel: ConnectorViewModel,
    onBack: () -> Unit,
    onCreateEdge: (Long, String, Double, Boolean) -> Unit
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao()
    )
    
    val graph by viewModel.graph.collectAsState()
    
    var edgeName by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("1.0") }
    var bidirectional by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedConnector by remember { mutableStateOf<Connector?>(null) }
    var selectedNode by remember { mutableStateOf<Node?>(null) }
    var showNodeCreation by remember { mutableStateOf(false) }
    var newNodeName by remember { mutableStateOf("") }
    var newConnectorName by remember { mutableStateOf("") }
    
    val allConnectors by repository.getAllConnectors().collectAsState(initial = emptyList())
    val allNodes by repository.getAllNodes().collectAsState(initial = emptyList())
    val currentNode by viewModel.currentNode.collectAsState()
    
    
    // Filtered results based on current state
    val filteredConnectors = remember(allConnectors, allNodes, searchText, selectedNode) {
        if (searchText.isBlank()) {
            emptyList()
        } else {
            if (selectedNode != null) {
                // Case 2: Node selected, show only connectors for that node
                allConnectors.filter { it.nodeId == selectedNode!!.id && it.name.contains(searchText, ignoreCase = true) }
            } else {
                // Show matching connectors
                allConnectors.filter { connector ->
                    connector.name.contains(searchText, ignoreCase = true) ||
                    allNodes.find { it.id == connector.nodeId }?.name?.contains(searchText, ignoreCase = true) == true
                }
            }
        }
    }
    
    val filteredNodes = remember(allNodes, searchText, selectedNode) {
        if (searchText.isBlank() || selectedNode != null) {
            emptyList()
        } else {
            allNodes.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Edge") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (graph?.hasEdgeLabels == true) {
                item {
                    OutlinedTextField(
                        value = edgeName,
                        onValueChange = { edgeName = it },
                        label = { Text("Edge Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            if (graph?.hasEdgeWeights == true) {
                item {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            
            if (graph?.isDirected == true) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = bidirectional,
                            onCheckedChange = { bidirectional = it }
                        )
                        Text("Bidirectional")
                    }
                }
            }
            
            item {
                Text(
                    text = "Target",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Case 1: Selected connector
            if (selectedConnector != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = selectedConnector!!.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Node: ${allNodes.find { it.id == selectedConnector!!.nodeId }?.name ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { selectedConnector = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove selection")
                            }
                        }
                    }
                }
            }
            
            // Case 2: Selected node
            if (selectedNode != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedNode!!.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Node selected",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { 
                                selectedNode = null
                                searchText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove selection")
                            }
                        }
                    }
                }
            }
            
            // Search field (shown when no connector is selected)
            if (selectedConnector == null) {
                item {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { 
                            Text(if (selectedNode != null) "Search connectors in ${selectedNode!!.name}" else "Search connectors or nodes") 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        singleLine = true
                    )
                }
            }
            
            // Case 2: Create connector button when node is selected
            if (selectedNode != null) {
                item {
                    OutlinedButton(
                        onClick = { showNodeCreation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create connector")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Connector in ${selectedNode!!.name}")
                    }
                }
            }
            
            // Search results - Connectors
            if (searchText.isNotBlank() && selectedConnector == null) {
                items(filteredConnectors) { connector ->
                    val node = allNodes.find { it.id == connector.nodeId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedConnector = connector
                                searchText = ""
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = connector.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Connector in: ${node?.name ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Search results - Nodes (only when no node is selected)
                if (selectedNode == null) {
                    items(filteredNodes) { node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedNode = node
                                    searchText = ""
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Node",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Case 3: Create new node & connector button
            if (selectedConnector == null && selectedNode == null) {
                item {
                    OutlinedButton(
                        onClick = { showNodeCreation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create new")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Node & Connector")
                    }
                }
            }
            
            // Create edge button
            if (selectedConnector != null) {
                item {
                    Button(
                        onClick = {
                            val weightValue = if (graph?.hasEdgeWeights == true) {
                                weight.toDoubleOrNull() ?: 1.0
                            } else {
                                1.0
                            }
                            val bidirectionalValue = if (graph?.isDirected == false) {
                                true  // Force bidirectional for undirected graphs
                            } else {
                                bidirectional  // Use checkbox value for directed graphs
                            }
                            val edgeNameValue = if (graph?.hasEdgeLabels == true) {
                                edgeName
                            } else {
                                ""  // Empty name when labels are disabled
                            }
                            onCreateEdge(selectedConnector!!.id, edgeNameValue, weightValue, bidirectionalValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedConnector != null
                    ) {
                        Text("Create Edge")
                    }
                }
            }
        }
    }
    
    if (showNodeCreation) {
        CreateNodeConnectorDialog(
            nodeName = newNodeName,
            connectorName = newConnectorName,
            onNodeNameChange = { newNodeName = it },
            onConnectorNameChange = { newConnectorName = it },
            selectedNodeName = selectedNode?.name,
            onDismiss = {
                showNodeCreation = false
                newNodeName = ""
                newConnectorName = ""
            },
            onCreateNodeConnector = { nodeName, connectorName ->
                val targetGraphId = if (selectedNode != null) {
                    selectedNode!!.graphId
                } else {
                    currentNode?.graphId
                }
                
                targetGraphId?.let { graphId ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val newConnectorId = viewModel.createNodeAndConnector(nodeName, connectorName, graphId)
                        // Use a delay to allow reactive flows to update, then auto-select
                        delay(100)
                        launch(Dispatchers.Main) {
                            // Auto-select the newly created connector for case 3
                            if (selectedNode == null) {
                                selectedConnector = allConnectors.find { it.id == newConnectorId }
                            }
                        }
                    }
                }
                showNodeCreation = false
                newNodeName = ""
                newConnectorName = ""
            }
        )
    }
}

@Composable
fun CreateNodeConnectorDialog(
    nodeName: String,
    connectorName: String,
    onNodeNameChange: (String) -> Unit,
    onConnectorNameChange: (String) -> Unit,
    selectedNodeName: String? = null,
    onDismiss: () -> Unit,
    onCreateNodeConnector: (String, String) -> Unit
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
                    text = if (selectedNodeName != null) "Create Connector in $selectedNodeName" else "Create New Node & Connector",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                if (selectedNodeName == null) {
                    OutlinedTextField(
                        value = nodeName,
                        onValueChange = onNodeNameChange,
                        label = { Text("Node Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                OutlinedTextField(
                    value = connectorName,
                    onValueChange = onConnectorNameChange,
                    label = { Text("Connector Name") },
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
                        onClick = {
                            val finalNodeName = selectedNodeName ?: nodeName
                            if (finalNodeName.isNotBlank() && connectorName.isNotBlank()) {
                                onCreateNodeConnector(finalNodeName.trim(), connectorName.trim())
                            }
                        },
                        enabled = (selectedNodeName != null || nodeName.isNotBlank()) && connectorName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}