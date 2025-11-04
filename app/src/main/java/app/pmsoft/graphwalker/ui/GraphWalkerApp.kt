package app.pmsoft.graphwalker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.pmsoft.graphwalker.data.model.FullGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphWalkerApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "graph_list",
    ) {
        composable("graph_list") {
            GraphListScreen(
                onNavigateToGraph = { fullGraph: FullGraph ->
                    navController.navigate("graphs/${fullGraph.id}")
                },
            )
        }
        composable("graphs/{graphId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            GraphScreen(
                graphId = graphId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { nodeId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId")
                },
                onNavigateToConnector = { connectorId, nodeId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId/connectors/$connectorId")
                },
                onNavigateToClique = { cliqueId ->
                    navController.navigate("graphs/$graphId/cliques/$cliqueId")
                },
                onNavigateToUnconnectedConnectors = { graphIdForConnectors ->
                    navController.navigate("graphs/$graphIdForConnectors/analysis/unconnected-connectors")
                },
            )
        }
        composable("graphs/{graphId}/nodes") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            NodeViewScreen(
                graphId = graphId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConnector = { connectorId ->
                    navController.navigate("graphs/$graphId/connectors/$connectorId")
                },
                onNavigateToAddEdge = { connectorId ->
                    navController.navigate("graphs/$graphId/connectors/$connectorId/add-edge")
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graphs/$graphId", inclusive = false)
                },
                onNavigateToClique = { cliqueId ->
                    navController.navigate("graphs/$graphId/cliques/$cliqueId")
                },
            )
        }
        composable("graphs/{graphId}/nodes/{nodeId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            val nodeId = backStackEntry.arguments?.getString("nodeId")?.toLong() ?: return@composable
            NodeViewScreen(
                graphId = graphId,
                nodeId = nodeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConnector = { connectorId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId/connectors/$connectorId")
                },
                onNavigateToAddEdge = { connectorId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId/connectors/$connectorId/add-edge")
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graphs/$graphId", inclusive = false)
                },
                onNavigateToClique = { cliqueId ->
                    navController.navigate("graphs/$graphId/cliques/$cliqueId")
                },
            )
        }
        composable("graphs/{graphId}/nodes/{nodeId}/connectors/{connectorId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            val nodeId = backStackEntry.arguments?.getString("nodeId")?.toLong() ?: return@composable
            val connectorId = backStackEntry.arguments?.getString("connectorId")?.toLong() ?: return@composable
            ConnectorScreen(
                connectorId = connectorId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { nodeId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId")
                },
                onNavigateToAddEdge = { connectorId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId/connectors/$connectorId/add-edge")
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graphs/$graphId", inclusive = false)
                },
            )
        }
        composable("graphs/{graphId}/nodes/{nodeId}/connectors/{connectorId}/add-edge") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            val connectorId = backStackEntry.arguments?.getString("connectorId")?.toLong() ?: return@composable
            AddEdgeScreen(
                connectorId = connectorId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graphs/$graphId", inclusive = false)
                },
            )
        }
        composable("graphs/{graphId}/cliques/{cliqueId}") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            val cliqueId = backStackEntry.arguments?.getString("cliqueId")?.toLong() ?: return@composable
            CliqueScreen(
                cliqueId = cliqueId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNode = { nodeId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId")
                },
                onNavigateToGraphOverview = {
                    navController.popBackStack("graphs/$graphId", inclusive = false)
                },
            )
        }
        composable("graphs/{graphId}/analysis/unconnected-connectors") { backStackEntry ->
            val graphId = backStackEntry.arguments?.getString("graphId")?.toLong() ?: return@composable
            UnconnectedConnectorsScreen(
                graphId = graphId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConnector = { connectorId, nodeId ->
                    navController.navigate("graphs/$graphId/nodes/$nodeId/connectors/$connectorId")
                },
            )
        }
    }
}
