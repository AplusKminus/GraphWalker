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