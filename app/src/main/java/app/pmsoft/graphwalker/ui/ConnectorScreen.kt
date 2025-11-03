package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.repository.GraphRepository
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModel
import app.pmsoft.graphwalker.ui.viewmodel.ConnectorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorScreen(
    connectorId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNode: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val database = GraphWalkerDatabase.getDatabase(context)
    val repository = GraphRepository(
        database.graphDao(),
        database.nodeDao(),
        database.connectorDao(),
        database.edgeDao()
    )
    val viewModel: ConnectorViewModel = viewModel(
        factory = ConnectorViewModelFactory(repository, connectorId)
    )

    val connector by viewModel.connector.collectAsState()
    
    var showContextMenu by remember { mutableStateOf(false) }
    var triggerEditDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(connector?.name ?: "Connector") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (connector != null) {
                        Box {
                            IconButton(onClick = { showContextMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit / Delete") },
                                    onClick = {
                                        showContextMenu = false
                                        triggerEditDelete = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        ConnectorView(
            connectorId = connectorId,
            paddingValues = paddingValues,
            viewModel = viewModel,
            onNavigateToNode = onNavigateToNode,
            onNavigateBack = onNavigateBack,
            triggerEditDelete = triggerEditDelete,
            onEditDeleteHandled = { triggerEditDelete = false }
        )
    }
}
