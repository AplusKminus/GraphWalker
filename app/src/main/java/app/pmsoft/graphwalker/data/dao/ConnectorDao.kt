package app.pmsoft.graphwalker.data.dao

import androidx.room.*
import app.pmsoft.graphwalker.data.entity.Connector
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectorDao {
    @Query("SELECT * FROM connectors WHERE nodeId = :nodeId")
    fun getConnectorsByNodeId(nodeId: Long): Flow<List<Connector>>

    @Query("SELECT * FROM connectors WHERE id = :id")
    fun getConnectorById(id: Long): Flow<Connector?>
    
    @Query("SELECT * FROM connectors")
    fun getAllConnectors(): Flow<List<Connector>>

    @Insert
    suspend fun insertConnector(connector: Connector): Long

    @Update
    suspend fun updateConnector(connector: Connector)

    @Delete
    suspend fun deleteConnector(connector: Connector)
}