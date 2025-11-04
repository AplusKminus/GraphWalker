package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Clique
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.data.model.NodeWithCliques
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModel
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModelFactory
import app.pmsoft.graphwalker.ui.viewmodel.NodeViewModel
import app.pmsoft.graphwalker.ui.viewmodel.NodeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeView(
    fullGraph: FullGraph,
    targetNodeId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToConnector: (Long) -> Unit = {},
    onNavigateToAddEdge: (Long) -> Unit = {},
    onNavigateToGraphOverview: () -> Unit = {},
    onNavigateToClique: (Long) -> Unit = {}
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
    // Use targetNodeId if provided, otherwise use the starting node
    val nodeIdForViewModel = targetNodeId ?: fullGraph.startingNode?.id ?: fullGraph.id
    val viewModel: NodeViewModel = viewModel(
        factory = NodeViewModelFactory(repository, fullGraph.id, nodeIdForViewModel)
    )

    var showEditNodeDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showTagContextMenu by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("") }
    var tagToEdit by remember { mutableStateOf("") }
    var nodeName by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }

    val connectors by viewModel.connectors.collectAsState()
    val edgeCounts by viewModel.edgeCounts.collectAsState()
    val viewModelFullGraph by viewModel.fullGraph.collectAsState()
    
    // Clique-related flows
    val nodeWithCliques by repository.getNodeWithCliques(nodeIdForViewModel).collectAsState(initial = null)
    val allCliques by repository.getCliquesByGraphId(fullGraph.id).collectAsState(initial = emptyList())

    // Determine which node to display: targetNodeId takes precedence over starting node
    val currentNode = remember(viewModelFullGraph, targetNodeId) {
        viewModelFullGraph?.let { graph ->
            targetNodeId?.let { nodeId ->
                graph.nodes.find { it.id == nodeId }
            } ?: graph.startingNode
        }
    }


    // Ensure default connector exists when hasConnectors is false
    LaunchedEffect(currentNode, viewModelFullGraph?.hasConnectors) {
        if (currentNode != null && viewModelFullGraph?.hasConnectors == false) {
            // Check if default connector already exists
            if (connectors.isEmpty()) {
                viewModel.addConnector(currentNode.id, "")
            }
        }
    }

    // Get the default connector for ConnectorView when hasConnectors is false
    val defaultConnector = if (viewModelFullGraph?.hasConnectors == false && connectors.isNotEmpty()) {
        connectors.first()
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(currentNode?.name ?: "No Node") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Always show Graph Overview button first (highest priority)
                    IconButton(onClick = onNavigateToGraphOverview) {
                        Icon(Icons.Default.Home, contentDescription = "Graph Overview")
                    }
                    
                    // Show context menu only if there's a current node
                    if (currentNode != null) {
                        Box {
                            IconButton(onClick = { showContextMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Node") },
                                    onClick = {
                                        showContextMenu = false
                                        nodeName = currentNode?.name ?: ""
                                        showEditNodeDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (currentNode != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagsSection(
                    tags = currentNode.tags,
                    onAddTag = { showAddTagDialog = true },
                    onTagTap = { tag ->
                        selectedTag = tag
                        tagToEdit = tag
                        showTagContextMenu = true
                    }
                )
                
                // Conditional rendering based on hasConnectors setting
                if (viewModelFullGraph?.hasConnectors == true) {
                    ConnectorsSection(
                        connectors = connectors,
                        edgeCounts = edgeCounts,
                        onNavigateToConnector = onNavigateToConnector,
                        onConnectorCreated = { connectorName ->
                            viewModel.addConnector(currentNode.id, connectorName)
                        }
                    )
                } else if (defaultConnector != null) {
                    val connectorViewModel: ConnectorViewModel = viewModel(
                        factory = ConnectorViewModelFactory(repository, defaultConnector.id)
                    )
                    ConnectorView(
                        connectorId = defaultConnector.id,
                        paddingValues = PaddingValues(0.dp),
                        viewModel = connectorViewModel,
                        onNavigateToNode = { _ -> 
                            // No navigation needed since we're already on the node view
                        },
                        onNavigateBack = {},
                        triggerEditDelete = false,
                        onEditDeleteHandled = {},
                        applyContentPadding = false,
                        onNavigateToAddEdge = onNavigateToAddEdge
                    )
                }
                
                // Cliques Section
                CliquesSection(
                    nodeWithCliques = nodeWithCliques,
                    allCliques = allCliques,
                    onToggleCliqueMembership = { clique, isMember ->
                        CoroutineScope(Dispatchers.IO).launch {
                            if (isMember) {
                                // Add node to clique
                                repository.insertCliqueNodeCrossRef(
                                    app.pmsoft.graphwalker.data.entity.CliqueNodeCrossRef(
                                        cliqueId = clique.id,
                                        nodeId = currentNode.id
                                    )
                                )
                            } else {
                                // Remove node from clique
                                repository.removeNodeFromClique(clique.id, currentNode.id)
                            }
                        }
                    },
                    onNavigateToClique = onNavigateToClique
                )
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

    if (showEditNodeDialog) {
        Dialog(onDismissRequest = { 
            showEditNodeDialog = false
            nodeName = ""
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
                        text = "Edit Node",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    OutlinedTextField(
                        value = nodeName,
                        onValueChange = { nodeName = it },
                        label = { Text("Node Name") },
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
                                showEditNodeDialog = false
                                nodeName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (nodeName.isNotBlank() && currentNode != null) {
                                    viewModel.updateNodeName(currentNode.id, nodeName.trim())
                                    showEditNodeDialog = false
                                    nodeName = ""
                                }
                            },
                            enabled = nodeName.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    if (showAddTagDialog) {
        AddTagDialog(
            newTag = newTag,
            onNewTagChange = { newTag = it },
            onAddTag = { tag ->
                if (tag.isNotBlank() && currentNode != null) {
                    viewModel.addTag(currentNode.id, tag.trim())
                    newTag = ""
                }
            },
            onDismiss = { 
                showAddTagDialog = false 
                newTag = ""
            }
        )
    }

    if (showTagContextMenu) {
        TagContextMenuDialog(
            tag = selectedTag,
            editTag = tagToEdit,
            onEditTagChange = { tagToEdit = it },
            onUpdateTag = { oldTag, newTag ->
                if (newTag.isNotBlank() && currentNode != null) {
                    viewModel.updateTag(currentNode.id, oldTag, newTag.trim())
                }
                showTagContextMenu = false
                selectedTag = ""
                tagToEdit = ""
            },
            onDeleteTag = { tag ->
                if (currentNode != null) {
                    viewModel.removeTag(currentNode.id, tag)
                }
                showTagContextMenu = false
                selectedTag = ""
                tagToEdit = ""
            },
            onDismiss = {
                showTagContextMenu = false
                selectedTag = ""
                tagToEdit = ""
            }
        )
    }

}


@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsSection(
    tags: List<String>,
    onAddTag: () -> Unit,
    onTagTap: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.headlineSmall
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = false,
                    onClick = { onTagTap(tag) },
                    label = { Text(tag) }
                )
            }
            
            // Add tag button
            FilterChip(
                selected = false,
                onClick = onAddTag,
                label = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Add tag",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Add tag")
                    }
                }
            )
        }
    }
}

@Composable
fun AddTagDialog(
    newTag: String,
    onNewTagChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit
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
                    text = "Add Tags",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = newTag,
                    onValueChange = onNewTagChange,
                    label = { Text("Tag name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { 
                            onAddTag(newTag)
                        },
                        enabled = newTag.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun TagContextMenuDialog(
    tag: String,
    editTag: String,
    onEditTagChange: (String) -> Unit,
    onUpdateTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onDismiss: () -> Unit
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
                    text = "Edit Tag",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = editTag,
                    onValueChange = onEditTagChange,
                    label = { Text("Tag name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDeleteTag(tag) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { onUpdateTag(tag, editTag) },
                            enabled = editTag.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorItem(
    connector: Connector,
    edgeCount: Int,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = connector.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Text(
                text = when (edgeCount) {
                    0 -> "No connected edges"
                    1 -> "1 connected edge"
                    else -> "$edgeCount connected edges"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CliquesSection(
    nodeWithCliques: NodeWithCliques?,
    allCliques: List<Clique>,
    onToggleCliqueMembership: (Clique, Boolean) -> Unit,
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

        if (allCliques.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cliques created in this graph yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            allCliques.forEach { clique ->
                CliqueItem(
                    clique = clique,
                    isChecked = nodeWithCliques?.cliques?.any { it.id == clique.id } ?: false,
                    onToggleCliqueMembership = onToggleCliqueMembership,
                    onNavigateToClique = onNavigateToClique
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CliqueItem(
    clique: Clique,
    isChecked: Boolean,
    onToggleCliqueMembership: (Clique, Boolean) -> Unit,
    onNavigateToClique: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigateToClique(clique.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggleCliqueMembership(clique, it) }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clique.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (clique.edgeWeight != 1.0) {
                    Text(
                        text = "Weight: ${clique.edgeWeight}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}