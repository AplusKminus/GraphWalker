package app.pmsoft.graphwalker.data.dao

import androidx.room.*
import app.pmsoft.graphwalker.data.entity.Graph
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphDao {
    @Query("SELECT * FROM graphs")
    fun getAllGraphs(): Flow<List<Graph>>

    @Query("SELECT * FROM graphs WHERE id = :id")
    fun getGraphById(id: Long): Flow<Graph?>

    @Insert
    suspend fun insertGraph(graph: Graph): Long

    @Update
    suspend fun updateGraph(graph: Graph)

    @Delete
    suspend fun deleteGraph(graph: Graph)
}