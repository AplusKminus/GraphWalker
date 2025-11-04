package app.pmsoft.graphwalker.repository

import app.pmsoft.graphwalker.data.dao.*
import app.pmsoft.graphwalker.data.entity.*
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.data.model.CliqueWithNodes
import app.pmsoft.graphwalker.data.model.NodeWithCliques
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
class GraphRepository(
    private val graphDao: GraphDao,
    private val nodeDao: NodeDao,
    private val connectorDao: ConnectorDao,
    private val edgeDao: EdgeDao,
    private val cliqueDao: CliqueDao
) {
    fun getAllGraphs(): Flow<List<Graph>> = graphDao.getAllGraphs()
    
    fun getAllFullGraphs(): Flow<List<FullGraph>> = 
        graphDao.getAllGraphs().combine(nodeDao.getAllNodes()) { graphs, allNodes ->
            graphs.map { graph ->
                val graphNodes = allNodes.filter { it.graphId == graph.id }
                FullGraph.from(graph, graphNodes)
            }
        }
    
    fun getGraphById(id: Long): Flow<Graph?> = graphDao.getGraphById(id)
    
    fun getFullGraphById(id: Long): Flow<FullGraph?> = 
        graphDao.getGraphById(id).combine(nodeDao.getNodesByGraphId(id)) { graph, nodes ->
            graph?.let { FullGraph.from(it, nodes) }
        }
    
    suspend fun insertGraph(graph: Graph): Long = graphDao.insertGraph(graph)
    
    suspend fun updateGraph(graph: Graph) = graphDao.updateGraph(graph)
    
    suspend fun deleteGraph(graph: Graph) = graphDao.deleteGraph(graph)
    
    fun getNodesByGraphId(graphId: Long): Flow<List<Node>> = nodeDao.getNodesByGraphId(graphId)
    
    fun getAllNodes(): Flow<List<Node>> = nodeDao.getAllNodes()
    
    fun getNodeById(id: Long): Flow<Node?> = nodeDao.getNodeById(id)
    
    fun getNodesByIds(ids: List<Long>): Flow<List<Node>> = nodeDao.getNodesByIds(ids)
    
    suspend fun insertNode(node: Node): Long = nodeDao.insertNode(node)
    
    suspend fun updateNode(node: Node) = nodeDao.updateNode(node)
    
    suspend fun deleteNode(node: Node) = nodeDao.deleteNode(node)
    
    fun getConnectorsByNodeId(nodeId: Long): Flow<List<Connector>> = connectorDao.getConnectorsByNodeId(nodeId)
    
    fun getConnectorById(id: Long): Flow<Connector?> = connectorDao.getConnectorById(id)
    
    fun getAllConnectors(): Flow<List<Connector>> = connectorDao.getAllConnectors()
    
    fun getConnectorsByGraphId(graphId: Long): Flow<List<Connector>> = connectorDao.getConnectorsByGraphId(graphId)
    
    suspend fun insertConnector(connector: Connector): Long = connectorDao.insertConnector(connector)
    
    suspend fun updateConnector(connector: Connector) = connectorDao.updateConnector(connector)
    
    suspend fun deleteConnector(connector: Connector) = connectorDao.deleteConnector(connector)
    
    fun getEdgesByConnectorId(connectorId: Long): Flow<List<Edge>> = edgeDao.getEdgesByConnectorId(connectorId)
    
    fun getEdgeById(id: Long): Flow<Edge?> = edgeDao.getEdgeById(id)
    
    fun getEdgesByGraphId(graphId: Long): Flow<List<Edge>> = edgeDao.getEdgesByGraphId(graphId)
    
    fun getAllEdges(): Flow<List<Edge>> = edgeDao.getAllEdges()
    
    suspend fun insertEdge(edge: Edge): Long = edgeDao.insertEdge(edge)
    
    suspend fun updateEdge(edge: Edge) = edgeDao.updateEdge(edge)
    
    suspend fun deleteEdge(edge: Edge) = edgeDao.deleteEdge(edge)
    
    // Clique methods
    fun getCliquesByGraphId(graphId: Long): Flow<List<Clique>> = cliqueDao.getCliquesByGraphId(graphId)
    
    fun getCliquesWithNodesByGraphId(graphId: Long): Flow<List<CliqueWithNodes>> = cliqueDao.getCliquesWithNodesByGraphId(graphId)
    
    fun getCliqueWithNodesById(cliqueId: Long): Flow<CliqueWithNodes?> = cliqueDao.getCliqueWithNodesById(cliqueId)
    
    fun getNodeWithCliques(nodeId: Long): Flow<NodeWithCliques?> = cliqueDao.getNodeWithCliques(nodeId)
    
    fun getNodesWithCliquesByGraphId(graphId: Long): Flow<List<NodeWithCliques>> = cliqueDao.getNodesWithCliquesByGraphId(graphId)
    
    suspend fun insertClique(clique: Clique): Long = cliqueDao.insertClique(clique)
    
    suspend fun insertCliqueNodeCrossRef(crossRef: CliqueNodeCrossRef) = cliqueDao.insertCliqueNodeCrossRef(crossRef)
    
    suspend fun deleteClique(clique: Clique) = cliqueDao.deleteClique(clique)
    
    suspend fun removeNodeFromClique(cliqueId: Long, nodeId: Long) = cliqueDao.removeNodeFromClique(cliqueId, nodeId)
    
    suspend fun removeAllNodesFromClique(cliqueId: Long) = cliqueDao.removeAllNodesFromClique(cliqueId)
    
    suspend fun updateClique(clique: Clique) = cliqueDao.updateClique(clique)
}