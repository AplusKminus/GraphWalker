package app.pmsoft.graphwalker.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import app.pmsoft.graphwalker.data.entity.Clique
import app.pmsoft.graphwalker.data.entity.CliqueNodeCrossRef
import app.pmsoft.graphwalker.data.model.CliqueWithNodes
import app.pmsoft.graphwalker.data.model.NodeWithCliques

@Dao
interface CliqueDao {
    @Query("SELECT * FROM cliques WHERE graphId = :graphId ORDER BY name")
    fun getCliquesByGraphId(graphId: Long): Flow<List<Clique>>
    
    @Transaction
    @Query("SELECT * FROM cliques WHERE graphId = :graphId ORDER BY name")
    fun getCliquesWithNodesByGraphId(graphId: Long): Flow<List<CliqueWithNodes>>
    
    @Transaction
    @Query("SELECT * FROM cliques WHERE id = :cliqueId")
    fun getCliqueWithNodesById(cliqueId: Long): Flow<CliqueWithNodes?>
    
    @Transaction
    @Query("SELECT * FROM nodes WHERE id = :nodeId")
    fun getNodeWithCliques(nodeId: Long): Flow<NodeWithCliques?>
    
    @Transaction
    @Query("SELECT * FROM nodes WHERE graphId = :graphId")
    fun getNodesWithCliquesByGraphId(graphId: Long): Flow<List<NodeWithCliques>>
    
    @Insert
    suspend fun insertClique(clique: Clique): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCliqueNodeCrossRef(crossRef: CliqueNodeCrossRef)
    
    @Delete
    suspend fun deleteClique(clique: Clique)
    
    @Query("DELETE FROM clique_node_cross_ref WHERE cliqueId = :cliqueId AND nodeId = :nodeId")
    suspend fun removeNodeFromClique(cliqueId: Long, nodeId: Long)
    
    @Query("DELETE FROM clique_node_cross_ref WHERE cliqueId = :cliqueId")
    suspend fun removeAllNodesFromClique(cliqueId: Long)
    
    @Update
    suspend fun updateClique(clique: Clique)
}