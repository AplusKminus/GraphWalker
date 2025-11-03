package app.pmsoft.graphwalker.data.dao

import androidx.room.*
import app.pmsoft.graphwalker.data.entity.Edge
import kotlinx.coroutines.flow.Flow

@Dao
interface EdgeDao {
    @Query("SELECT * FROM edges WHERE fromConnectorId = :connectorId OR toConnectorId = :connectorId")
    fun getEdgesByConnectorId(connectorId: Long): Flow<List<Edge>>

    @Query("SELECT * FROM edges WHERE id = :id")
    fun getEdgeById(id: Long): Flow<Edge?>

    @Query("SELECT * FROM edges")
    fun getAllEdges(): Flow<List<Edge>>

    @Query("""
        SELECT edges.* FROM edges 
        INNER JOIN connectors c1 ON edges.fromConnectorId = c1.id 
        INNER JOIN connectors c2 ON edges.toConnectorId = c2.id 
        INNER JOIN nodes n1 ON c1.nodeId = n1.id 
        INNER JOIN nodes n2 ON c2.nodeId = n2.id 
        WHERE n1.graphId = :graphId OR n2.graphId = :graphId
    """)
    fun getEdgesByGraphId(graphId: Long): Flow<List<Edge>>

    @Insert
    suspend fun insertEdge(edge: Edge): Long

    @Update
    suspend fun updateEdge(edge: Edge)

    @Delete
    suspend fun deleteEdge(edge: Edge)
}