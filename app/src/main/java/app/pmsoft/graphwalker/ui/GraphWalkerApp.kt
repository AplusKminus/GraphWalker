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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.GraphListViewModel
import app.pmsoft.graphwalker.ui.viewmodel.GraphListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphWalkerApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "graph_list"
    ) {
        composable("graph_list") {
            GraphListScreen(
                onNavigateToGraph = { fullGraph: FullGraph ->
                    navController.navigate("graph_view/${fullGraph.id}")
                }
            )
        }
        composable("graph_view/{graphId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            GraphScreen(
                graphId = graphId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { graphId, nodeId ->
                    navController.navigate("node_view/$graphId/$nodeId")
                },
                onNavigateToConnector = { connectorId ->
                    navController.navigate("connector_view/$connectorId")
                }
            )
        }
        composable("node_view/{graphId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            NodeViewScreen(
                graphId = graphId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConnector = { connectorId ->
                    navController.navigate("connector_view/$connectorId")
                },
                onNavigateToAddEdge = { connectorId ->
                    navController.navigate("add_edge/$connectorId")
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graph_view/$graphId", inclusive = false)
                }
            )
        }
        composable("node_view/{graphId}/{nodeId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            val nodeId = backStackEntry.arguments?.getString("nodeId")?.toLong() ?: return@composable
            NodeViewScreen(
                graphId = graphId,
                nodeId = nodeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConnector = { connectorId ->
                    navController.navigate("connector_view/$connectorId")
                },
                onNavigateToAddEdge = { connectorId ->
                    navController.navigate("add_edge/$connectorId")
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graph_view/$graphId", inclusive = false)
                }
            )
        }
        composable("connector_view/{connectorId}") { backStackEntry ->
            val connectorId = backStackEntry.arguments?.getString("connectorId")?.toLong() ?: return@composable
            ConnectorScreen(
                connectorId = connectorId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { graphId, nodeId ->
                    navController.navigate("node_view/$graphId/$nodeId")
                },
                onNavigateToAddEdge = { connectorId ->
                    navController.navigate("add_edge/$connectorId")
                },
                onNavigateToGraphOverview = {
                    // Navigate back to graph list (we don't know which graph this connector belongs to)
                    navController.popBackStack("graph_list", inclusive = false)
                }
            )
        }
        composable("add_edge/{connectorId}") { backStackEntry ->
            val connectorId = backStackEntry.arguments?.getString("connectorId")?.toLong() ?: return@composable
            AddEdgeScreen(
                connectorId = connectorId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGraphOverview = {
                    // Navigate back to graph list (we don't know which graph this connector belongs to)
                    navController.popBackStack("graph_list", inclusive = false)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphListScreen(
    onNavigateToGraph: (FullGraph) -> Unit
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao()
    )
    val viewModel: GraphListViewModel = viewModel(
        factory = GraphListViewModelFactory(repository)
    )

    var showCreateDialog by remember { mutableStateOf(false) }
    var graphName by remember { mutableStateOf("") }
    var isDirected by remember { mutableStateOf(true) }
    var hasEdgeWeights by remember { mutableStateOf(false) }
    var hasEdgeLabels by remember { mutableStateOf(false) }
    var hasConnectors by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GraphWalker") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Graph")
            }
        }
    ) { paddingValues ->
        val fullGraphs by viewModel.fullGraphs.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fullGraphs) { fullGraph ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToGraph(fullGraph) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = fullGraph.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Nodes: ${fullGraph.nodes.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (fullGraph.startingNode != null) {
                            Text(
                                text = "Starting node: ${fullGraph.startingNode.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!fullGraph.isDirected) {
                                Text(
                                    text = "Undirected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (fullGraph.hasEdgeWeights) {
                                Text(
                                    text = "Weighted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (fullGraph.hasEdgeLabels) {
                                Text(
                                    text = "Labeled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (fullGraph.hasConnectors) {
                                Text(
                                    text = "Connectors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (fullGraphs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No graphs yet. Tap + to create one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        Dialog(onDismissRequest = { 
            showCreateDialog = false
            graphName = ""
            isDirected = true
            hasEdgeWeights = false
            hasEdgeLabels = false
            hasConnectors = false
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
                        text = "Create New Graph",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    OutlinedTextField(
                        value = graphName,
                        onValueChange = { graphName = it },
                        label = { Text("Graph Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Graph Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Directed Graph")
                            Switch(
                                checked = isDirected,
                                onCheckedChange = { isDirected = it }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Edge Weights")
                            Switch(
                                checked = hasEdgeWeights,
                                onCheckedChange = { hasEdgeWeights = it }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Edge Labels")
                            Switch(
                                checked = hasEdgeLabels,
                                onCheckedChange = { hasEdgeLabels = it }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connectors")
                            Switch(
                                checked = hasConnectors,
                                onCheckedChange = { hasConnectors = it }
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showCreateDialog = false
                                graphName = ""
                                isDirected = true
                                hasEdgeWeights = false
                                hasEdgeLabels = false
                                hasConnectors = false
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (graphName.isNotBlank()) {
                                    viewModel.createNewGraph(
                                        name = graphName.trim(),
                                        isDirected = isDirected,
                                        hasEdgeWeights = hasEdgeWeights,
                                        hasEdgeLabels = hasEdgeLabels,
                                        hasConnectors = hasConnectors
                                    )
                                    showCreateDialog = false
                                    graphName = ""
                                    isDirected = true
                                    hasEdgeWeights = false
                                    hasEdgeLabels = false
                                    hasConnectors = false
                                }
                            },
                            enabled = graphName.isNotBlank()
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NodeViewScreen(
    graphId: Long,
    nodeId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToConnector: (Long) -> Unit = {},
    onNavigateToAddEdge: (Long) -> Unit = {},
    onNavigateToGraphOverview: () -> Unit = {}
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

    fullGraph?.let { graph ->
        NodeView(
            fullGraph = graph,
            targetNodeId = nodeId,
            onNavigateBack = onNavigateBack,
            onNavigateToConnector = onNavigateToConnector,
            onNavigateToAddEdge = onNavigateToAddEdge,
            onNavigateToGraphOverview = onNavigateToGraphOverview
        )
    } ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
