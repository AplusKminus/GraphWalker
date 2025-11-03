package app.pmsoft.graphwalker.data.dao

import androidx.room.*
import app.pmsoft.graphwalker.data.entity.Node
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    @Query("SELECT * FROM nodes WHERE graphId = :graphId")
    fun getNodesByGraphId(graphId: Long): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE id = :id")
    fun getNodeById(id: Long): Flow<Node?>

    @Query("SELECT * FROM nodes WHERE id IN (:ids)")
    fun getNodesByIds(ids: List<Long>): Flow<List<Node>>
    
    @Query("SELECT * FROM nodes")
    fun getAllNodes(): Flow<List<Node>>

    @Insert
    suspend fun insertNode(node: Node): Long

    @Update
    suspend fun updateNode(node: Node)

    @Delete
    suspend fun deleteNode(node: Node)
}