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
                onNavigateToNode = { fullGraph: FullGraph ->
                    navController.navigate("node_view/${fullGraph.id}")
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
                }
            )
        }
        composable("connector_view/{connectorId}") { backStackEntry ->
            val connectorId = backStackEntry.arguments?.getString("connectorId")?.toLong() ?: return@composable
            ConnectorViewScreen(
                connectorId = connectorId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { graphId, nodeId ->
                    navController.navigate("node_view/$graphId/$nodeId")
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphListScreen(
    onNavigateToNode: (FullGraph) -> Unit
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
                    onClick = { onNavigateToNode(fullGraph) }
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
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showCreateDialog = false
                                graphName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (graphName.isNotBlank()) {
                                    viewModel.createNewGraph(graphName.trim())
                                    showCreateDialog = false
                                    graphName = ""
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
    onNavigateToConnector: (Long) -> Unit = {}
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
            onNavigateToConnector = onNavigateToConnector
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

@Composable
fun ConnectorViewScreen(
    connectorId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long, Long) -> Unit = { _, _ -> }
) {
    ConnectorView(
        connectorId = connectorId,
        onNavigateBack = onNavigateBack,
        onNavigateToNode = onNavigateToNode
    )
}