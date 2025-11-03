package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.entity.Edge
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.repository.GraphRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    graphId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long, Long) -> Unit,
    onNavigateToConnector: (Long) -> Unit
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao()
    )

    val fullGraph by repository.getFullGraphById(graphId).collectAsState(initial = null)
    val allNodes by repository.getNodesByGraphId(graphId).collectAsState(initial = emptyList())
    val allConnectors by repository.getAllConnectors().collectAsState(initial = emptyList())
    val allEdges by repository.getEdgesByGraphId(graphId).collectAsState(initial = emptyList())
    
    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

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
                // Starting Node Section
                item {
                    StartingNodeSection(
                        startingNode = graph.startingNode,
                        onNavigateToNode = { nodeId ->
                            onNavigateToNode(graphId, nodeId)
                        }
                    )
                }

                // Graph Configuration Section
                item {
                    GraphConfigSection(graph = graph)
                }

                // Search Section
                item {
                    SearchSection(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it }
                    )
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
                                onNavigateToConnector = onNavigateToConnector
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
}

@Composable
private fun StartingNodeSection(
    startingNode: Node?,
    onNavigateToNode: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Starting Node",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (startingNode != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNode(startingNode.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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
                Text(
                    text = "No starting node set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GraphConfigSection(graph: FullGraph) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Graph Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConfigChip(text = if (graph.isDirected) "Directed" else "Undirected")
                if (graph.hasEdgeWeights) ConfigChip(text = "Weighted")
                if (graph.hasEdgeLabels) ConfigChip(text = "Labeled")
                if (graph.hasConnectors) ConfigChip(text = "Connectors")
            }
            
            Text(
                text = "Nodes: ${graph.nodes.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfigChip(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedFilter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = { Text("Search nodes, connectors, and edges") },
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.displayName) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onNavigateToNode: (Long, Long) -> Unit,
    onNavigateToConnector: (Long) -> Unit
) {
    when (result) {
        is SearchResult.NodeResult -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNode(result.node.graphId, result.node.id) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.node.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "Node",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (result.node.tags.isNotEmpty()) {
                        Text(
                            text = "Tags: ${result.node.tags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        is SearchResult.ConnectorResult -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToConnector(result.connector.id) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.connector.name.ifBlank { "(Unnamed connector)" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "Connector",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        text = "In node: ${result.node?.name ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        is SearchResult.EdgeResult -> {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.edge.name.ifBlank { "(Unnamed edge)" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = "Edge",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    val directionSymbol = if (result.edge.bidirectional) "↔" else "→"
                    val fromNodeName = result.fromNode?.name ?: "Unknown"
                    val toNodeName = result.toNode?.name ?: "Unknown"
                    val fromConnectorName = result.fromConnector?.name?.let { if (it.isBlank()) "(unnamed)" else it } ?: "Unknown"
                    val toConnectorName = result.toConnector?.name?.let { if (it.isBlank()) "(unnamed)" else it } ?: "Unknown"
                    
                    Text(
                        text = "$fromNodeName:$fromConnectorName $directionSymbol $toNodeName:$toConnectorName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (result.edge.weight != 1.0) {
                        Text(
                            text = "Weight: ${result.edge.weight}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

enum class SearchFilter(val displayName: String) {
    ALL("All"),
    NODES("Nodes"),
    CONNECTORS("Connectors"),
    EDGES("Edges")
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
}