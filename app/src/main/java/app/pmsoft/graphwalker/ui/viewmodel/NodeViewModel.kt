package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pmsoft.graphwalker.data.entity.Connector
import app.pmsoft.graphwalker.data.entity.Graph
import app.pmsoft.graphwalker.data.entity.Node
import app.pmsoft.graphwalker.repository.GraphRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NodeViewModel(
    private val repository: GraphRepository,
    private val graphId: Long,
    private val nodeId: Long
) : ViewModel() {
    
    val fullGraph = repository.getFullGraphById(graphId)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val connectors: StateFlow<List<Connector>> = repository.getAllConnectors()
        .map { allConnectors ->
            allConnectors.filter { it.nodeId == nodeId }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val edgeCounts: StateFlow<Map<Long, Int>> = connectors
        .combine(repository.getAllEdges()) { connectorList, allEdges ->
            connectorList.associate { connector ->
                val count = allEdges.count { edge ->
                    edge.fromConnectorId == connector.id || edge.toConnectorId == connector.id
                }
                connector.id to count
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun createStartingNode(name: String) {
        viewModelScope.launch {
            val nodeId = repository.insertNode(Node(graphId = graphId, name = name))
            
            // Get the current graph to preserve its name
            val currentFullGraph = fullGraph.value
            if (currentFullGraph != null) {
                val updatedGraph = Graph(
                    id = currentFullGraph.id,
                    name = currentFullGraph.name,
                    startingNodeId = nodeId
                )
                repository.updateGraph(updatedGraph)
            }
        }
    }

    fun updateNodeName(nodeId: Long, newName: String) {
        viewModelScope.launch {
            val currentFullGraph = fullGraph.value
            val nodeToUpdate = currentFullGraph?.nodes?.find { it.id == nodeId }
            if (nodeToUpdate != null) {
                val updatedNode = nodeToUpdate.copy(name = newName)
                repository.updateNode(updatedNode)
            }
        }
    }

    fun addTag(nodeId: Long, tag: String) {
        viewModelScope.launch {
            val currentFullGraph = fullGraph.value
            val nodeToUpdate = currentFullGraph?.nodes?.find { it.id == nodeId }
            if (nodeToUpdate != null && !nodeToUpdate.tags.contains(tag)) {
                val updatedTags = nodeToUpdate.tags + tag
                val updatedNode = nodeToUpdate.copy(tags = updatedTags)
                repository.updateNode(updatedNode)
            }
        }
    }

    fun removeTag(nodeId: Long, tag: String) {
        viewModelScope.launch {
            val currentFullGraph = fullGraph.value
            val nodeToUpdate = currentFullGraph?.nodes?.find { it.id == nodeId }
            if (nodeToUpdate != null) {
                val updatedTags = nodeToUpdate.tags.filter { it != tag }
                val updatedNode = nodeToUpdate.copy(tags = updatedTags)
                repository.updateNode(updatedNode)
            }
        }
    }

    fun updateTag(nodeId: Long, oldTag: String, newTag: String) {
        viewModelScope.launch {
            val currentFullGraph = fullGraph.value
            val nodeToUpdate = currentFullGraph?.nodes?.find { it.id == nodeId }
            if (nodeToUpdate != null && !nodeToUpdate.tags.contains(newTag)) {
                val updatedTags = nodeToUpdate.tags.map { if (it == oldTag) newTag else it }
                val updatedNode = nodeToUpdate.copy(tags = updatedTags)
                repository.updateNode(updatedNode)
            }
        }
    }

    fun addConnector(nodeId: Long, connectorName: String) {
        viewModelScope.launch {
            val newConnector = Connector(nodeId = nodeId, name = connectorName)
            repository.insertConnector(newConnector)
        }
    }
}