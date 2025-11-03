package app.pmsoft.graphwalker.data.model

import app.pmsoft.graphwalker.data.entity.Graph
import app.pmsoft.graphwalker.data.entity.Node

data class FullGraph(
    val id: Long,
    val name: String,
    val nodes: List<Node>,
    val startingNode: Node?,
    val isDirected: Boolean = true,
    val hasEdgeWeights: Boolean = false,
    val hasEdgeLabels: Boolean = false,
    val hasConnectors: Boolean = false
) {
    companion object {
        fun from(graph: Graph, nodes: List<Node>): FullGraph {
            val startingNode = if (graph.startingNodeId != null) {
                nodes.find { it.id == graph.startingNodeId }
            } else {
                null
            }
            return FullGraph(
                id = graph.id,
                name = graph.name,
                nodes = nodes,
                startingNode = startingNode,
                isDirected = graph.isDirected,
                hasEdgeWeights = graph.hasEdgeWeights,
                hasEdgeLabels = graph.hasEdgeLabels,
                hasConnectors = graph.hasConnectors
            )
        }
    }
}