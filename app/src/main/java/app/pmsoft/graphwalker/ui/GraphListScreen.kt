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
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.GraphListViewModel
import app.pmsoft.graphwalker.ui.viewmodel.GraphListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GraphListScreen(
    onNavigateToGraph: (FullGraph) -> Unit,
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.Companion.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao(),
        database.cliqueDao(),
    )
    val viewModel: GraphListViewModel = viewModel(
        factory = GraphListViewModelFactory(repository),
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
                title = { Text("GraphWalker") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Graph")
            }
        }
    ) { paddingValues ->
        val fullGraphs by viewModel.fullGraphs.collectAsState()

        LazyColumn(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(fullGraphs) { fullGraph ->
                Card(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    onClick = { onNavigateToGraph(fullGraph) },
                ) {
                    Column(
                        modifier = Modifier.Companion.padding(16.dp),
                    ) {
                        Text(
                            text = fullGraph.name,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Nodes: ${fullGraph.nodes.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (fullGraph.isDirected) "Directed" else "Undirected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (fullGraph.hasEdgeWeights) "Weighted" else "Unweighted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (fullGraph.hasEdgeLabels) "Labeled Edges" else "Unlabeled Edges",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (fullGraph.hasConnectors) "Connectors" else "No Connectors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (fullGraphs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Companion.Center,
                    ) {
                        Text(
                            text = "No graphs yet. Tap + to create one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.Companion.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Create New Graph",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    OutlinedTextField(
                        value = graphName,
                        onValueChange = { graphName = it },
                        label = { Text("Graph Name") },
                        modifier = Modifier.Companion.fillMaxWidth(),
                        singleLine = true,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Graph Configuration",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(
                            modifier = Modifier.Companion.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.CenterVertically,
                        ) {
                            Text("Directed Graph")
                            Switch(
                                checked = isDirected,
                                onCheckedChange = { isDirected = it },
                            )
                        }

                        Row(
                            modifier = Modifier.Companion.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.CenterVertically,
                        ) {
                            Text("Edge Weights")
                            Switch(
                                checked = hasEdgeWeights,
                                onCheckedChange = { hasEdgeWeights = it },
                            )
                        }

                        Row(
                            modifier = Modifier.Companion.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.CenterVertically,
                        ) {
                            Text("Edge Labels")
                            Switch(
                                checked = hasEdgeLabels,
                                onCheckedChange = { hasEdgeLabels = it },
                            )
                        }

                        Row(
                            modifier = Modifier.Companion.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.CenterVertically,
                        ) {
                            Text("Connectors")
                            Switch(
                                checked = hasConnectors,
                                onCheckedChange = { hasConnectors = it },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Companion.CenterVertically,
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

                        Spacer(modifier = Modifier.Companion.width(8.dp))

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
                            enabled = graphName.isNotBlank(),
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}