package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.NodeViewModel
import app.pmsoft.graphwalker.ui.viewmodel.NodeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeView(
    fullGraph: FullGraph,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao()
    )
    val viewModel: NodeViewModel = viewModel(
        factory = NodeViewModelFactory(repository, fullGraph.id)
    )

    var showCreateNodeDialog by remember { mutableStateOf(false) }
    var showEditNodeDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showTagContextMenu by remember { mutableStateOf(false) }
    var showAddConnectorDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("") }
    var tagToEdit by remember { mutableStateOf("") }
    var nodeName by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    var newConnectorName by remember { mutableStateOf("") }

    val connectors by viewModel.connectors.collectAsState()
    val edgeCounts by viewModel.edgeCounts.collectAsState()

    LaunchedEffect(fullGraph.startingNode) {
        if (fullGraph.startingNode == null) {
            showCreateNodeDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(fullGraph.startingNode?.name ?: "No Starting Node") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (fullGraph.startingNode != null) {
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
                                        nodeName = fullGraph.startingNode?.name ?: ""
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
        if (fullGraph.startingNode != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TagsSection(
                        tags = fullGraph.startingNode?.tags ?: emptyList(),
                        onAddTag = { showAddTagDialog = true },
                        onTagTap = { tag ->
                            selectedTag = tag
                            tagToEdit = tag
                            showTagContextMenu = true
                        }
                    )
                }
                
                item {
                    Text(
                        text = "Connectors",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(connectors) { connector ->
                    ConnectorItem(
                        connector = connector,
                        edgeCount = edgeCounts[connector.id] ?: 0
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showAddConnectorDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add connector",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Connector")
                    }
                }

                if (connectors.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No connectors yet. Tap 'Add Connector' to create one.",
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

    if (showCreateNodeDialog) {
        Dialog(onDismissRequest = { }) {
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
                        text = "This graph needs a starting node to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Button(
                            onClick = {
                                if (nodeName.isNotBlank()) {
                                    viewModel.createStartingNode(nodeName.trim())
                                    showCreateNodeDialog = false
                                    nodeName = ""
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
                                if (nodeName.isNotBlank() && fullGraph.startingNode != null) {
                                    viewModel.updateNodeName(fullGraph.startingNode.id, nodeName.trim())
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
                if (tag.isNotBlank() && fullGraph.startingNode != null) {
                    viewModel.addTag(fullGraph.startingNode.id, tag.trim())
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
                if (newTag.isNotBlank() && fullGraph.startingNode != null) {
                    viewModel.updateTag(fullGraph.startingNode.id, oldTag, newTag.trim())
                }
                showTagContextMenu = false
                selectedTag = ""
                tagToEdit = ""
            },
            onDeleteTag = { tag ->
                if (fullGraph.startingNode != null) {
                    viewModel.removeTag(fullGraph.startingNode.id, tag)
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

    if (showAddConnectorDialog) {
        Dialog(onDismissRequest = { 
            showAddConnectorDialog = false
            newConnectorName = ""
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
                        text = "Add Connector",
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
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showAddConnectorDialog = false
                                newConnectorName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (newConnectorName.isNotBlank() && fullGraph.startingNode != null) {
                                    viewModel.addConnector(fullGraph.startingNode.id, newConnectorName.trim())
                                    showAddConnectorDialog = false
                                    newConnectorName = ""
                                }
                            },
                            enabled = newConnectorName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
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
    edgeCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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