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
import androidx.compose.material.icons.filled.Home
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
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModel
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEdgeScreen(
    connectorId: Long,
    onNavigateBack: () -> Unit,
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
    
    val viewModel: ConnectorViewModel = viewModel(
        factory = ConnectorViewModelFactory(repository, connectorId)
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
    
    val currentNode by viewModel.currentNode.collectAsState()
    val currentGraphId = currentNode?.graphId
    
    val allConnectors by if (currentGraphId != null) {
        repository.getConnectorsByGraphId(currentGraphId).collectAsState(initial = emptyList())
    } else {
        repository.getAllConnectors().collectAsState(initial = emptyList())
    }
    val allNodes by if (currentGraphId != null) {
        repository.getNodesByGraphId(currentGraphId).collectAsState(initial = emptyList())
    } else {
        repository.getAllNodes().collectAsState(initial = emptyList())
    }
    
    
    // Filtered results based on current state
    val filteredConnectors = remember(allConnectors, allNodes, searchText, selectedNode, selectedConnector) {
        if (selectedConnector != null) {
            emptyList()
        } else {
            if (selectedNode != null) {
                // Node selected, show all connectors for that node, filtered by search text if provided
                val nodeConnectors = allConnectors.filter { it.nodeId == selectedNode!!.id }
                if (searchText.isBlank()) {
                    nodeConnectors
                } else {
                    nodeConnectors.filter { it.name.contains(searchText, ignoreCase = true) }
                }
            } else {
                // No node selected, only show results if search text is provided
                if (searchText.isBlank()) {
                    emptyList()
                } else {
                    // Show matching connectors
                    allConnectors.filter { connector ->
                        connector.name.contains(searchText, ignoreCase = true) ||
                        allNodes.find { it.id == connector.nodeId }?.name?.contains(searchText, ignoreCase = true) == true
                    }
                }
            }
        }
    }
    
    val filteredNodes = remember(allNodes, searchText, selectedNode, selectedConnector) {
        if (searchText.isBlank() || selectedNode != null || selectedConnector != null) {
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Always show Graph Overview button (only action, so never overflows)
                    IconButton(onClick = onNavigateToGraphOverview) {
                        Icon(Icons.Default.Home, contentDescription = "Graph Overview")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bidirectional = !bidirectional },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Bidirectional")
                        Switch(
                            checked = bidirectional,
                            onCheckedChange = { bidirectional = it },
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Target",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Node selection card (always visible)
            item {
                if (selectedNode != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                    text = selectedNode!!.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Selected Node",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { 
                                selectedNode = null
                                selectedConnector = null // Remove connector when node is removed
                                searchText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove node selection")
                            }
                        }
                    }
                } else {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNodeCreation = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create new",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Create New Node & Connector",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            
            // Connector selection card (always visible)
            item {
                if (selectedConnector != null) {
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
                                    text = "Selected Connector",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { selectedConnector = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove connector selection")
                            }
                        }
                    }
                } else if (selectedNode != null) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNodeCreation = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create connector",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Create Connector in ${selectedNode!!.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Create or select a node before choosing a connector",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
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
            
            
            // Search results - Connectors
            if (selectedConnector == null && filteredConnectors.isNotEmpty()) {
                items(filteredConnectors) { connector ->
                    val node = allNodes.find { it.id == connector.nodeId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedConnector = connector
                                selectedNode = node // Also set the selected node
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
                            viewModel.createEdge(selectedConnector!!.id, edgeNameValue, weightValue, bidirectionalValue)
                            onNavigateBack()
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
                CoroutineScope(Dispatchers.IO).launch {
                    val result = if (selectedNode != null) {
                        // Case 2: Create connector for existing node
                        val newConnId = viewModel.createConnectorForNode(selectedNode!!.id, connectorName)
                        Pair(newConnId, selectedNode!!.id)
                    } else {
                        // Case 3: Create new node and connector
                        val targetGraphId = currentNode?.graphId
                        if (targetGraphId != null) {
                            viewModel.createNodeAndConnectorWithNodeId(nodeName, connectorName, targetGraphId)
                        } else {
                            Pair(-1L, -1L)
                        }
                    }
                    
                    val (newConnectorId, nodeId) = result
                    if (newConnectorId != -1L) {
                        // Use a delay to allow reactive flows to update, then auto-select
                        delay(200)
                        launch(Dispatchers.Main) {
                            // Auto-select the newly created connector by ID
                            val newConnector = Connector(
                                id = newConnectorId,
                                nodeId = nodeId,
                                name = connectorName
                            )
                            selectedConnector = newConnector
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