package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.entity.Edge
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.data.model.CliqueWithNodes
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.CliqueViewModel
import app.pmsoft.graphwalker.ui.viewmodel.CliqueViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    graphId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long?) -> Unit,
    onNavigateToConnector: (Long, Long) -> Unit, // connectorId, nodeId
    onNavigateToClique: (Long) -> Unit,
    onNavigateToUnconnectedConnectors: (Long) -> Unit
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

    val cliqueViewModel: CliqueViewModel = viewModel(
        factory = CliqueViewModelFactory(repository, graphId)
    )

    val fullGraph by repository.getFullGraphById(graphId).collectAsState(initial = null)
    val allNodes by repository.getNodesByGraphId(graphId).collectAsState(initial = emptyList())
    val allConnectors by repository.getAllConnectors().collectAsState(initial = emptyList())
    val allEdges by repository.getEdgesByGraphId(graphId).collectAsState(initial = emptyList())
    val cliques by cliqueViewModel.cliques.collectAsState(initial = emptyList())
    
    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var showCreateCliqueDialog by remember { mutableStateOf(false) }
    var showCreateStartingNodeDialog by remember { mutableStateOf(false) }
    var cliqueName by remember { mutableStateOf("") }
    var cliqueWeight by remember { mutableStateOf("1.0") }
    var startingNodeName by remember { mutableStateOf("") }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showEditGraphDialog by remember { mutableStateOf(false) }
    var showDeleteGraphDialog by remember { mutableStateOf(false) }
    var editedGraphName by remember { mutableStateOf("") }

    // Initialize edit graph name when graph loads
    LaunchedEffect(fullGraph) {
        fullGraph?.let { 
            editedGraphName = it.name 
        }
    }

    // Filter connectors for this graph
    val graphConnectors = remember(allConnectors, allNodes) {
        allConnectors.filter { connector ->
            allNodes.any { node -> node.id == connector.nodeId }
        }
    }

    // Search results
    val searchResults = remember(searchText, allNodes, graphConnectors, allEdges, selectedFilter) {
        if (searchText.isBlank()) {
            emptyList()
        } else {
            val query = searchText.lowercase()
            val results = mutableListOf<SearchResult>()
            
            // Search nodes
            if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.NODES) {
                allNodes.filter { node ->
                    node.name.lowercase().contains(query) ||
                    node.tags.any { it.lowercase().contains(query) }
                }.forEach { node ->
                    results.add(SearchResult.NodeResult(node))
                }
            }
            
            // Search connectors
            if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.CONNECTORS) {
                graphConnectors.filter { connector ->
                    connector.name.lowercase().contains(query)
                }.forEach { connector ->
                    val node = allNodes.find { it.id == connector.nodeId }
                    results.add(SearchResult.ConnectorResult(connector, node))
                }
            }
            
            // Search edges
            if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.EDGES) {
                allEdges.filter { edge ->
                    edge.name.lowercase().contains(query)
                }.forEach { edge ->
                    val fromConnector = graphConnectors.find { it.id == edge.fromConnectorId }
                    val toConnector = graphConnectors.find { it.id == edge.toConnectorId }
                    val fromNode = fromConnector?.let { conn -> allNodes.find { it.id == conn.nodeId } }
                    val toNode = toConnector?.let { conn -> allNodes.find { it.id == conn.nodeId } }
                    results.add(SearchResult.EdgeResult(edge, fromNode, toNode, fromConnector, toConnector))
                }
            }
            
            // Search cliques
            if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.CLIQUES) {
                cliques.filter { cliqueWithNodes ->
                    cliqueWithNodes.clique.name.lowercase().contains(query)
                }.forEach { cliqueWithNodes ->
                    results.add(SearchResult.CliqueResult(cliqueWithNodes))
                }
            }
            
            results
        }
    }

    fullGraph?.let { graph ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(graph.name) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
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
                                    showEditGraphDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showDropdownMenu = false
                                    showDeleteGraphDialog = true
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Section - Always at top
                item {
                    SearchSection(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        graph = graph
                    )
                }

                // Show other content only when search is empty
                if (searchText.isBlank()) {
                    // Starting Node Section
                    item {
                        StartingNodeSection(
                            startingNode = graph.startingNode,
                            onClick = {
                                if (graph.startingNode != null) {
                                    onNavigateToNode(graph.startingNode.id)
                                } else {
                                    // Show create starting node dialog
                                    showCreateStartingNodeDialog = true
                                }
                            },
                        )
                    }


                    // Cliques Section
                    item {
                        CliquesSection(
                            cliques = cliques,
                            graph = graph,
                            onCreateClique = { showCreateCliqueDialog = true },
                            onNavigateToClique = onNavigateToClique
                        )
                    }

                    // Analysis Section
                    item {
                        AnalysisSection(
                            graph = graph,
                            onNavigateToUnconnectedConnectors = {
                                onNavigateToUnconnectedConnectors(graphId)
                            }
                        )
                    }
                }

                // Search Results
                if (searchText.isNotBlank()) {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results found for \"$searchText\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                onNavigateToNode = onNavigateToNode,
                                onNavigateToConnector = onNavigateToConnector,
                                onNavigateToClique = onNavigateToClique
                            )
                        }
                    }
                }
            }
        }
    } ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Create Clique Dialog
    if (showCreateCliqueDialog) {
        CreateCliqueDialog(
            cliqueName = cliqueName,
            cliqueWeight = cliqueWeight,
            hasEdgeWeights = fullGraph?.hasEdgeWeights ?: false,
            onCliqueNameChange = { cliqueName = it },
            onCliqueWeightChange = { cliqueWeight = it },
            onDismiss = {
                showCreateCliqueDialog = false
                cliqueName = ""
                cliqueWeight = "1.0"
            },
            onCreateClique = { name, weight ->
                cliqueViewModel.createClique(name, weight)
                showCreateCliqueDialog = false
                cliqueName = ""
                cliqueWeight = "1.0"
            }
        )
    }


    // Create Starting Node Dialog
    if (showCreateStartingNodeDialog) {
        CreateStartingNodeDialog(
            nodeName = startingNodeName,
            onNodeNameChange = { startingNodeName = it },
            onDismiss = {
                showCreateStartingNodeDialog = false
                startingNodeName = ""
            },
            onCreateNode = { name ->
                // Create the starting node and set it as the graph's starting node
                CoroutineScope(Dispatchers.IO).launch {
                    val nodeId = repository.insertNode(
                        app.pmsoft.graphwalker.data.entity.Node(
                            graphId = graphId,
                            name = name
                        )
                    )
                    // Update the graph to set this as the starting node
                    fullGraph?.let { fullGraphData ->
                        val updatedGraph = app.pmsoft.graphwalker.data.entity.Graph(
                            id = fullGraphData.id,
                            name = fullGraphData.name,
                            startingNodeId = nodeId,
                            isDirected = fullGraphData.isDirected,
                            hasEdgeWeights = fullGraphData.hasEdgeWeights,
                            hasEdgeLabels = fullGraphData.hasEdgeLabels,
                            hasConnectors = fullGraphData.hasConnectors
                        )
                        repository.updateGraph(updatedGraph)
                    }
                }
                showCreateStartingNodeDialog = false
                startingNodeName = ""
            }
        )
    }

    // Edit Graph Dialog
    if (showEditGraphDialog) {
        EditGraphDialog(
            graphName = editedGraphName,
            onGraphNameChange = { editedGraphName = it },
            onDismiss = {
                showEditGraphDialog = false
                editedGraphName = fullGraph?.name ?: ""
            },
            onSave = { newName ->
                fullGraph?.let { graph ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val updatedGraph = app.pmsoft.graphwalker.data.entity.Graph(
                            id = graph.id,
                            name = newName.trim(),
                            startingNodeId = graph.startingNode?.id,
                            isDirected = graph.isDirected,
                            hasEdgeWeights = graph.hasEdgeWeights,
                            hasEdgeLabels = graph.hasEdgeLabels,
                            hasConnectors = graph.hasConnectors
                        )
                        repository.updateGraph(updatedGraph)
                    }
                }
                showEditGraphDialog = false
            }
        )
    }

    // Delete Graph Dialog
    if (showDeleteGraphDialog) {
        DeleteGraphDialog(
            graphName = fullGraph?.name ?: "",
            onDismiss = { showDeleteGraphDialog = false },
            onConfirm = {
                fullGraph?.let { graph ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val graphEntity = app.pmsoft.graphwalker.data.entity.Graph(
                            id = graph.id,
                            name = graph.name,
                            startingNodeId = graph.startingNode?.id,
                            isDirected = graph.isDirected,
                            hasEdgeWeights = graph.hasEdgeWeights,
                            hasEdgeLabels = graph.hasEdgeLabels,
                            hasConnectors = graph.hasConnectors
                        )
                        repository.deleteGraph(graphEntity)
                    }
                }
                showDeleteGraphDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun StartingNodeSection(
    startingNode: Node?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Starting Node",
            style = MaterialTheme.typography.headlineSmall
        )
        
        if (startingNode != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = startingNode.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (startingNode.tags.isNotEmpty()) {
                        Text(
                            text = "Tags: ${startingNode.tags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create starting node")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Starting Node")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    graph: FullGraph
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SearchFilter.entries.forEach { filter ->
                val shouldShow = when (filter) {
                    SearchFilter.ALL -> true
                    SearchFilter.NODES -> true // Nodes are always present
                    SearchFilter.CONNECTORS -> graph.hasConnectors
                    SearchFilter.EDGES -> graph.hasEdgeLabels // Only show if edges have labels to search
                    SearchFilter.CLIQUES -> true // Cliques can be created regardless of other features
                }
                
                if (shouldShow) {
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onNavigateToNode: (Long?) -> Unit,
    onNavigateToConnector: (Long, Long) -> Unit,
    onNavigateToClique: (Long) -> Unit
) {
    when (result) {
        is SearchResult.NodeResult -> {
            val contextInfo = buildList {
                if (result.node.tags.isNotEmpty()) {
                    add("Tags: ${result.node.tags.joinToString(", ")}")
                }
            }
            SearchResultCard(
                title = result.node.name,
                type = "Node",
                contextInfo = contextInfo,
                onClick = { onNavigateToNode(result.node.id) }
            )
        }
        
        is SearchResult.ConnectorResult -> {
            SearchResultCard(
                title = result.connector.name.ifBlank { "(Unnamed connector)" },
                type = "Connector",
                contextInfo = listOf("In node: ${result.node?.name ?: "Unknown"}"),
                onClick = { onNavigateToConnector(result.connector.id, result.connector.nodeId) }
            )
        }
        
        is SearchResult.EdgeResult -> {
            val directionSymbol = if (result.edge.bidirectional) "↔" else "→"
            val fromNodeName = result.fromNode?.name ?: "Unknown"
            val toNodeName = result.toNode?.name ?: "Unknown"
            val fromConnectorName = result.fromConnector?.name?.let { if (it.isBlank()) "(unnamed)" else it } ?: "Unknown"
            val toConnectorName = result.toConnector?.name?.let { if (it.isBlank()) "(unnamed)" else it } ?: "Unknown"
            
            val contextInfo = buildList {
                add("$fromNodeName:$fromConnectorName $directionSymbol $toNodeName:$toConnectorName")
                if (result.edge.weight != 1.0) {
                    add("Weight: ${result.edge.weight}")
                }
            }
            
            SearchResultCard(
                title = result.edge.name.ifBlank { "(Unnamed edge)" },
                type = "Edge",
                contextInfo = contextInfo,
                onClick = { /* Edges are not clickable in the original implementation */ }
            )
        }
        
        is SearchResult.CliqueResult -> {
            SearchResultCard(
                title = result.cliqueWithNodes.clique.name,
                type = "Clique",
                contextInfo = listOf("Nodes: ${result.cliqueWithNodes.nodes.size}"),
                onClick = { onNavigateToClique(result.cliqueWithNodes.clique.id) }
            )
        }
    }
}

enum class SearchFilter(val displayName: String) {
    ALL("All"),
    NODES("Nodes"),
    CONNECTORS("Connectors"),
    EDGES("Edges"),
    CLIQUES("Cliques")
}

sealed class SearchResult {
    data class NodeResult(val node: Node) : SearchResult()
    data class ConnectorResult(val connector: Connector, val node: Node?) : SearchResult()
    data class EdgeResult(
        val edge: Edge,
        val fromNode: Node?,
        val toNode: Node?,
        val fromConnector: Connector?,
        val toConnector: Connector?
    ) : SearchResult()
    data class CliqueResult(val cliqueWithNodes: CliqueWithNodes) : SearchResult()
}

@Composable
private fun CliquesSection(
    cliques: List<CliqueWithNodes>,
    graph: FullGraph,
    onCreateClique: () -> Unit,
    onNavigateToClique: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Cliques",
            style = MaterialTheme.typography.headlineSmall
        )
        
        if (cliques.isEmpty()) {
            Text(
                text = "No cliques created yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            cliques.forEach { cliqueWithNodes ->
                CliqueItem(
                    cliqueWithNodes = cliqueWithNodes,
                    hasEdgeWeights = graph.hasEdgeWeights,
                    onNavigateToClique = { onNavigateToClique(cliqueWithNodes.clique.id) }
                )
            }
        }
        
        OutlinedButton(
            onClick = onCreateClique,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create clique",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Clique")
        }
    }
}

@Composable
private fun AnalysisSection(
    graph: FullGraph,
    onNavigateToUnconnectedConnectors: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Analysis",
            style = MaterialTheme.typography.headlineSmall
        )
        
        if (graph.hasConnectors) {
            OutlinedButton(
                onClick = onNavigateToUnconnectedConnectors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find unconnected connectors")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CliqueItem(
    cliqueWithNodes: CliqueWithNodes,
    hasEdgeWeights: Boolean,
    onNavigateToClique: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNavigateToClique
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = cliqueWithNodes.clique.name,
                style = MaterialTheme.typography.titleSmall
            )
            if (hasEdgeWeights && cliqueWithNodes.clique.edgeWeight != 1.0) {
                Text(
                    text = "Weight: ${cliqueWithNodes.clique.edgeWeight}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Nodes: ${cliqueWithNodes.nodes.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateCliqueDialog(
    cliqueName: String,
    cliqueWeight: String,
    hasEdgeWeights: Boolean,
    onCliqueNameChange: (String) -> Unit,
    onCliqueWeightChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateClique: (String, Double) -> Unit
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
                    text = "Create New Clique",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = cliqueName,
                    onValueChange = onCliqueNameChange,
                    label = { Text("Clique Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (hasEdgeWeights) {
                    OutlinedTextField(
                        value = cliqueWeight,
                        onValueChange = onCliqueWeightChange,
                        label = { Text("Edge Weight") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                
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
                            val weight = if (hasEdgeWeights) {
                                cliqueWeight.toDoubleOrNull() ?: 1.0
                            } else {
                                1.0
                            }
                            onCreateClique(cliqueName.trim(), weight)
                        },
                        enabled = cliqueName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}


@Composable
private fun CreateStartingNodeDialog(
    nodeName: String,
    onNodeNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateNode: (String) -> Unit
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
                    text = "Create Starting Node",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Text(
                    text = "This graph needs a starting node to begin exploring.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = nodeName,
                    onValueChange = onNodeNameChange,
                    label = { Text("Node Name") },
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
                            if (nodeName.isNotBlank()) {
                                onCreateNode(nodeName.trim())
                            }
                        },
                        enabled = nodeName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditGraphDialog(
    graphName: String,
    onGraphNameChange: (String) -> Unit,
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
                    text = "Edit Graph",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = graphName,
                    onValueChange = onGraphNameChange,
                    label = { Text("Graph Name") },
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
                        onClick = { onSave(graphName) },
                        enabled = graphName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteGraphDialog(
    graphName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Graph") },
        text = { Text("Are you sure you want to delete the graph \"$graphName\"? This action cannot be undone and will delete all its nodes, edges, and cliques.") },
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