package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.repository.GraphRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnconnectedConnectorsScreen(
    graphId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToConnector: (Long) -> Unit
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

    val allNodes by repository.getNodesByGraphId(graphId).collectAsState(initial = emptyList())
    val allConnectors by repository.getAllConnectors().collectAsState(initial = emptyList())
    val allEdges by repository.getEdgesByGraphId(graphId).collectAsState(initial = emptyList())

    // Filter connectors for this graph
    val graphConnectors = remember(allConnectors, allNodes) {
        allConnectors.filter { connector ->
            allNodes.any { node -> node.id == connector.nodeId }
        }
    }

    // Find unconnected connectors
    val unconnectedConnectors = remember(graphConnectors, allEdges) {
        val connectedConnectorIds = allEdges.flatMap { edge ->
            listOf(edge.fromConnectorId, edge.toConnectorId)
        }.toSet()
        
        graphConnectors.filter { connector ->
            connector.id !in connectedConnectorIds
        }
    }

    // Create map of connector to node for display
    val connectorToNode = remember(unconnectedConnectors, allNodes) {
        unconnectedConnectors.associateWith { connector ->
            allNodes.find { node -> node.id == connector.nodeId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unconnected Connectors") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (unconnectedConnectors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "All connectors are connected!",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Every connector in this graph has at least one edge.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Found ${unconnectedConnectors.size} unconnected connector${if (unconnectedConnectors.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(unconnectedConnectors) { connector ->
                    UnconnectedConnectorItem(
                        connector = connector,
                        node = connectorToNode[connector],
                        onClick = { onNavigateToConnector(connector.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnconnectedConnectorItem(
    connector: Connector,
    node: Node?,
    onClick: () -> Unit
) {
    SearchResultCard(
        title = connector.name.ifBlank { "(Unnamed connector)" },
        type = "Connector",
        contextInfo = listOf("In node: ${node?.name ?: "Unknown"}"),
        onClick = onClick
    )
}