package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.entity.Edge
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.repository.GraphRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectorViewModel(
    private val repository: GraphRepository,
    private val connectorId: Long
) : ViewModel() {
    
    val connector = repository.getConnectorById(connectorId)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val currentNode = connector
        .combine(repository.getAllNodes()) { currentConnector, allNodes ->
            if (currentConnector != null) {
                allNodes.find { it.id == currentConnector.nodeId }
            } else null
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val edges: StateFlow<List<Edge>> = repository.getAllEdges()
        .combine(connector) { allEdges, currentConnector ->
            if (currentConnector != null) {
                allEdges.filter { edge ->
                    edge.fromConnectorId == connectorId || edge.toConnectorId == connectorId
                }
            } else {
                emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val connectedConnectors: StateFlow<Map<Long, String>> = combine(
        edges,
        repository.getAllConnectors(),
        repository.getAllNodes()
    ) { edgeList, allConnectors, allNodes ->
        val connectorMap = allConnectors.associateBy { it.id }
        val nodeMap = allNodes.associateBy { it.id }
        
        edgeList.associate { edge ->
            val otherConnectorId = if (edge.fromConnectorId == connectorId) {
                edge.toConnectorId
            } else {
                edge.fromConnectorId
            }
            val connector = connectorMap[otherConnectorId]
            val node = connector?.let { nodeMap[it.nodeId] }
            
            val displayName = if (connector != null && node != null) {
                "${node.name} (${connector.name})"
            } else {
                connector?.name ?: "Unknown"
            }
            
            edge.id to displayName
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val targetNodeIds: StateFlow<Map<Long, Long>> = combine(
        edges,
        repository.getAllConnectors()
    ) { edgeList, allConnectors ->
        val connectorMap = allConnectors.associateBy { it.id }
        
        edgeList.associate { edge ->
            val otherConnectorId = if (edge.fromConnectorId == connectorId) {
                edge.toConnectorId
            } else {
                edge.fromConnectorId
            }
            val connector = connectorMap[otherConnectorId]
            edge.id to (connector?.nodeId ?: -1L)
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun createEdge(targetConnectorId: Long, edgeName: String, weight: Double, bidirectional: Boolean) {
        viewModelScope.launch {
            val edge = Edge(
                fromConnectorId = connectorId,
                toConnectorId = targetConnectorId,
                name = edgeName,
                weight = weight,
                bidirectional = bidirectional
            )
            repository.insertEdge(edge)
        }
    }

    suspend fun createNodeAndConnector(nodeName: String, connectorName: String, graphId: Long): Long {
        val nodeId = repository.insertNode(Node(graphId = graphId, name = nodeName))
        val connectorId = repository.insertConnector(Connector(nodeId = nodeId, name = connectorName))
        return connectorId
    }
}