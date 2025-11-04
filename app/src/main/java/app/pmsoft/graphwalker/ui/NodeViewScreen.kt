package app.pmsoft.graphwalker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.pmsoft.graphwalker.data.GraphWalkerDatabase
import app.pmsoft.graphwalker.repository.GraphRepository

@Composable
fun NodeViewScreen(
    graphId: Long,
    nodeId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToConnector: (Long) -> Unit = {},
    onNavigateToAddEdge: (Long) -> Unit = {},
    onNavigateToGraphOverview: () -> Unit = {},
    onNavigateToClique: (Long) -> Unit = {},
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

    val fullGraph by repository.getFullGraphById(graphId).collectAsState(initial = null)

    fullGraph?.let { graph ->
        NodeView(
            fullGraph = graph,
            targetNodeId = nodeId,
            onNavigateBack = onNavigateBack,
            onNavigateToConnector = onNavigateToConnector,
            onNavigateToAddEdge = onNavigateToAddEdge,
            onNavigateToGraphOverview = onNavigateToGraphOverview,
            onNavigateToClique = onNavigateToClique,
        )
    } ?: run {
        Box(
            modifier = Modifier.Companion.fillMaxSize(),
            contentAlignment = Alignment.Companion.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}